import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '../../stores/auth';
import { useCollabStore } from '../../stores/collab';

// Default to localhost for dev if env is missing
const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

class WebSocketManager {
    private client: Client;
    private lastSendWarnAt = 0;
    private chatSubscriptions = new Map<string, ReturnType<Client['subscribe']>>();
    private chatHandlers = new Map<string, (message: any) => void>();
    private presenceSubscriptions = new Map<string, ReturnType<Client['subscribe']>>();
    private presenceHandlers = new Map<string, (message: any) => void>();
    private rtcSubscription: ReturnType<Client['subscribe']> | null = null;
    private rtcHandler: ((message: any) => void) | null = null;
    private currentProjectId: string | null = null;
    private currentRoomId: string | null = null;

    constructor() {
        this.client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            debug: (str) => {
                console.debug('[WS]:', str);
            },
            reconnectDelay: 5000, // Auto reconnect
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        this.client.onConnect = () => {
            console.log('Connected to WebSocket');
            // Subscribe to topics
            this.subscribe();
            this.resubscribeChats();
            this.resubscribePresence();
            this.resubscribeRTC();
        };

        this.client.onStompError = (frame) => {
            console.error('WebSocket Error:', frame.headers['message']);
            console.error('Details:', frame.body);
        };

        this.client.onWebSocketClose = () => {
            console.warn('WebSocket disconnected');
        };
    }

    public connect() {
        const authStore = useAuthStore();
        if (authStore.accessToken) {
            this.client.connectHeaders = {
                Authorization: `Bearer ${authStore.accessToken}`,
            };
        } else {
            this.client.connectHeaders = {};
        }
        this.client.activate();
    }

    public disconnect() {
        this.client.deactivate();
    }

    public subscribeToRoom(roomId: string) {
        if (this.currentRoomId === roomId) return;
        this.currentRoomId = roomId;

        if (!this.client.connected) return;

        console.log(`Subscribing to room: ${roomId}`);
        // Topic for broadcasting signals (join, offer, answer, ice)
        this.client.subscribe(`/topic/room/${roomId}`, (message) => {
            const signal = JSON.parse(message.body);
            // Dispatch to stores
            const collabStore = useCollabStore();
            collabStore.handleSignal(signal);
        });
    }

    public subscribeToChat(projectId: string, handler: (message: any) => void) {
        if (this.chatSubscriptions.has(projectId)) return;
        this.chatHandlers.set(projectId, handler);
        if (!this.client.connected) return;

        const subscription = this.client.subscribe(`/topic/chat/${projectId}`, (message) => {
            try {
                const payload = JSON.parse(message.body);
                handler(payload);
            } catch (error) {
                console.error('Failed to parse chat message', error);
            }
        });
        this.chatSubscriptions.set(projectId, subscription);
    }

    public unsubscribeChat(projectId: string) {
        const sub = this.chatSubscriptions.get(projectId);
        if (sub) {
            sub.unsubscribe();
            this.chatSubscriptions.delete(projectId);
        }
        this.chatHandlers.delete(projectId);
    }

    public sendChat(projectId: string, payload: { content: string; type?: string }) {
        if (!this.client.connected) {
            console.warn('Cannot send chat: disconnected');
            return;
        }
        this.client.publish({
            destination: `/pub/chat/${projectId}`,
            body: JSON.stringify(payload),
        });
    }

    public subscribeToPresence(projectId: string, handler: (message: any) => void) {
        if (this.presenceSubscriptions.has(projectId)) return;
        this.presenceHandlers.set(projectId, handler);
        if (!this.client.connected) return;

        const subscription = this.client.subscribe(`/topic/presence/${projectId}`, (message) => {
            try {
                const payload = JSON.parse(message.body);
                handler(payload);
            } catch (error) {
                console.error('Failed to parse presence message', error);
            }
        });
        this.presenceSubscriptions.set(projectId, subscription);
    }

    public unsubscribePresence(projectId: string) {
        const sub = this.presenceSubscriptions.get(projectId);
        if (sub) {
            sub.unsubscribe();
            this.presenceSubscriptions.delete(projectId);
        }
        this.presenceHandlers.delete(projectId);
    }

    public sendPresence(projectId: string, payload: { type: 'LOCATION'; location: string; sceneId?: number | null; nodeId?: number | null }) {
        if (!this.client.connected) {
            console.warn('Cannot send presence: disconnected');
            return;
        }
        this.client.publish({
            destination: `/pub/presence/${projectId}`,
            body: JSON.stringify(payload),
        });
    }

