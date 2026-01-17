/**
 * WebRTC Signaling Service (Stub)
 * 
 * 백엔드 완료 후 실제 WebSocket 시그널링 구현 예정
 * 현재는 UI 테스트를 위한 Mock 구현
 */

export interface SignalingMessage {
    type: 'offer' | 'answer' | 'ice-candidate' | 'join' | 'leave';
    payload: unknown;
    senderId: string;
    targetId?: string;
}

export interface SignalingCallbacks {
    onMessage: (message: SignalingMessage) => void;
    onConnected: () => void;
    onDisconnected: () => void;
    onError: (error: Error) => void;
}

class SignalingService {
    private callbacks: SignalingCallbacks | null = null;
    private isConnected = false;

    /**
     * 시그널링 서버에 연결 (Stub)
     */
    connect(roomId: string, callbacks: SignalingCallbacks): void {
        console.log(`[Signaling Stub] Connecting to room: ${roomId}`);
        this.callbacks = callbacks;

        // Simulate connection delay
        setTimeout(() => {
            this.isConnected = true;
            this.callbacks?.onConnected();
            console.log('[Signaling Stub] Connected');
        }, 300);
    }

    /**
     * 시그널링 서버 연결 해제 (Stub)
     */
    disconnect(): void {
        console.log('[Signaling Stub] Disconnecting');
        this.isConnected = false;
        this.callbacks?.onDisconnected();
        this.callbacks = null;
    }

    /**
     * 시그널링 메시지 전송 (Stub)
     */
    send(message: SignalingMessage): void {
        if (!this.isConnected) {
            console.warn('[Signaling Stub] Not connected, cannot send message');
            return;
        }
        console.log('[Signaling Stub] Sending message:', message.type);
        // TODO: 실제 WebSocket 전송 구현
    }

    /**
     * 연결 상태 확인
     */
    getConnectionStatus(): boolean {
        return this.isConnected;
    }
}

// Singleton instance
export const signalingService = new SignalingService();
