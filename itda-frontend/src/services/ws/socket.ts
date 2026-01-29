import { Client, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '../../stores/auth';
import { useCollabStore } from '../../stores/collab';

// Default to localhost for dev if env is missing
const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

class WebSocketManager {
    private client: Client;
    private subscription: StompSubscription | null = null;
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

        this.client.beforeConnect = () => {
            const authStore = useAuthStore();
            const token = authStore.accessToken;
            this.client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
        };

        this.client.onConnect = () => {
            console.log('Connected to WebSocket');
            // Subscribe to topics
            this.subscribe();
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
        const token = authStore.accessToken;
        this.client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
        this.client.activate();
    }

    public disconnect() {
        if (this.subscription) {
            this.subscription.unsubscribe();
            this.subscription = null;
        }
        this.currentRoomId = null;
        this.client.deactivate();
    }

    public subscribeToRoom(roomId: string) {
        if (this.currentRoomId === roomId) return;
        this.currentRoomId = roomId;

        if (!this.client.connected) return;

        this.subscribeToQueue();
    }

    private subscribeToQueue() {
        if (this.subscription) return;
        console.log('Subscribing to RTC queue');
        this.subscription = this.client.subscribe('/user/queue/rtc', (message) => {
            const signal = JSON.parse(message.body);
            if (this.currentRoomId && signal?.projectId && String(signal.projectId) !== this.currentRoomId) {
                return;
            }
            // Dispatch to stores
            const collabStore = useCollabStore();
            collabStore.handleSignal(signal);
        });
    }

    public sendSignal(signal: any) {
        if (!this.client.connected || !this.currentRoomId) {
            console.warn('Cannot send signal: disconnected or no room joined');
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
            destination: `/pub/rtc/${this.currentRoomId}`,
            body: JSON.stringify(signalWithSender),
        });
    }

    private subscribe() {
        // Re-subscribe to current room on reconnect
        if (this.currentRoomId) {
            if (this.subscription) {
                this.subscription.unsubscribe();
                this.subscription = null;
            }
            this.subscribeToQueue();
        }
    }

    public getClient() {
        return this.client;
    }
}

export const socketManager = new WebSocketManager();