    // RTC Signaling (Backend Spec: /pub/rtc/{projectId} + /user/queue/rtc)
    public subscribeToRTC(projectId: string, handler: (message: any) => void) {
        if (this.rtcSubscription) return; // Already subscribed
        this.currentProjectId = projectId;
        this.rtcHandler = handler;
        if (!this.client.connected) return;

        // Subscribe to personal queue for 1:1 RTC signals
        const subscription = this.client.subscribe('/user/queue/rtc', (message) => {
            try {
                const payload = JSON.parse(message.body);
                handler(payload);
            } catch (error) {
                console.error('Failed to parse RTC message', error);
            }
        });
        this.rtcSubscription = subscription;
        console.log('[RTC] Subscribed to /user/queue/rtc');
    }

    public unsubscribeRTC() {
        if (this.rtcSubscription) {
            this.rtcSubscription.unsubscribe();
            this.rtcSubscription = null;
        }
        this.rtcHandler = null;
        this.currentProjectId = null;
    }

    public sendRTC(projectId: string, payload: any) {
        if (!this.client.connected) {
            console.warn('Cannot send RTC signal: disconnected');
            return;
        }
        this.client.publish({
            destination: `/pub/rtc/${projectId}`,
            body: JSON.stringify(payload),
        });
    }

    public sendSignal(signal: any) {
        if (!this.client.connected || !this.currentRoomId) {
            const now = Date.now();
            if (now - this.lastSendWarnAt > 5000) {
                console.warn('Cannot send signal: disconnected or no room joined');
                this.lastSendWarnAt = now;
            }
            return;
        }
        const authStore = useAuthStore();
        const senderId = String(authStore.user?.id || `anon-${Math.random().toString(36).substr(2, 9)}`);

        const signalWithSender = {
            ...signal,
            senderId,
            timestamp: Date.now()
        };

        this.client.publish({
            destination: `/app/room/${this.currentRoomId}/signal`,
            body: JSON.stringify(signalWithSender),
        });
    }

    private subscribe() {
        // Re-subscribe to current room on reconnect
        if (this.currentRoomId) {
            const roomId = this.currentRoomId;
            this.currentRoomId = null; // Force re-subscription
            this.subscribeToRoom(roomId);
        }
    }

    private resubscribeChats() {
        if (!this.client.connected) return;
        const entries = Array.from(this.chatHandlers.entries());
        this.chatSubscriptions.forEach((sub) => sub.unsubscribe());
        this.chatSubscriptions.clear();
        entries.forEach(([projectId, handler]) => {
            const subscription = this.client.subscribe(`/topic/chat/${projectId}`, (message) => {
                try {
                    const payload = JSON.parse(message.body);
                    handler(payload);
                } catch (error) {
                    console.error('Failed to parse chat message', error);
                }
            });
            this.chatSubscriptions.set(projectId, subscription);
        });
    }

    private resubscribePresence() {
        if (!this.client.connected) return;
        const entries = Array.from(this.presenceHandlers.entries());
        this.presenceSubscriptions.forEach((sub) => sub.unsubscribe());
        this.presenceSubscriptions.clear();
        entries.forEach(([projectId, handler]) => {
            const subscription = this.client.subscribe(`/topic/presence/${projectId}`, (message) => {
                try {
                    const payload = JSON.parse(message.body);
                    handler(payload);
                } catch (error) {
                    console.error('Failed to parse presence message', error);
                }
            });
            this.presenceSubscriptions.set(projectId, subscription);
        });
    }

    private resubscribeRTC() {
        if (!this.client.connected || !this.rtcHandler || !this.currentProjectId) return;
        if (this.rtcSubscription) {
            this.rtcSubscription.unsubscribe();
            this.rtcSubscription = null;
        }
        const handler = this.rtcHandler;
        const subscription = this.client.subscribe('/user/queue/rtc', (message) => {
            try {
                const payload = JSON.parse(message.body);
                handler(payload);
            } catch (error) {
                console.error('Failed to parse RTC message', error);
            }
        });
        this.rtcSubscription = subscription;
        console.log('[RTC] Resubscribed to /user/queue/rtc');
    }

    public getClient() {
        return this.client;
    }

    public canSendSignal() {
        return this.client.connected && !!this.currentRoomId;
    }
}

export const socketManager = new WebSocketManager();
