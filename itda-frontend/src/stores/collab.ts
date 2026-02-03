import { defineStore } from 'pinia';
import { ref, computed, reactive, watch } from 'vue';
import type { CollabParticipant, CollabMessage, CollabStatus } from '../types/ui/collab';
import { socketManager } from '../services/ws/socket';
import { fetchChatMessages } from '../services/api/chat';
import { peerConnectionService } from '../services/webrtc/peerConnection';
import { useAuthStore } from './auth';
import { useSceneNodeStore } from './sceneNode';

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
    const READ_KEY_PREFIX = 'collab:lastReadAt:';
    const CURSOR_THROTTLE_MS = 80;
    const NODE_MOVE_THROTTLE_MS = 80;

    // ================================
    // State
    // ================================
    const status = ref<CollabStatus>('disconnected');
    const roomId = ref<string | null>(null);
    const currentProjectId = ref<number | null>(null);
    const participants = ref<CollabParticipant[]>([]);
    const messages = ref<CollabMessage[]>([]);
    const lastReadAt = ref(0);
    const isPanelOpen = ref(false);
    const getReadStorageKey = (projectId: number) => `${READ_KEY_PREFIX}${projectId}`;

    // UI State
    const isFloatingBarVisible = ref(false);
    const isMediaConnected = ref(false);
    const isAutoStarting = ref(false);
    const floatingBarResetToken = ref(0);
    const speakingMap = reactive(new Map<string, boolean>());
    const localStream = ref<MediaStream | null>(null);
    const audioInputDevices = ref<MediaDeviceInfo[]>([]);
    const selectedMicId = ref<string>(localStorage.getItem('collab:micId') ?? '');
    const remoteVolumeMap = reactive(new Map<string, number>());
    const rtcJoinPending = ref(false);
    const rtcJoined = ref(false);
    const rtcPeers = reactive(new Set<string>());

    // Local User State
    const isMuted = ref(false);
    const isVideoOff = ref(true);
    const isScreenSharing = ref(false);
    const currentLocation = ref('');
    const currentSceneId = ref<number | null>(null);
    const currentNodeId = ref<number | null>(null);
    const pendingPresence = ref<{ location: PresenceLocation; sceneId: number | null; nodeId: number | null } | null>(null);

    // Cursors (Map for performance)
    const cursors = reactive(new Map<string, { x: number; y: number; color: string; sceneId: number }>());
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

    const nodeLocks = reactive(new Map<string, { userId: string; name: string; sceneId: number; updatedAt: number }>());
    const nodeLockUpdatedAt = new Map<string, number>();

    // Throttle state
    let lastCursorSentAt = 0;
    const lastNodeMoveSentAt = new Map<string, number>();
    const lastRemoteNodeMoveAt = new Map<string, number>();
    const localDraggingNodes = new Set<string>();
    let localLockedNodeId: string | null = null;

    // ================================
    // Getters
    // ================================
    const isConnected = computed(() => status.value === 'connected');
    const participantCount = computed(() => participants.value.length);
    const getStoredLastReadAt = () => {
        if (currentProjectId.value === null) return 0;
        const stored = localStorage.getItem(getReadStorageKey(currentProjectId.value));
        return stored ? Number(stored) || 0 : 0;
    };

    const hasUnreadMessages = computed(() => {
        const lastRead = Math.max(lastReadAt.value, getStoredLastReadAt());
        return messages.value.some(
            (m) => m.senderId !== localParticipant.value.odps && (m.timestamp ?? 0) > lastRead
        );
    });

    const authStore = useAuthStore();
    const sceneNodeStore = useSceneNodeStore();
    const flowToScreenCoordinate = ref<((pos: { x: number; y: number }) => { x: number; y: number }) | null>(null);
    const localUserId = ref(authStore.user?.id ? String(authStore.user.id) : `user-${Math.random().toString(36).slice(2, 7)}`);

    function remapLocalLocks(prevId: string, nextId: string): void {
        if (!prevId || prevId === nextId) return;
        const now = Date.now();
        const entries = Array.from(nodeLocks.entries());
        entries.forEach(([nodeId, lock]) => {
            if (lock.userId !== prevId) return;
            nodeLocks.set(nodeId, { ...lock, userId: nextId, updatedAt: now });
            nodeLockUpdatedAt.set(nodeId, now);
        });
        if (localLockedNodeId) {
            sendNodeSelect('LOCK', localLockedNodeId);
        }
    }

    watch(
        () => authStore.user?.id,
        (nextId) => {
            if (!nextId) return;
            const nextUserId = String(nextId);
            const prevUserId = localUserId.value;
            if (prevUserId !== nextUserId) {
                remapLocalLocks(prevUserId, nextUserId);
                localUserId.value = nextUserId;
            }
        }
    );

    watch(
        () => sceneNodeStore.selectedNodeId,
        (nextId, prevId) => {
            if (prevId && prevId !== nextId && prevId === localLockedNodeId) {
                unlockNode(prevId);
            }
            if (nextId) {
                lockNode(nextId);
            } else {
                localLockedNodeId = null;
            }
        }
    );

    const localParticipant = computed<CollabParticipant>(() => ({
        odps: localUserId.value,
        name: authStore.user?.name || 'Guest',
        avatarUrl: authStore.user?.profileImageUrl ?? undefined,
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

    let deviceChangeBound = false;
    const handleDeviceChange = () => {
        void loadAudioInputDevices();
    };

    async function loadAudioInputDevices() {
        if (!navigator?.mediaDevices?.enumerateDevices) return;
        try {
            const devices = await navigator.mediaDevices.enumerateDevices();
            const inputs = devices.filter((device) => device.kind === 'audioinput');
            audioInputDevices.value = inputs;

            if (inputs.length === 0) {
                selectedMicId.value = '';
                return;
            }

            if (!selectedMicId.value || !inputs.some((device) => device.deviceId === selectedMicId.value)) {
                selectedMicId.value = inputs[0]?.deviceId ?? '';
            }

            if (!deviceChangeBound && navigator.mediaDevices.addEventListener) {
                navigator.mediaDevices.addEventListener('devicechange', handleDeviceChange);
                deviceChangeBound = true;
            } else if (!deviceChangeBound) {
                navigator.mediaDevices.ondevicechange = handleDeviceChange;
                deviceChangeBound = true;
            }
        } catch (error) {
            console.warn('Failed to load audio input devices', error);
        }
    }

    async function selectMicrophone(deviceId: string) {
        selectedMicId.value = deviceId;
        if (deviceId) {
            localStorage.setItem('collab:micId', deviceId);
        } else {
            localStorage.removeItem('collab:micId');
        }

        if (!isMediaConnected.value || !rtcJoined.value) return;
        const stream = await peerConnectionService.switchMicrophone(deviceId);
        if (stream) {
            localStream.value = stream;
            stopSpeakingMonitor(localUserId.value);
            await startSpeakingMonitor(localUserId.value, stream);
            if (isMuted.value) {
                peerConnectionService.toggleMute(true);
            }
        }
    }

    // ================================
    // Actions: Room Management
    // ================================

    /**
     * Join a project room (Signaling Only)
     */
    async function joinRoom(projectId: number): Promise<void> {
        const nextRoomId = `project-${projectId}`;
        const nextProjectId = projectId;

        if (status.value !== 'disconnected' && roomId.value && roomId.value !== nextRoomId) {
            leaveRoom();
        }

        if (roomId.value === nextRoomId && status.value !== 'disconnected') {
            return;
        }

        if (status.value === 'connected' || status.value === 'connecting') return;

        status.value = 'connecting';
        roomId.value = nextRoomId;
        currentProjectId.value = nextProjectId;
        localStorage.setItem(STORAGE_KEY, nextRoomId);
        const storedLastRead = localStorage.getItem(getReadStorageKey(nextProjectId));
        lastReadAt.value = storedLastRead ? Number(storedLastRead) || 0 : 0;

        try {
            void loadAudioInputDevices();
            // 1. Connect WebSocket
            if (!socketManager.getClient().connected) {
                socketManager.connect();
            }

            // 2. Subscribe to room (legacy signaling)
            socketManager.subscribeToRoom(roomId.value);
            // 2-1. Subscribe to chat (projectId)
            socketManager.subscribeToChat(String(nextProjectId), handleChatMessage);
            // 2-2. Subscribe to presence (projectId)
            socketManager.subscribeToPresence(String(nextProjectId), handlePresenceMessage);
            // 2-3. Subscribe to RTC signaling + errors
            socketManager.subscribeToRTC(String(nextProjectId), handleRtcMessage);
            socketManager.subscribeToErrors(handleRtcError);

            // 3. Setup WebRTC Callbacks (Prepare for later)
            peerConnectionService.setCallbacks({
                onTrack: handleRemoteTrack,
                onIceCandidate: (candidate, peerId) => handleLocalIceCandidate(peerId, candidate),
                onConnectionStateChange: handleConnectionStateChange,
            });

            // 4. Broadcast Join + Presence after connection is ready
            socketManager.onConnected(() => {
                status.value = 'connected';
                announcePresence();
            });

            // 5. Load chat history (optional)
            void loadChatHistory(nextProjectId);

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
        if (isMediaConnected.value || rtcJoinPending.value || rtcJoined.value) {
            isAutoStarting.value = false;
            return;
        }

        // Backend spec: max 6 participants for audio mesh
        if (participants.value.length > 6) {
            console.warn('[RTC] Room is full (max 6 participants)');
            alert('협업 통화는 최대 6명까지 참여할 수 있습니다.');
            isAutoStarting.value = false;
            return;
        }

        try {
            // 1. Get Local Stream
            const stream = await peerConnectionService.getLocalStream({
                video: false,
                audio: true,
                audioDeviceId: selectedMicId.value || undefined,
            });
            localStream.value = stream;
            if (stream) {
                await startSpeakingMonitor(localUserId.value, stream);
            }
            await loadAudioInputDevices();

            // 2. Join RTC room (server will return JOIN_ACK with participant list)
            if (currentProjectId.value === null) {
                rtcJoinPending.value = false;
                return;
            }
            rtcJoinPending.value = true;
            socketManager.sendRTC(String(currentProjectId.value), {
                type: 'JOIN',
                projectId: currentProjectId.value,
            });
        } catch (e) {
            console.error('Failed to enable media:', e);
            rtcJoinPending.value = false;
        } finally {
            isAutoStarting.value = false;
        }
    }

    /**
     * Disable Media - "End Call"
     */
    function disableMedia() {
        if (!isMediaConnected.value && !rtcJoinPending.value && !rtcJoined.value) return;

        if (rtcJoined.value && currentProjectId.value !== null) {
            socketManager.sendRTC(String(currentProjectId.value), {
                type: 'LEAVE',
                projectId: currentProjectId.value,
            });
        }

        // Close all peer connections but keep socket
        peerConnectionService.closeAll();
        // peerConnectionService.stopLocalStream(); // Assuming this method exists or handled in closeAll

        isMediaConnected.value = false;
        isMuted.value = false;
        localStream.value = null;
        isPanelOpen.value = false;
        rtcJoinPending.value = false;
        rtcJoined.value = false;
        rtcPeers.clear();
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

        if (currentProjectId.value !== null) {
            socketManager.unsubscribeChat(String(currentProjectId.value));
            socketManager.unsubscribePresence(String(currentProjectId.value));
            socketManager.unsubscribeRTC();
            socketManager.unsubscribeErrors();
        }
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
        currentProjectId.value = null;
        participants.value = [];
        messages.value = [];
        lastReadAt.value = 0;
        cursors.clear();
        lastCursorSentAt = 0;
        lastNodeMoveSentAt.clear();
        lastRemoteNodeMoveAt.clear();
        localDraggingNodes.clear();
        nodeLocks.clear();
        nodeLockUpdatedAt.clear();
        localLockedNodeId = null;
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
                await handleRemoteIceCandidate(senderId, payload);
                break;
            case 'cursor':
                if (payload?.sceneId == null) break;
                if (!Number.isFinite(payload?.x) || !Number.isFinite(payload?.y)) break;
                handleCursorUpdate(senderId, {
                    x: Number(payload.x),
                    y: Number(payload.y),
                    sceneId: Number(payload.sceneId),
                });
                break;
            case 'media_stop':
                handleMediaStop(senderId);
                break;
            case 'state_update':
                handleStateUpdate(senderId, payload);
                break;
        }
    }

    async function handleRtcMessage(message: any) {
        const type = String(message?.type || '').toUpperCase();
        const senderId = message?.senderId != null ? String(message.senderId) : '';

        switch (type) {
            case 'JOIN_ACK':
                await handleRtcJoinAck(message);
                break;
            case 'JOIN':
                handleRtcJoin(senderId, message);
                break;
            case 'LEAVE':
                handleRtcLeave(senderId);
                break;
            case 'MUTE':
                handleRtcMute(senderId, message?.muted);
                break;
            case 'OFFER':
                if (message?.sdp) {
                    await handleOffer(senderId, message.sdp);
                }
                break;
            case 'ANSWER':
                if (message?.sdp) {
                    await handleAnswer(senderId, message.sdp);
                }
                break;
            case 'CANDIDATE':
                if (message?.candidate) {
                    await handleRemoteIceCandidate(senderId, message.candidate);
                }
                break;
            default:
                break;
        }
    }

    function handleRtcError(error: any) {
        const code = error?.code ?? 'RTC_ERROR';
        const message = error?.message ?? 'RTC error';
        console.warn('[RTC] Error:', code, message);
        alert(message);
        cleanupRtcState();
        isAutoStarting.value = false;
    }

    async function handleRtcJoinAck(message: any) {
        if (!rtcJoinPending.value && !rtcJoined.value) return;
        if (rtcJoined.value) return;
        rtcJoinPending.value = false;
        rtcJoined.value = true;
        isMediaConnected.value = true;
        isMuted.value = false;
        peerConnectionService.toggleMute(false);

        const participantsList = Array.isArray(message?.participants) ? message.participants : [];
        for (const participant of participantsList) {
            const peerId = participant?.userId != null ? String(participant.userId) : null;
            if (!peerId || peerId === localParticipant.value.odps) continue;

            rtcPeers.add(peerId);
            updateParticipantMute(peerId, participant?.muted);
            peerConnectionService.createPeerConnection(peerId);
            const offer = await peerConnectionService.createOffer(peerId);
            if (offer && currentProjectId.value !== null) {
                socketManager.sendRTC(String(currentProjectId.value), {
                    type: 'OFFER',
                    projectId: currentProjectId.value,
                    targetId: Number(peerId),
                    sdp: offer,
                });
            }
        }
    }

    function handleRtcJoin(peerId: string, payload: any) {
        if (!peerId || peerId === localParticipant.value.odps) return;
        rtcPeers.add(peerId);
        updateParticipantMute(peerId, payload?.muted);
    }

    function handleRtcLeave(peerId: string) {
        if (!peerId) return;
        rtcPeers.delete(peerId);
        peerConnectionService.removePeer(peerId);
        stopSpeakingMonitor(peerId);
        remoteVolumeMap.delete(peerId);
        const audio = document.getElementById(`audio-${peerId}`);
        if (audio) audio.remove();
        updateParticipantMute(peerId, undefined);
    }

    function handleRtcMute(peerId: string, muted: boolean | null | undefined) {
        if (!peerId || muted == null) return;
        updateParticipantMute(peerId, muted);
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
            if (currentProjectId.value !== null) {
                socketManager.sendRTC(String(currentProjectId.value), {
                    type: 'ANSWER',
                    projectId: currentProjectId.value,
                    targetId: Number(peerId),
                    sdp: answer,
                });
            }
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
    function handleLocalIceCandidate(peerId: string, candidate: RTCIceCandidateInit) {
        if (currentProjectId.value === null) return;
        socketManager.sendRTC(String(currentProjectId.value), {
            type: 'CANDIDATE',
            projectId: currentProjectId.value,
            targetId: Number(peerId),
            candidate,
        });
    }

    async function handleRemoteIceCandidate(peerId: string, candidate: RTCIceCandidateInit) {
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
        audio.volume = getRemoteVolume(peerId);
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
        if (!canSendSignal() || currentProjectId.value === null) return;

        // Backend spec: max 2000 characters
        if (content.length > 2000) {
            console.warn('[Chat] Message too long');
            alert('메시지는 최대 2000자까지 입력할 수 있습니다.');
            return;
        }

        const now = Date.now();
        const localId = `local-${now}`;

        // Publish to chat topic (spec)
        socketManager.sendChat(String(currentProjectId.value), {
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
        updateLastReadAt(now);
    }

    function updateLastReadAt(nextValue: number) {
        if (!Number.isFinite(nextValue)) return;
        if (nextValue <= lastReadAt.value) return;
        lastReadAt.value = nextValue;
        if (currentProjectId.value !== null) {
            localStorage.setItem(getReadStorageKey(currentProjectId.value), String(lastReadAt.value));
        }
    }

    function markChatRead() {
        const latest = messages.value.reduce((max, m) => Math.max(max, m.timestamp ?? 0), 0);
        updateLastReadAt(latest);
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

    function updateCursor(x: number, y: number) {
        if (!authStore.isAuthenticated) return;
        if (currentProjectId.value === null || currentSceneId.value === null) return;

        const now = Date.now();
        if (now - lastCursorSentAt < CURSOR_THROTTLE_MS) return;
        lastCursorSentAt = now;

        if (!socketManager.getClient().connected || status.value !== 'connected') return;

        socketManager.sendPresence(String(currentProjectId.value), {
            type: 'CURSOR',
            sceneId: currentSceneId.value,
            x: Number.isFinite(x) ? x : 0,
            y: Number.isFinite(y) ? y : 0,
        });
    }

    function setFlowToScreenCoordinate(
        transform: ((pos: { x: number; y: number }) => { x: number; y: number }) | null
    ): void {
        flowToScreenCoordinate.value = transform;
    }

    function startNodeDrag(nodeId: string): void {
        if (!nodeId) return;
        localDraggingNodes.add(nodeId);
    }

    function stopNodeDrag(nodeId: string): void {
        if (!nodeId) return;
        localDraggingNodes.delete(nodeId);
    }

    function updateNodeMove(nodeId: string, x: number, y: number, force = false): void {
        if (!authStore.isAuthenticated) return;
        if (currentProjectId.value === null || currentSceneId.value === null) return;
        if (!socketManager.getClient().connected || status.value !== 'connected') return;
        const numericNodeId = Number(nodeId);
        if (!Number.isFinite(numericNodeId) || numericNodeId <= 0) return;

        const now = Date.now();
        if (!force) {
            const lastSent = lastNodeMoveSentAt.get(nodeId) ?? 0;
            if (now - lastSent < NODE_MOVE_THROTTLE_MS) return;
            lastNodeMoveSentAt.set(nodeId, now);
        }

        socketManager.sendPresence(String(currentProjectId.value), {
            type: 'NODE_MOVE',
            sceneId: currentSceneId.value,
            nodeId: numericNodeId,
            x,
            y,
        });
    }

    function finishNodeDrag(nodeId: string, x: number, y: number): void {
        updateNodeMove(nodeId, x, y, true);
        stopNodeDrag(nodeId);
    }

    function resolveLockName(userId: string, name?: string | null): string {
        const rawName = typeof name === 'string' ? name.trim() : '';
        if (rawName && rawName !== 'Guest') {
            return rawName;
        }
        const participantName = participants.value.find((participant) => participant.odps === userId)?.name;
        const fallbackName = typeof participantName === 'string' ? participantName.trim() : '';
        if (fallbackName) {
            return fallbackName;
        }
        return rawName || 'Guest';
    }

    function applyNodeLock(
        nodeId: string,
        lock: { userId: string; name: string; sceneId: number; updatedAt: number }
    ): void {
        const nextUpdatedAt = Number.isFinite(lock.updatedAt) ? lock.updatedAt : Date.now();
        const lastUpdatedAt = nodeLockUpdatedAt.get(nodeId) ?? 0;
        if (nextUpdatedAt < lastUpdatedAt) return;
        const resolvedName = resolveLockName(lock.userId, lock.name);
        nodeLockUpdatedAt.set(nodeId, nextUpdatedAt);
        nodeLocks.set(nodeId, { ...lock, name: resolvedName, updatedAt: nextUpdatedAt });
    }

    function releaseNodeLock(nodeId: string, userId?: string, updatedAt?: number): void {
        const nextUpdatedAt = Number.isFinite(updatedAt) ? (updatedAt as number) : Date.now();
        const lastUpdatedAt = nodeLockUpdatedAt.get(nodeId) ?? 0;
        if (nextUpdatedAt < lastUpdatedAt) return;
        const existing = nodeLocks.get(nodeId);
        if (userId && existing && existing.userId !== userId) {
            return;
        }
        nodeLockUpdatedAt.set(nodeId, nextUpdatedAt);
        nodeLocks.delete(nodeId);
    }

    function clearLocksByUser(userId: string): void {
        const entries = Array.from(nodeLocks.entries());
        entries.forEach(([nodeId, lock]) => {
            if (lock.userId === userId) {
                nodeLocks.delete(nodeId);
                nodeLockUpdatedAt.delete(nodeId);
            }
        });
    }

    function sendNodeSelect(action: 'LOCK' | 'UNLOCK', nodeId: string): void {
        if (!authStore.isAuthenticated) return;
        if (currentProjectId.value === null || currentSceneId.value === null) return;
        if (!socketManager.getClient().connected || status.value !== 'connected') return;
        const numericNodeId = Number(nodeId);
        if (!Number.isFinite(numericNodeId) || numericNodeId <= 0) return;

        socketManager.sendPresence(String(currentProjectId.value), {
            type: 'NODE_SELECT',
            sceneId: currentSceneId.value,
            nodeId: numericNodeId,
            action,
        });
    }

    function lockNode(nodeId: string): void {
        if (!nodeId || currentSceneId.value === null) return;
        if (currentProjectId.value === null || status.value !== 'connected') return;
        if (isNodeLockedByOther(nodeId)) return;
        localLockedNodeId = nodeId;
        const ownerId = authStore.user?.id != null ? String(authStore.user.id) : localUserId.value;
        applyNodeLock(nodeId, {
            userId: ownerId,
            name: localParticipant.value.name,
            sceneId: currentSceneId.value,
            updatedAt: Date.now(),
        });
        sendNodeSelect('LOCK', nodeId);
    }

    function unlockNode(nodeId: string): void {
        if (!nodeId) return;
        const existing = nodeLocks.get(nodeId);
        if (localLockedNodeId !== nodeId && existing?.userId !== localUserId.value) {
            return;
        }
        if (localLockedNodeId === nodeId) {
            localLockedNodeId = null;
        }
        releaseNodeLock(nodeId, localUserId.value, Date.now());
        sendNodeSelect('UNLOCK', nodeId);
    }

    function getNodeLock(nodeId: string): { userId: string; name: string; sceneId: number; updatedAt: number } | null {
        const lock = nodeLocks.get(nodeId);
        if (!lock) return null;
        const resolvedName = resolveLockName(lock.userId, lock.name);
        if (resolvedName !== lock.name) {
            nodeLocks.set(nodeId, { ...lock, name: resolvedName });
        }
        return { ...lock, name: resolvedName };
    }

    function isNodeLockedByOther(nodeId: string): boolean {
        const lock = nodeLocks.get(nodeId);
        if (!lock) return false;
        if (participants.value.length <= 1) return false;
        const authId = authStore.user?.id != null ? String(authStore.user.id) : null;
        if (lock.userId === localUserId.value) return false;
        if (authId && lock.userId === authId) return false;
        if (lock.name && lock.name === localParticipant.value.name) return false;
        const isKnownParticipant = participants.value.some((participant) => participant.odps === lock.userId);
        if (!isKnownParticipant) return false;
        return true;
    }

    // 노드를 잠근 사용자의 커서 색상 반환
    function getNodeLockColor(nodeId: string): string | null {
        const lock = nodeLocks.get(nodeId);
        if (!lock) return null;
        if (!isNodeLockedByOther(nodeId)) return null;
        // 커서 색상 맵에서 해당 사용자의 색상 가져오기
        return cursorColorByUser.get(lock.userId) || null;
    }

    function handleCursorUpdate(peerId: string, payload: { x: number; y: number; sceneId: number }) {
        const color = getOrAssignCursorColor(peerId);
        cursors.set(peerId, {
            x: payload.x,
            y: payload.y,
            color,
            sceneId: payload.sceneId,
        });
    }

    type PresenceLocation = 'PROJECT_LIST' | 'SCENE_LIST' | 'SCENE_EDIT' | 'TIMELINE';

    function updateLocation(location: PresenceLocation, sceneId?: number | null, nodeId?: number | null) {
        currentLocation.value = location;
        const previousSceneId = currentSceneId.value;
        if (previousSceneId !== (sceneId ?? null) && localLockedNodeId) {
            unlockNode(localLockedNodeId);
        }
        currentSceneId.value = sceneId ?? null;
        if (previousSceneId !== currentSceneId.value) {
            cursors.clear();
            lastRemoteNodeMoveAt.clear();
            nodeLocks.clear();
            nodeLockUpdatedAt.clear();
            localLockedNodeId = null;
        }
        currentNodeId.value = nodeId ?? null;
        pendingPresence.value = {
            location,
            sceneId: sceneId ?? null,
            nodeId: nodeId ?? null,
        };

        flushPresence();
        broadcastState();
    }

    function handlePresenceMessage(message: any) {
        const type = message?.type;

        if (type === 'SNAPSHOT') {
            if (message?.projectId && currentProjectId.value && Number(message.projectId) !== currentProjectId.value) {
                return;
            }
            const snapshot = Array.isArray(message?.participants) ? message.participants : [];
            const existing = new Map(participants.value.map((participant) => [participant.odps, participant]));
            const next = snapshot
                .map(mapPresenceToParticipant)
                .filter((participant: CollabParticipant | null): participant is CollabParticipant => !!participant)
                .map((participant: CollabParticipant) => {
                    const prev = existing.get(participant.odps);
                    return prev ? { ...prev, ...participant } : participant;
                });
            participants.value = next;
            return;
        }

        const userId = message?.userId;
        if (!userId) return;
        const peerId = String(userId);
        if (type === 'CURSOR') {
            const sceneId = message?.sceneId;
            if (currentSceneId.value === null || sceneId == null) return;
            if (Number(sceneId) !== currentSceneId.value) return;
            const x = Number(message?.x);
            const y = Number(message?.y);
            if (!Number.isFinite(x) || !Number.isFinite(y)) return;
            handleCursorUpdate(peerId, { x, y, sceneId: Number(sceneId) });
            return;
        }
        if (type === 'NODE_MOVE') {
            if (peerId === localParticipant.value.odps) return;
            const sceneId = message?.sceneId;
            const nodeId = message?.nodeId;
            if (currentSceneId.value === null || sceneId == null || nodeId == null) return;
            if (Number(sceneId) !== currentSceneId.value) return;
            const numericNodeId = Number(nodeId);
            if (!Number.isFinite(numericNodeId) || numericNodeId <= 0) return;
            const nodeKey = String(numericNodeId);
            if (localDraggingNodes.has(nodeKey)) return;
            const x = Number(message?.x);
            const y = Number(message?.y);
            if (!Number.isFinite(x) || !Number.isFinite(y)) return;
            let updatedAt = message?.updatedAt ? Date.parse(message.updatedAt) : Date.now();
            if (!Number.isFinite(updatedAt)) {
                updatedAt = Date.now();
            }
            const lastUpdated = lastRemoteNodeMoveAt.get(nodeKey) ?? 0;
            if (updatedAt <= lastUpdated) return;
            lastRemoteNodeMoveAt.set(nodeKey, updatedAt);
            sceneNodeStore.applyRemoteNodeMove(nodeKey, x, y);
            return;
        }
        if (type === 'NODE_SELECT') {
            const sceneId = message?.sceneId;
            const nodeId = message?.nodeId;
            const action = String(message?.action ?? '').toUpperCase();
            if (currentSceneId.value === null || sceneId == null || nodeId == null) return;
            if (Number(sceneId) !== currentSceneId.value) return;
            if (!action) return;
            const messageUserId = message?.userId != null ? String(message.userId) : peerId;
            const authId = authStore.user?.id != null ? String(authStore.user.id) : null;
            if (messageUserId === localUserId.value) return;
            if (authId && messageUserId === authId) return;
            const nodeKey = String(nodeId);
            let updatedAt = message?.updatedAt ? Date.parse(message.updatedAt) : Date.now();
            if (!Number.isFinite(updatedAt)) {
                updatedAt = Date.now();
            }
            if (action === 'LOCK') {
                applyNodeLock(nodeKey, {
                    userId: messageUserId,
                    name: message?.name ?? '',
                    sceneId: Number(sceneId),
                    updatedAt,
                });
            } else if (action === 'UNLOCK') {
                releaseNodeLock(nodeKey, messageUserId, updatedAt);
            }
            return;
        }
        if (type === 'STATUS') {
            return;
        }
        if (type === 'LEAVE') {
            removeParticipant(peerId);
            return;
        }
        const participant = mapPresenceToParticipant(message);
        if (!participant) return;
        addParticipant(peerId, participant);
    }

    function mapPresenceToParticipant(message: any): CollabParticipant | null {
        const userId = message?.userId;
        if (!userId) return null;
        const peerId = String(userId);
        return {
            odps: peerId,
            name: message?.name ?? 'Guest',
            avatarUrl: message?.profileImageUrl ?? undefined,
            currentLocation: message?.location ?? '',
            sceneId: message?.sceneId ?? null,
            nodeId: message?.nodeId ?? null,
        };
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
        clearLocksByUser(peerId);
        remoteVolumeMap.delete(peerId);
        stopSpeakingMonitor(peerId);
        // Cleanup audio
        const audio = document.getElementById(`audio-${peerId}`);
        if (audio) audio.remove();
    }

    function syncLockNamesFromParticipants(): void {
        if (participants.value.length === 0 || nodeLocks.size === 0) return;
        const now = Date.now();
        nodeLocks.forEach((lock, nodeId) => {
            const resolvedName = resolveLockName(lock.userId, lock.name);
            if (resolvedName === lock.name) return;
            nodeLockUpdatedAt.set(nodeId, Math.max(nodeLockUpdatedAt.get(nodeId) ?? 0, now));
            nodeLocks.set(nodeId, { ...lock, name: resolvedName, updatedAt: now });
        });
    }

    function updateParticipantMute(peerId: string, muted: boolean | undefined | null) {
        if (!peerId) return;
        const idx = participants.value.findIndex(p => p.odps === peerId);
        if (idx === -1) return;
        const next = muted == null ? undefined : muted;
        const existing = participants.value[idx];
        if (!existing) return;
        participants.value[idx] = {
            ...existing,
            isMuted: next,
        };
    }

    function getRemoteVolume(peerId: string): number {
        const volume = remoteVolumeMap.get(peerId);
        return typeof volume === 'number' ? volume : 1;
    }

    function setRemoteVolume(peerId: string, volume: number) {
        const clamped = Math.max(0, Math.min(1, volume));
        remoteVolumeMap.set(peerId, clamped);
        const audio = document.getElementById(`audio-${peerId}`) as HTMLAudioElement | null;
        if (audio) {
            audio.volume = clamped;
        }
    }

    function cleanupRtcState() {
        rtcPeers.forEach((peerId) => {
            stopSpeakingMonitor(peerId);
            const audio = document.getElementById(`audio-${peerId}`);
            if (audio) audio.remove();
        });
        rtcPeers.clear();
        remoteVolumeMap.clear();
        peerConnectionService.closeAll();
        isMediaConnected.value = false;
        isMuted.value = false;
        localStream.value = null;
        rtcJoinPending.value = false;
        rtcJoined.value = false;
        isPanelOpen.value = false;
        stopSpeakingMonitor(localUserId.value);
    }

    function getOrAssignCursorColor(peerId: string): string {
        const existing = cursorColorByUser.get(peerId);
        if (existing) return existing;

        let hash = 0;
        for (let i = 0; i < peerId.length; i += 1) {
            hash = (hash + peerId.charCodeAt(i)) % 2147483647;
        }
        const color = cursorColors[hash % cursorColors.length] ?? '#9CA3AF';
        cursorColorByUser.set(peerId, color);
        return color;
    }

    function toggleMute() {
        isMuted.value = !isMuted.value;
        peerConnectionService.toggleMute(isMuted.value);

        if (isMediaConnected.value && currentProjectId.value !== null) {
            socketManager.sendRTC(String(currentProjectId.value), {
                type: 'MUTE',
                projectId: currentProjectId.value,
                muted: isMuted.value,
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

    function announcePresence() {
        if (!roomId.value || currentProjectId.value === null) return;
        socketManager.sendSignal({
            type: 'join',
            payload: {
                user: localParticipant.value
            }
        });
        const location = currentLocation.value || 'PROJECT_LIST';
        socketManager.sendPresence(String(currentProjectId.value), {
            type: 'LOCATION',
            location,
            sceneId: currentSceneId.value ?? null,
            nodeId: currentNodeId.value ?? null,
        });
        pendingPresence.value = null;
        broadcastState();
    }

    function flushPresence() {
        if (currentProjectId.value === null || !pendingPresence.value) return;
        if (!socketManager.getClient().connected || status.value !== 'connected') return;
        socketManager.sendPresence(String(currentProjectId.value), {
            type: 'LOCATION',
            location: pendingPresence.value.location,
            sceneId: pendingPresence.value.sceneId,
            nodeId: pendingPresence.value.nodeId,
        });
        pendingPresence.value = null;
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

    watch(
        () => participants.value.map((participant) => `${participant.odps}:${participant.name ?? ''}`).join('|'),
        () => {
            syncLockNamesFromParticipants();
        }
    );

    function isSpeaking(peerId: string): boolean {
        return speakingMap.get(peerId) ?? false;
    }

    return {
        // State
        status,
        participants,
        messages,
        cursors,
        nodeLocks,
        isPanelOpen,
        isFloatingBarVisible, // Exported
        isMediaConnected,     // Exported
        isAutoStarting,
        floatingBarResetToken,
        isMuted,
        isVideoOff,
        isScreenSharing,
        localParticipant,
        audioInputDevices,
        selectedMicId,
        // Getters
        isConnected,
        hasUnreadMessages,
        participantCount,
        isSpeaking,
        getRemoteVolume,
        markChatRead,
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
        flowToScreenCoordinate,
        setFlowToScreenCoordinate,
        startNodeDrag,
        updateNodeMove,
        finishNodeDrag,
        getNodeLock,
        isNodeLockedByOther,
        getNodeLockColor,
        updateLocation,
        rejoinIfNeeded,
        selectMicrophone,
        setRemoteVolume,
    };
});
