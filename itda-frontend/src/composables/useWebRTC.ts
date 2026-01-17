import { ref, onUnmounted } from 'vue';
import { useCollabStore } from '../stores/collab';
import { signalingService } from '../services/webrtc/signaling';
import { peerConnectionService } from '../services/webrtc/peerConnection';

/**
 * WebRTC Composable (Stub)
 * 
 * 백엔드 완료 후 실제 WebRTC 로직 구현 예정
 * 현재는 Store 연동 및 기본 구조만 제공
 */
export function useWebRTC() {
    const collabStore = useCollabStore();

    const isInitialized = ref(false);
    const localStream = ref<MediaStream | null>(null);
    const remoteStreams = ref<Map<string, MediaStream>>(new Map());

    /**
     * WebRTC 초기화 및 방 입장
     */
    async function joinRoom(projectId: number): Promise<void> {
        console.log('[useWebRTC] Joining room for project:', projectId);

        // Store 업데이트
        collabStore.joinRoom(projectId);

        // 시그널링 연결 (Stub)
        signalingService.connect(`project-${projectId}`, {
            onMessage: handleSignalingMessage,
            onConnected: () => {
                console.log('[useWebRTC] Signaling connected');
                isInitialized.value = true;
            },
            onDisconnected: () => {
                console.log('[useWebRTC] Signaling disconnected');
                isInitialized.value = false;
            },
            onError: (error) => {
                console.error('[useWebRTC] Signaling error:', error);
            },
        });
    }

    /**
     * 방 퇴장 및 정리
     */
    function leaveRoom(): void {
        console.log('[useWebRTC] Leaving room');

        // 모든 연결 정리
        peerConnectionService.closeAll();
        signalingService.disconnect();

        // Store 업데이트
        collabStore.leaveRoom();

        // Local state 정리
        localStream.value = null;
        remoteStreams.value.clear();
        isInitialized.value = false;
    }

    /**
     * 마이크 토글
     */
    function toggleMute(): void {
        collabStore.toggleMute();
        peerConnectionService.toggleMute(collabStore.isMuted);
    }

    /**
     * 비디오 토글
     */
    function toggleVideo(): void {
        collabStore.toggleVideo();
        peerConnectionService.toggleVideo(collabStore.isVideoOff);
    }

    /**
     * 화면 공유 토글
     */
    async function toggleScreenShare(): Promise<void> {
        if (collabStore.isScreenSharing) {
            peerConnectionService.stopScreenShare();
            collabStore.toggleScreenShare();
        } else {
            const stream = await peerConnectionService.startScreenShare();
            if (stream) {
                collabStore.toggleScreenShare();
                // TODO: 화면 공유 스트림을 peers에게 전송
            }
        }
    }

    /**
     * 시그널링 메시지 핸들러
     */
    function handleSignalingMessage(message: { type: string; payload: unknown }): void {
        console.log('[useWebRTC] Received signaling message:', message.type);

        switch (message.type) {
            case 'offer':
                // TODO: Handle offer
                break;
            case 'answer':
                // TODO: Handle answer
                break;
            case 'ice-candidate':
                // TODO: Handle ICE candidate
                break;
            case 'join':
                // TODO: Handle participant join
                break;
            case 'leave':
                // TODO: Handle participant leave
                break;
        }
    }

    // Cleanup on unmount
    onUnmounted(() => {
        if (isInitialized.value) {
            leaveRoom();
        }
    });

    return {
        // State
        isInitialized,
        localStream,
        remoteStreams,
        // Store shortcuts
        isConnected: () => collabStore.isConnected,
        isMuted: () => collabStore.isMuted,
        isVideoOff: () => collabStore.isVideoOff,
        isScreenSharing: () => collabStore.isScreenSharing,
        participants: () => collabStore.participants,
        // Actions
        joinRoom,
        leaveRoom,
        toggleMute,
        toggleVideo,
        toggleScreenShare,
    };
}
