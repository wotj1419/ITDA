/**
 * WebRTC PeerConnection Service (Stub)
 * 
 * 백엔드 완료 후 실제 PeerConnection 구현 예정
 * 현재는 UI 테스트를 위한 Mock 구현
 */

export interface PeerConnectionConfig {
    iceServers?: RTCIceServer[];
}

export interface MediaStreamConfig {
    video: boolean;
    audio: boolean;
}

export interface PeerConnectionCallbacks {
    onTrack: (stream: MediaStream, peerId: string) => void;
    onIceCandidate: (candidate: RTCIceCandidate) => void;
    onConnectionStateChange: (state: RTCPeerConnectionState, peerId: string) => void;
}

class PeerConnectionService {
    private localStream: MediaStream | null = null;
    private screenStream: MediaStream | null = null;
    private peers: Map<string, RTCPeerConnection> = new Map();

    /**
     * 로컬 미디어 스트림 가져오기 (Stub)
     */
    async getLocalStream(config: MediaStreamConfig): Promise<MediaStream | null> {
        console.log('[PeerConnection Stub] Getting local stream:', config);

        // TODO: 실제 getUserMedia 구현
        // try {
        //   this.localStream = await navigator.mediaDevices.getUserMedia({
        //     video: config.video,
        //     audio: config.audio,
        //   });
        //   return this.localStream;
        // } catch (error) {
        //   console.error('Failed to get local stream:', error);
        //   return null;
        // }

        return null;
    }

    /**
     * 화면 공유 시작 (Stub)
     */
    async startScreenShare(): Promise<MediaStream | null> {
        console.log('[PeerConnection Stub] Starting screen share');

        // TODO: 실제 getDisplayMedia 구현
        // try {
        //   this.screenStream = await navigator.mediaDevices.getDisplayMedia({
        //     video: true,
        //   });
        //   return this.screenStream;
        // } catch (error) {
        //   console.error('Failed to start screen share:', error);
        //   return null;
        // }

        return null;
    }

    /**
     * 화면 공유 중지 (Stub)
     */
    stopScreenShare(): void {
        console.log('[PeerConnection Stub] Stopping screen share');
        this.screenStream?.getTracks().forEach(track => track.stop());
        this.screenStream = null;
    }

    /**
     * Peer 연결 생성 (Stub)
     */
    createPeerConnection(
        peerId: string,
        _config: PeerConnectionConfig,
        _callbacks: PeerConnectionCallbacks
    ): RTCPeerConnection | null {
        console.log('[PeerConnection Stub] Creating peer connection for:', peerId);

        // TODO: 실제 RTCPeerConnection 구현
        // const pc = new RTCPeerConnection(config);
        // this.peers.set(peerId, pc);
        // return pc;

        return null;
    }

    /**
     * Offer 생성 (Stub)
     */
    async createOffer(_peerId: string): Promise<RTCSessionDescriptionInit | null> {
        console.log('[PeerConnection Stub] Creating offer');

        // TODO: 실제 offer 생성 구현
        return null;
    }

    /**
     * Answer 생성 (Stub)
     */
    async createAnswer(_peerId: string): Promise<RTCSessionDescriptionInit | null> {
        console.log('[PeerConnection Stub] Creating answer');

        // TODO: 실제 answer 생성 구현
        return null;
    }

    /**
     * 모든 연결 종료 (Stub)
     */
    closeAll(): void {
        console.log('[PeerConnection Stub] Closing all connections');
        this.peers.forEach(pc => pc.close());
        this.peers.clear();
        this.localStream?.getTracks().forEach(track => track.stop());
        this.localStream = null;
        this.stopScreenShare();
    }

    /**
     * 마이크 음소거 토글 (Stub)
     */
    toggleMute(muted: boolean): void {
        console.log('[PeerConnection Stub] Toggle mute:', muted);
        this.localStream?.getAudioTracks().forEach(track => {
            track.enabled = !muted;
        });
    }

    /**
     * 비디오 토글 (Stub)
     */
    toggleVideo(videoOff: boolean): void {
        console.log('[PeerConnection Stub] Toggle video:', videoOff);
        this.localStream?.getVideoTracks().forEach(track => {
            track.enabled = !videoOff;
        });
    }
}

// Singleton instance
export const peerConnectionService = new PeerConnectionService();
