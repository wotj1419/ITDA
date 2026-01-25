import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { CollabParticipant, CollabMessage, CollabStatus } from '../types/ui';

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

    // Local user state
    const isMuted = ref(true);
    const isVideoOff = ref(true);
    const isScreenSharing = ref(false);
    const currentLocation = ref('');

    // Room
    const roomId = ref<string | null>(null);

    // ================================
    // Getters
    // ================================
    const isConnected = computed(() => status.value === 'connected');
    const participantCount = computed(() => participants.value.length);
    const hasUnreadMessages = computed(() => messages.value.length > 0);

    const localParticipant = computed<CollabParticipant>(() => ({
        odps: 'me',
        name: 'ME',
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
    function joinRoom(projectId: number): void {
        roomId.value = `project-${projectId}`;
        status.value = 'connecting';

        // Simulate connection (백엔드 연동 후 실제 연결로 대체)
        setTimeout(() => {
            status.value = 'connected';
            // Add mock participants for UI testing
            participants.value = [
                {
                    odps: 'user-sj',
                    name: 'SJ',
                    isMuted: false,
                    isVideoOff: false,
                    isScreenSharing: false,
                    currentLocation: 'Scene 1 편집 중',
                },
            ];
        }, 500);
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
        // Reset local state
        isMuted.value = true;
        isVideoOff.value = true;
        isScreenSharing.value = false;
    }

    /**
     * 마이크 음소거 토글
     */
    function toggleMute(): void {
        isMuted.value = !isMuted.value;
        // TODO: peerConnectionService.toggleMute(isMuted.value)
    }

    /**
     * 비디오 토글
     */
    function toggleVideo(): void {
        isVideoOff.value = !isVideoOff.value;
        // TODO: peerConnectionService.toggleVideo(isVideoOff.value)
    }

    /**
     * 화면 공유 토글
     */
    function toggleScreenShare(): void {
        isScreenSharing.value = !isScreenSharing.value;
        // TODO: 실제 화면 공유 로직
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
            senderId: 'me',
            senderName: 'ME',
            content: content.trim(),
            timestamp: Date.now(),
        };
        messages.value.push(message);
        // TODO: 실제 메시지 브로드캐스트
    }

    /**
     * 현재 위치 업데이트
     */
    function updateLocation(location: string): void {
        currentLocation.value = location;
        // TODO: Broadcast to other participants
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

    return {
        // State
        status,
        participants,
        messages,
        isPanelOpen,
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
        leaveRoom,
        toggleMute,
        toggleVideo,
        toggleScreenShare,
        togglePanel,
        sendMessage,
        updateLocation,
        addParticipant,
        removeParticipant,
        receiveMessage,
    };
});
