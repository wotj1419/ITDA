import { defineStore } from 'pinia';
import { ref, computed, reactive, watch } from 'vue';
import type { CollabParticipant, CollabMessage, CollabStatus } from '../types/ui/collab';
import { socketManager } from '../services/ws/socket';
import { fetchChatMessages } from '../services/api/chat';
import { peerConnectionService } from '../services/webrtc/peerConnection';
import { useAuthStore } from './auth';

/**
 * Collaboration Store
 * 
 * Manages:
 * - WebSocket connection and signaling
 * - WebRTC Peer Connections (Mesh P2P)
 * - Collaboration State (Participants, Messages, Cursors)
 */
export const useCollabStore = defineStore('collab', () => {
    const STORAGE_KEY = 'collab:lastRoomId';
    const CURSOR_THROTTLE_MS = 80;

    // ================================
    // State
    // ================================
    const status = ref<CollabStatus>('disconnected');
    const roomId = ref<string | null>(null);
    const participants = ref<CollabParticipant[]>([]);
    const messages = ref<CollabMessage[]>([]);
    const isPanelOpen = ref(false);

    // UI State
    const isFloatingBarVisible = ref(false);
    const isMediaConnected = ref(false);
    const isAutoStarting = ref(false);
    const floatingBarResetToken = ref(0);
    const speakingMap = reactive(new Map<string, boolean>());
    const localStream = ref<MediaStream | null>(null);

    // Local User State
    const isMuted = ref(false);
    const isVideoOff = ref(true);
    const isScreenSharing = ref(false);
    const currentLocation = ref('');
    const currentSceneId = ref<number | null>(null);
    const currentNodeId = ref<number | null>(null);

    // Cursors (Map for performance)
    const cursors = reactive(new Map<string, { x: number, y: number, color: string }>());
    const cursorColors = [
        '#FF0000', // red
        '#FF8C00', // orange
        '#FFD700', // gold
        '#00C853', // green
        '#00B0FF', // blue
        '#5E35B1', // indigo
        '#E040FB', // violet
    ];
    const cursorColorByUser = new Map<string, string>();

    // Throttle state
    let lastCursorSentAt = 0;

    // ================================
    // Getters
    // ================================
    const isConnected = computed(() => status.value === 'connected');
    const participantCount = computed(() => participants.value.length);
    const hasUnreadMessages = computed(() => messages.value.length > 0);

    const authStore = useAuthStore();
    const localUserId = ref(authStore.user?.id ? String(authStore.user.id) : `user-${Math.random().toString(36).slice(2, 7)}`);

    watch(
        () => authStore.user?.id,
        (nextId) => {
            if (nextId) {
                localUserId.value = String(nextId);
            }
        }
    );

    const localParticipant = computed<CollabParticipant>(() => ({
        odps: localUserId.value,
        name: authStore.user?.name || 'Guest',
        isMuted: isMuted.value,
        isVideoOff: isVideoOff.value,
        isScreenSharing: isScreenSharing.value,
        currentLocation: currentLocation.value,
        sceneId: currentSceneId.value,
        nodeId: currentNodeId.value,
    }));

    const speakingMonitors = new Map<
        string,
        { ctx: AudioContext; source: MediaStreamAudioSourceNode; analyser: AnalyserNode; data: Uint8Array; rafId: number }
    >();

    const SPEAKING_THRESHOLD_ON = 0.06;
    const SPEAKING_THRESHOLD_OFF = 0.045;
    const SPEAKING_DECAY = 0.85;
    const SPEAKING_HOLD_MS = 180;

    const startSpeakingMonitor = async (id: string, stream: MediaStream) => {
        if (speakingMonitors.has(id)) return;
        try {
            const ctx = new AudioContext();
            const source = ctx.createMediaStreamSource(stream);
            const analyser = ctx.createAnalyser();
            analyser.fftSize = 512;
            const data = new Uint8Array(analyser.fftSize);
            source.connect(analyser);
            let smoothed = 0;
            let lastSpokeAt = 0;

            const tick = () => {
                analyser.getByteTimeDomainData(data);
                let sum = 0;
                for (let i = 0; i < data.length; i++) {
                    const sample = data[i] ?? 128;
                    const v = (sample - 128) / 128;
                    sum += v * v;
                }
                const rms = Math.sqrt(sum / data.length);
                smoothed = smoothed * SPEAKING_DECAY + rms * (1 - SPEAKING_DECAY);
                const prev = speakingMap.get(id) ?? false;
                const now = Date.now();
                if (prev) {
                    if (smoothed < SPEAKING_THRESHOLD_OFF && now - lastSpokeAt > SPEAKING_HOLD_MS) {
                        speakingMap.set(id, false);
                    }
                } else if (smoothed > SPEAKING_THRESHOLD_ON) {
                    speakingMap.set(id, true);
                    lastSpokeAt = now;
                }
                const monitor = speakingMonitors.get(id);
                if (monitor) {
                    monitor.rafId = requestAnimationFrame(tick);
                }
            };

            speakingMonitors.set(id, {
                ctx,
                source,
                analyser,
                data,
                rafId: requestAnimationFrame(tick),
            });

            await ctx.resume();
        } catch (error) {
            console.warn('Failed to start speaking monitor', error);
        }
    };

    const stopSpeakingMonitor = (id: string) => {
        const monitor = speakingMonitors.get(id);
        if (!monitor) return;
        cancelAnimationFrame(monitor.rafId);
        monitor.source.disconnect();
        monitor.analyser.disconnect();
        monitor.ctx.close().catch(() => null);
        speakingMonitors.delete(id);
        speakingMap.delete(id);
    };

    // ================================
    // Actions: Room Management
    // ================================

    /**
     * Join a project room (Signaling Only)
     */
    async function joinRoom(projectId: number): Promise<void> {
        const nextRoomId = `project-${projectId}`;

        if (status.value !== 'disconnected' && roomId.value && roomId.value !== nextRoomId) {
            leaveRoom();
        }

        if (roomId.value === nextRoomId && status.value !== 'disconnected') {
            return;
        }

        if (status.value === 'connected' || status.value === 'connecting') return;

        status.value = 'connecting';
        roomId.value = nextRoomId;
        localStorage.setItem(STORAGE_KEY, nextRoomId);

        try {
            // 1. Connect WebSocket
            if (!socketManager.getClient().connected) {
                socketManager.connect();
            }

            // 2. Subscribe to room (signaling)
            socketManager.subscribeToRoom(roomId.value);
            // 2-1. Subscribe to chat
            socketManager.subscribeToChat(roomId.value, handleChatMessage);
            // 2-2. Subscribe to presence
            socketManager.subscribeToPresence(roomId.value, handlePresenceMessage);

            // 3. Setup WebRTC Callbacks (Prepare for later)
            peerConnectionService.setCallbacks({
                onTrack: handleRemoteTrack,
                onIceCandidate: (candidate, peerId) => handleIceCandidate(peerId, candidate),
                onConnectionStateChange: handleConnectionStateChange,
            });

            // 4. Broadcast Join (Signaling Only)
            setTimeout(() => {
                socketManager.sendSignal({
                    type: 'join',
                    payload: {
                        user: localParticipant.value
                    }
                });
                status.value = 'connected';
            }, 1000);

            // 5. Load chat history (optional)
            void loadChatHistory(projectId);

        } catch (error) {
            console.error('Failed to join room:', error);
            status.value = 'error';
        }
    }

    /**
     * Enable Media (Voice/Video) - "Go Live"
     */
    async function enableMedia() {
        if (!roomId.value || status.value !== 'connected') return;
        if (isMediaConnected.value) {
            isAutoStarting.value = false;
            return;
        }

        // Backend spec: max 6 participants for audio mesh
        if (participants.value.length >= 6) {
            console.warn('[RTC] Room is full (max 6 participants)');
            alert('협업 통화는 최대 6명까지 참여할 수 있습니다.');
            isAutoStarting.value = false;
            return;
        }

        try {
            // 1. Get Local Stream
            const stream = await peerConnectionService.getLocalStream({ video: false, audio: true });
            localStream.value = stream;
            if (stream) {
                await startSpeakingMonitor(localUserId.value, stream);
            }

            // 2. Initialize Peer Connections for existing participants
            // (In a mesh, we need to offer to everyone who is already here? 
            //  Or just wait for them? Ideally we offer to existing peers)
            //  The current logic relied on 'join' signal trigger. 
            //  Since we already joined, we might need to send a 'media_ready' signal?
            //  Or just create offers now.

            // Simplified: Just iterate participants and offer if they are media ready?
            // For now, let's assume standard mesh: create offer to all existing.
            participants.value.forEach(async (p) => {
                if (p.odps && p.odps !== localParticipant.value.odps) {
                    peerConnectionService.createPeerConnection(p.odps);
                    const offer = await peerConnectionService.createOffer(p.odps);
                    if (offer) {
                        socketManager.sendSignal({
                            type: 'offer',
                            targetId: p.odps,
                            payload: offer
                        });
                    }
                }
            });

            isMediaConnected.value = true;
            isMuted.value = false; // Auto-unmute on connect? Or keep muted? User said "Live" button. Usually starts unmuted.
            peerConnectionService.toggleMute(false);

        } catch (e) {
            console.error('Failed to enable media:', e);
        } finally {
            isAutoStarting.value = false;
        }
    }

    /**
     * Disable Media - "End Call"
     */
    function disableMedia() {
        if (!isMediaConnected.value) return;

        // Broadcast Media Stop to peers so they can clean up
        if (roomId.value) {
            socketManager.sendSignal({
                type: 'media_stop',
            });
        }

        // Close all peer connections but keep socket
        peerConnectionService.closeAll();
        // peerConnectionService.stopLocalStream(); // Assuming this method exists or handled in closeAll

        isMediaConnected.value = false;
        isMuted.value = false;
        localStream.value = null;
        isPanelOpen.value = false;
        stopSpeakingMonitor(localUserId.value);
    }

    function handleMediaStop(peerId: string) {
        console.log(`[Collab] Peer stopped media: ${peerId}`);
        peerConnectionService.removePeer(peerId);
    }

    /**
     * Leave the room completely
     */
    function leaveRoom() {
        if (!roomId.value) return;

        // Broadcast Leave
        socketManager.sendSignal({
            type: 'leave',
        });

        socketManager.unsubscribeChat(roomId.value);
        socketManager.unsubscribePresence(roomId.value);
        disableMedia(); // Handles WebRTC cleanup
        isFloatingBarVisible.value = false;
        floatingBarResetToken.value += 1;
        isAutoStarting.value = false;
        localStorage.removeItem('collab:floatingPos');

        // Close Socket Subscription
        socketManager.disconnect();

        // Reset State
        status.value = 'disconnected';
        roomId.value = null;
        participants.value = [];
        messages.value = [];
        cursors.clear();
        lastCursorSentAt = 0;
        isPanelOpen.value = false;
        localStorage.removeItem(STORAGE_KEY);
    }

    function showFloatingBar(resetPosition = false) {
        isFloatingBarVisible.value = true;
        if (resetPosition) {
            floatingBarResetToken.value += 1;
        }
    }

    function hideFloatingBar() {
        isFloatingBarVisible.value = false;
    }

    function startCall(projectId: number) {
        isAutoStarting.value = true;
        isPanelOpen.value = false;
        joinRoom(projectId);
        showFloatingBar(true);
        if (status.value === 'connected') {
            void enableMedia();
        }
    }
    async function handleSignal(signal: any) {
        const { type, senderId, payload, targetId } = signal;

        // Ignore my own signals
        if (String(senderId) === localParticipant.value.odps) return;

        // If signal is targeted and not for me, ignore
        if (targetId && targetId !== localParticipant.value.odps) return;

        switch (type) {
            case 'join':
                handlePeerJoin(senderId, payload.user);
                break;
            case 'leave':
                handlePeerLeave(senderId);
                break;
            case 'offer':
                await handleOffer(senderId, payload);
                break;
            case 'answer':
                await handleAnswer(senderId, payload);
                break;
            case 'candidate':
                await handleIceCandidate(senderId, payload);
                break;
            case 'cursor':
                handleCursorUpdate(senderId, payload);
                break;
            case 'media_stop':
                handleMediaStop(senderId);
                break;
            case 'state_update':
                handleStateUpdate(senderId, payload);
                break;
        }
    }

    /**
     * Handle Peer Join
     * Current user (already in room) creates offer for new user
     */
    async function handlePeerJoin(peerId: string, userData: CollabParticipant) {
        console.log(`[Collab] Peer Joined: ${peerId}`);
        addParticipant(peerId, userData);

        // Create PeerConnection
        peerConnectionService.createPeerConnection(peerId);

        // Create Offer
        const offer = await peerConnectionService.createOffer(peerId);
        if (offer) {
            socketManager.sendSignal({
                type: 'offer',
                targetId: peerId,
                payload: offer
            });
        }

        // Send back my state so they know I'm here
        socketManager.sendSignal({
            type: 'state_update',
            targetId: peerId,
            payload: localParticipant.value
        });
    }

    /**
     * Handle Peer Leave
     */
    function handlePeerLeave(peerId: string) {
        console.log(`[Collab] Peer Left: ${peerId}`);
        removeParticipant(peerId);
        peerConnectionService.removePeer(peerId);
    }

    /**
     * Handle Offer
     */
    async function handleOffer(peerId: string, offer: RTCSessionDescriptionInit) {
        console.log(`[Collab] Received Offer from ${peerId}`);

        // Ensure PeerConnection exists
        peerConnectionService.createPeerConnection(peerId);

        await peerConnectionService.setRemoteDescription(peerId, offer);

        const answer = await peerConnectionService.createAnswer(peerId);
        if (answer) {
            socketManager.sendSignal({
                type: 'answer',
                targetId: peerId,
                payload: answer
            });
        }
    }

    /**
     * Handle Answer
     */
    async function handleAnswer(peerId: string, answer: RTCSessionDescriptionInit) {
        console.log(`[Collab] Received Answer from ${peerId}`);
        await peerConnectionService.setRemoteDescription(peerId, answer);
    }

    /**
     * Handle Ice Candidate
     */
    async function handleIceCandidate(peerId: string, candidate: RTCIceCandidateInit) {
        await peerConnectionService.addIceCandidate(peerId, candidate);
    }

    /**
     * Handle Remote Track (Audio/Video)
     */
    function handleRemoteTrack(stream: MediaStream, peerId: string) {
        console.log(`[Collab] Received Remote Stream from ${peerId}`);
        // Typically we attach this stream to an audio element in UI
        // We can expose a map of streams or emit an event
        // For audio-only mesh, we usually create an audio element dynamically
        let audio = document.getElementById(`audio-${peerId}`) as HTMLAudioElement;
        if (!audio) {
            audio = document.createElement('audio');
            audio.id = `audio-${peerId}`;
            audio.autoplay = true;
            audio.style.display = 'none'; // Hidden audio
            document.body.appendChild(audio);
        }
        audio.srcObject = stream;
        void startSpeakingMonitor(peerId, stream);
    }

    function handleConnectionStateChange(state: RTCPeerConnectionState, peerId: string) {
        console.log(`[Collab] Connection State ${peerId}: ${state}`);
    }

    // ================================
    // Actions: Features
    // ================================

    const canSendSignal = () => socketManager.canSendSignal() && status.value === 'connected' && !!roomId.value;

    function sendMessage(content: string) {
        if (!content.trim()) return;
        if (!canSendSignal() || !roomId.value) return;

        // Backend spec: max 2000 characters
        if (content.length > 2000) {
            console.warn('[Chat] Message too long');
            alert('메시지는 최대 2000자까지 입력할 수 있습니다.');
            return;
        }

        const now = Date.now();
        const localId = `local-${now}`;

        // Publish to chat topic (spec)
        socketManager.sendChat(roomId.value, {
            content,
            type: 'TEXT',
        });

        // Optimistic local append
        messages.value.push({
            messageId: localId,
            senderId: localParticipant.value.odps,
            senderName: localParticipant.value.name,
            content: content,
            timestamp: now,
            type: 'chat',
        });
    }

    function handleChatMessage(message: any) {
        const createdAt = message?.createdAt ? Date.parse(message.createdAt) : Date.now();
        const senderId = message?.sender?.userId ? String(message.sender.userId) : 'system';
        const senderName = message?.sender?.name ?? '알 수 없음';
        const content = message?.content ?? '';
        const messageId = message?.messageId ? String(message.messageId) : `msg-${createdAt}-${senderId}`;

        // Replace optimistic local message if applicable
        if (senderId === localParticipant.value.odps && content) {
            const idx = [...messages.value].reverse().findIndex((m) =>
                m.senderId === senderId && m.content === content && m.messageId.startsWith('local-')
            );
            if (idx !== -1) {
                const realIdx = messages.value.length - 1 - idx;
                messages.value[realIdx] = {
                    ...messages.value[realIdx],
                    messageId,
                    timestamp: createdAt,
                } as CollabMessage;
                return;
            }
        }

        messages.value.push({
            messageId,
            senderId,
            senderName,
            content,
            timestamp: createdAt,
            type: 'chat',
        });
    }

    async function loadChatHistory(projectId: number) {
        if (!authStore.isAuthenticated) return;
        try {
            const data = await fetchChatMessages(projectId, 50);
            if (!data?.items?.length) return;
            const mapped = data.items
                .slice()
                .reverse()
                .map((item) => ({
                    messageId: String(item.messageId),
                    senderId: String(item.sender?.userId ?? 'system'),
                    senderName: item.sender?.name ?? '알 수 없음',
                    content: item.content ?? '',
                    timestamp: Date.parse(item.createdAt),
                    type: 'chat' as const,
                }));
            messages.value = mapped;
        } catch (error) {
            console.warn('Failed to load chat history', error);
        }
    }

    function updateCursor(x: number, y: number, _sceneId?: number | null) {
        if (!authStore.isAuthenticated) return;

        const now = Date.now();
        if (now - lastCursorSentAt < CURSOR_THROTTLE_MS) return;
        lastCursorSentAt = now;

        if (!canSendSignal()) return;

        socketManager.sendSignal({
            type: 'cursor',
            payload: { x, y }
        });
    }

    function handleCursorUpdate(peerId: string, payload: { x: number, y: number }) {
        const color = getOrAssignCursorColor(peerId);
        cursors.set(peerId, {
            x: payload.x,
            y: payload.y,
            color,
        });
    }

    type PresenceLocation = 'PROJECT_LIST' | 'SCENE_LIST' | 'SCENE_EDIT' | 'TIMELINE';

    function updateLocation(location: PresenceLocation, sceneId?: number | null, nodeId?: number | null) {
        currentLocation.value = location;
        currentSceneId.value = sceneId ?? null;
        currentNodeId.value = nodeId ?? null;
        if (roomId.value) {
            socketManager.sendPresence(roomId.value, {
                type: 'LOCATION',
                location,
                sceneId: sceneId ?? null,
                nodeId: nodeId ?? null,
            });
        }
        broadcastState();
    }

    function handlePresenceMessage(message: any) {
        const userId = message?.userId;
        if (!userId) return;
        const peerId = String(userId);
        addParticipant(peerId, {
            odps: peerId,
            name: message?.name ?? 'Guest',
            avatarUrl: message?.profileImageUrl ?? undefined,
            currentLocation: message?.location ?? '',
            sceneId: message?.sceneId ?? null,
            nodeId: message?.nodeId ?? null,
        });
    }

    // ================================
    // Helpers
    // ================================

    function handleStateUpdate(peerId: string, userData: CollabParticipant) {
        addParticipant(peerId, userData);
    }

    function addParticipant(peerId: string, userData: CollabParticipant) {
        const idx = participants.value.findIndex(p => p.odps === peerId);
        if (idx === -1) {
            // Use provided data or defaults
            participants.value.push({
                ...userData,
                odps: peerId, // Ensure ID matches
            });
        } else {
            // Update existing
            participants.value[idx] = { ...participants.value[idx], ...userData };
        }
    }

    function removeParticipant(peerId: string) {
        participants.value = participants.value.filter(p => p.odps !== peerId);
        cursors.delete(peerId);
        cursorColorByUser.delete(peerId);
        stopSpeakingMonitor(peerId);
        // Cleanup audio
        const audio = document.getElementById(`audio-${peerId}`);
        if (audio) audio.remove();
    }

    function getOrAssignCursorColor(peerId: string): string {
        const existing = cursorColorByUser.get(peerId);
        if (existing) return existing;

        const used = new Set(cursorColorByUser.values());
        const available = cursorColors.filter((c) => !used.has(c));
        const pool = available.length > 0 ? available : cursorColors;
        const color = pool[Math.floor(Math.random() * pool.length)] ?? cursorColors[0] ?? '#9CA3AF';
        cursorColorByUser.set(peerId, color);
        return color;
    }

    function toggleMute() {
        isMuted.value = !isMuted.value;
        peerConnectionService.toggleMute(isMuted.value);

        // Broadcast mute state to other participants (Backend spec: MUTE type)
        if (roomId.value && isMediaConnected.value) {
            socketManager.sendSignal({
                type: 'MUTE',
                payload: {
                    muted: isMuted.value,
                },
            });
        }

        broadcastState();
    }

    function toggleVideo() {
        isVideoOff.value = !isVideoOff.value;
        peerConnectionService.toggleVideo(isVideoOff.value);
        broadcastState();
    }

    async function toggleScreenShare() {
        if (isScreenSharing.value) {
            peerConnectionService.stopScreenShare();
            isScreenSharing.value = false;
        } else {
            const stream = await peerConnectionService.startScreenShare();
            if (stream) {
                isScreenSharing.value = true;
            }
        }
        broadcastState();
    }

    function togglePanel() {
        isPanelOpen.value = !isPanelOpen.value;
    }

    function broadcastState() {
        if (!canSendSignal()) return;
        socketManager.sendSignal({
            type: 'state_update',
            payload: localParticipant.value
        });
    }

    function rejoinIfNeeded() {
        if (status.value === 'connected' || status.value === 'connecting') return;

        const savedRoomId = localStorage.getItem(STORAGE_KEY);
        if (!savedRoomId) return;

        let parsedId: number | null = null;
        if (savedRoomId.startsWith('project-')) {
            const raw = savedRoomId.replace('project-', '');
            const value = Number(raw);
            parsedId = Number.isFinite(value) ? value : null;
        } else {
            const value = Number(savedRoomId);
            parsedId = Number.isFinite(value) ? value : null;
        }

        if (!parsedId) {
            localStorage.removeItem(STORAGE_KEY);
            stopSpeakingMonitor(localUserId.value);
            return;
        }

        joinRoom(parsedId);
    }

    watch([status, isMediaConnected], ([nextStatus, nextMedia]) => {
        if (!isAutoStarting.value) return;
        if (nextStatus === 'connected' && !nextMedia) {
            void enableMedia();
        }
        if (nextMedia || nextStatus === 'error' || nextStatus === 'disconnected') {
            isAutoStarting.value = false;
        }
    });

    function isSpeaking(peerId: string): boolean {
        return speakingMap.get(peerId) ?? false;
    }

    return {
        // State
        status,
        participants,
        messages,
        cursors,
        isPanelOpen,
        isFloatingBarVisible, // Exported
        isMediaConnected,     // Exported
        isAutoStarting,
        floatingBarResetToken,
        isMuted,
        isVideoOff,
        isScreenSharing,
        localParticipant,
        // Getters
        isConnected,
        hasUnreadMessages,
        participantCount,
        isSpeaking,
        // Actions
        joinRoom,
        leaveRoom,
        enableMedia,  // Exported
        disableMedia, // Exported
        startCall,
        showFloatingBar, // Exported
        hideFloatingBar, // Exported
        sendMessage,
        toggleMute,
        toggleVideo,
        toggleScreenShare,
        togglePanel,
        handleSignal,
        updateCursor,
        updateLocation,
        rejoinIfNeeded,
    };
});
