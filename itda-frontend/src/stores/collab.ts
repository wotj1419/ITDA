import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { CollabParticipant, CollabMessage, CollabStatus } from '../types/ui';
import { signalingService, type CollabWsMessage } from '../services/webrtc/signaling'
import { peerConnectionService } from '../services/webrtc/peerConnection'

/**
 * 협업 상태 관리 스토어
 * 
 * Discord 허들 스타일의 실시간 협업 기능을 위한 상태 관리
 * - 참여자 관리
 * - 마이크/카메라/화면공유 상태
 * - 실시간 채팅
 */
export const useCollabStore = defineStore('collab', () => {
    // ================================
    // State
    // ================================
    const status = ref<CollabStatus>('disconnected');
    const participants = ref<CollabParticipant[]>([]);
    const messages = ref<CollabMessage[]>([]);
    const isPanelOpen = ref(false);

    // Identity
    const clientId = ref<string | null>(null)
    const displayName = ref<string>('ME')

    // Media
    const localStream = ref<MediaStream | null>(null)
    const remoteStreams = ref<Record<string, MediaStream>>({})

    // Local user state
    const isMuted = ref(true);
    const isVideoOff = ref(true);
    const isScreenSharing = ref(false);
    const currentLocation = ref('');

    // Room
    const roomId = ref<string | null>(null);

    const seenMessageIds = new Set<string>()
    let lastCursorSentAt = 0

    // ================================
    // Getters
    // ================================
    const isConnected = computed(() => status.value === 'connected');
    const participantCount = computed(() => participants.value.length);
    const hasUnreadMessages = computed(() => messages.value.length > 0);

    const localParticipant = computed<CollabParticipant>(() => ({
        odps: clientId.value ?? 'me',
        name: displayName.value || 'ME',
        isMuted: isMuted.value,
        isVideoOff: isVideoOff.value,
        isScreenSharing: isScreenSharing.value,
        currentLocation: currentLocation.value,
    }));

    // ================================
    // Actions
    // ================================

    /**
     * 협업 방 입장
     */
    async function joinRoom(projectId: number, options?: { name?: string }): Promise<void> {
        await joinRoomByRoomId(`project-${projectId}`, options)
    }

    async function joinRoomByRoomId(nextRoomId: string, options?: { name?: string }): Promise<void> {
        if (status.value === 'connected' && roomId.value === nextRoomId) return

        leaveRoom()

        roomId.value = nextRoomId
        status.value = 'connecting'

        const storedClientId = sessionStorage.getItem('collab.clientId')
        const nextClientId = storedClientId || crypto.randomUUID()
        sessionStorage.setItem('collab.clientId', nextClientId)
        clientId.value = nextClientId

        const storedName = localStorage.getItem('collab.displayName')
        const nextName = options?.name?.trim() || storedName || `User-${nextClientId.slice(0, 4)}`
        localStorage.setItem('collab.displayName', nextName)
        displayName.value = nextName

        signalingService.connect(nextRoomId, {
            onConnected: () => {
                signalingService.send('hello', { clientId: nextClientId, name: nextName })
            },
            onDisconnected: () => {
                status.value = 'disconnected'
            },
            onError: (error) => {
                console.error('[collab] ws error', error)
                status.value = 'error'
            },
            onMessage: (msg) => {
                void handleWsMessage(msg)
            },
        })
    }

    /**
     * 협업 방 퇴장
     */
    function leaveRoom(): void {
        status.value = 'disconnected';
        roomId.value = null;
        participants.value = [];
        messages.value = [];
        isPanelOpen.value = false;
        localStream.value = null
        remoteStreams.value = {}
        seenMessageIds.clear()
        // Reset local state
        isMuted.value = true;
        isVideoOff.value = true;
        isScreenSharing.value = false;

        peerConnectionService.closeAll()
        signalingService.disconnect()
    }

    /**
     * 마이크 음소거 토글
     */
    function toggleMute(): void {
        isMuted.value = !isMuted.value;
        peerConnectionService.toggleMute(isMuted.value)
        sendPresenceUpdate()
    }

    /**
     * 비디오 토글
     */
    function toggleVideo(): void {
        isVideoOff.value = !isVideoOff.value;
        peerConnectionService.toggleVideo(isVideoOff.value)
        sendPresenceUpdate()
    }

    /**
     * 화면 공유 토글
     */
    function toggleScreenShare(): void {
        isScreenSharing.value = !isScreenSharing.value;
        sendPresenceUpdate()
    }

    /**
     * 패널 열기/닫기
     */
    function togglePanel(): void {
        isPanelOpen.value = !isPanelOpen.value;
    }

    /**
     * 채팅 메시지 전송
     */
    function sendMessage(content: string): void {
        if (!content.trim()) return;

        const message: CollabMessage = {
            messageId: `msg-${Date.now()}`,
            senderId: clientId.value ?? 'me',
            senderName: displayName.value || 'ME',
            content: content.trim(),
            timestamp: Date.now(),
        };
        messages.value.push(message);
        seenMessageIds.add(message.messageId)
        signalingService.send('chat.send', { messageId: message.messageId, content: message.content })
    }

    /**
     * 현재 위치 업데이트
     */
    function updateLocation(location: string): void {
        currentLocation.value = location;
        sendPresenceUpdate()
    }

    function updateCursor(x: number, y: number): void {
        const now = Date.now()
        if (now - lastCursorSentAt < 40) return
        lastCursorSentAt = now
        signalingService.send('cursor.move', { x, y })
    }

    /**
     * 참여자 추가 (외부 이벤트용)
     */
    function addParticipant(participant: CollabParticipant): void {
        const exists = participants.value.find(p => p.odps === participant.odps);
        if (!exists) {
            participants.value.push(participant);
        }
    }

    /**
     * 참여자 제거 (외부 이벤트용)
     */
    function removeParticipant(odps: string): void {
        participants.value = participants.value.filter(p => p.odps !== odps);
    }

    /**
     * 메시지 수신 (외부 이벤트용)
     */
    function receiveMessage(message: CollabMessage): void {
        messages.value.push(message);
    }

    function setRemoteStream(peerId: string, stream: MediaStream): void {
        remoteStreams.value = { ...remoteStreams.value, [peerId]: stream }
    }

    function removeRemoteStream(peerId: string): void {
        const next = { ...remoteStreams.value }
        delete next[peerId]
        remoteStreams.value = next
    }

    function updateParticipantState(odps: string, patch: Partial<CollabParticipant>): void {
        const idx = participants.value.findIndex((p) => p.odps === odps)
        if (idx === -1) return
        participants.value[idx] = { ...participants.value[idx], ...patch }
    }

    function sendPresenceUpdate(): void {
        if (status.value !== 'connected') return
        signalingService.send('presence.update', {
            isMuted: isMuted.value,
            isVideoOff: isVideoOff.value,
            isScreenSharing: isScreenSharing.value,
            currentLocation: currentLocation.value,
        })
    }

    async function ensureLocalAudio(): Promise<MediaStream | null> {
        if (localStream.value) return localStream.value
        const stream = await peerConnectionService.getLocalStream({ audio: true, video: false })
        if (stream) {
            localStream.value = stream
            peerConnectionService.toggleMute(isMuted.value)
        }
        return stream
    }

    function ensurePeer(peerId: string) {
        peerConnectionService.createPeerConnection(
            peerId,
            {},
            {
                onTrack: (stream, pid) => setRemoteStream(pid, stream),
                onIceCandidate: (candidate, pid) => {
                    signalingService.send('webrtc.ice', { candidate: candidate.toJSON() }, pid)
                },
                onConnectionStateChange: (state, pid) => {
                    if (state === 'failed' || state === 'closed') {
                        removeRemoteStream(pid)
                        peerConnectionService.closePeer(pid)
                    }
                },
            }
        )
    }

    async function handleWsMessage(msg: CollabWsMessage): Promise<void> {
        switch (msg.type) {
            case 'welcome': {
                const data = msg.data as any
                status.value = 'connected'

                if (data?.self?.clientId) clientId.value = data.self.clientId
                if (data?.self?.name) displayName.value = data.self.name

                participants.value = Array.isArray(data?.peers)
                    ? data.peers.map((p: any) => ({
                          odps: p.clientId,
                          name: p.name,
                          isMuted: p.state?.isMuted,
                          isVideoOff: p.state?.isVideoOff,
                          isScreenSharing: p.state?.isScreenSharing,
                          currentLocation: p.state?.currentLocation,
                          cursor: p.state?.cursor ? { x: p.state.cursor.x, y: p.state.cursor.y } : undefined,
                      }))
                    : []

                const stream = await ensureLocalAudio()
                if (!stream) {
                    console.warn('[collab] local audio denied/unavailable')
                    return
                }

                for (const p of participants.value) {
                    ensurePeer(p.odps)
                    const offer = await peerConnectionService.createOffer(p.odps)
                    signalingService.send('webrtc.offer', { sdp: offer }, p.odps)
                }
                return
            }
            case 'presence.join': {
                const p = msg.data as any
                if (!p?.clientId) return
                addParticipant({
                    odps: p.clientId,
                    name: p.name,
                    isMuted: p.state?.isMuted,
                    isVideoOff: p.state?.isVideoOff,
                    isScreenSharing: p.state?.isScreenSharing,
                    currentLocation: p.state?.currentLocation,
                })
                return
            }
            case 'presence.leave': {
                const p = msg.data as any
                if (!p?.clientId) return
                removeParticipant(p.clientId)
                removeRemoteStream(p.clientId)
                peerConnectionService.closePeer(p.clientId)
                return
            }
            case 'presence.update': {
                const senderId = msg.sender?.clientId
                if (!senderId) return
                const d = msg.data as any
                updateParticipantState(senderId, {
                    isMuted: d?.isMuted,
                    isVideoOff: d?.isVideoOff,
                    isScreenSharing: d?.isScreenSharing,
                    currentLocation: d?.currentLocation,
                })
                return
            }
            case 'chat.message': {
                const senderId = msg.sender?.clientId ?? 'unknown'
                const senderName = msg.sender?.name ?? 'Unknown'
                const d = msg.data as any
                const messageId = String(d?.messageId ?? `srv-${Date.now()}`)
                if (seenMessageIds.has(messageId)) return
                seenMessageIds.add(messageId)
                receiveMessage({
                    messageId,
                    senderId,
                    senderName,
                    content: String(d?.content ?? ''),
                    timestamp: msg.ts ?? Date.now(),
                    type: 'chat',
                })
                return
            }
            case 'cursor.update': {
                const senderId = msg.sender?.clientId
                const d = msg.data as any
                if (!senderId || typeof d?.x !== 'number' || typeof d?.y !== 'number') return
                updateParticipantState(senderId, {
                    cursor: { x: d.x, y: d.y },
                })
                return
            }
            case 'webrtc.offer': {
                const from = msg.sender?.clientId
                const d = msg.data as any
                if (!from || !d?.sdp) return

                const stream = await ensureLocalAudio()
                if (!stream) return

                ensurePeer(from)
                const answer = await peerConnectionService.handleOffer(from, d.sdp)
                signalingService.send('webrtc.answer', { sdp: answer }, from)
                return
            }
            case 'webrtc.answer': {
                const from = msg.sender?.clientId
                const d = msg.data as any
                if (!from || !d?.sdp) return
                await peerConnectionService.handleAnswer(from, d.sdp)
                return
            }
            case 'webrtc.ice': {
                const from = msg.sender?.clientId
                const d = msg.data as any
                if (!from || !d?.candidate) return
                await peerConnectionService.addIceCandidate(from, d.candidate)
                return
            }
            case 'error': {
                console.warn('[collab] server error', msg.data)
                status.value = 'error'
                return
            }
            default:
                return
        }
    }

    return {
        // State
        status,
        participants,
        messages,
        isPanelOpen,
        clientId,
        displayName,
        localStream,
        remoteStreams,
        isMuted,
        isVideoOff,
        isScreenSharing,
        currentLocation,
        roomId,
        // Getters
        isConnected,
        participantCount,
        hasUnreadMessages,
        localParticipant,
        // Actions
        joinRoom,
        joinRoomByRoomId,
        leaveRoom,
        toggleMute,
        toggleVideo,
        toggleScreenShare,
        togglePanel,
        sendMessage,
        updateLocation,
        updateCursor,
        addParticipant,
        removeParticipant,
        receiveMessage,
    };
});
