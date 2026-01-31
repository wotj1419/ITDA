/**
 * WebRTC PeerConnection Service (Mesh P2P)
 *
 * Handles multiple RTCPeerConnections for a mesh network.
 * - Manages local media stream.
 * - Creates and manages peer connections.
 * - Handles ICE candidates and SDP negotiation.
 */

export interface PeerConnectionConfig {
    iceServers?: RTCIceServer[];
}

export interface MediaStreamConfig {
    video: boolean;
    audio: boolean;
    audioDeviceId?: string | null;
}

export interface PeerConnectionCallbacks {
    onTrack: (stream: MediaStream, peerId: string) => void;
    onIceCandidate: (candidate: RTCIceCandidate, peerId: string) => void;
    onConnectionStateChange: (state: RTCPeerConnectionState, peerId: string) => void;
}

// Default Google STUN servers (free and reliable)
const DEFAULT_ICE_SERVERS: RTCIceServer[] = [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
];

class PeerConnectionService {
    private localStream: MediaStream | null = null;
    private screenStream: MediaStream | null = null;
    private peers: Map<string, RTCPeerConnection> = new Map();
    private callbacks: PeerConnectionCallbacks | null = null;

    /**
     * Initialize callbacks
     */
    setCallbacks(callbacks: PeerConnectionCallbacks) {
        this.callbacks = callbacks;
    }

    /**
     * Get Local Media Stream
     */
    async getLocalStream(config: MediaStreamConfig): Promise<MediaStream | null> {
        console.log('[PeerConnection] Getting local stream:', config);
        try {
            // Stop existing tracks if any
            if (this.localStream) {
                this.localStream.getTracks().forEach(track => track.stop());
            }

            const audioConstraint = config.audio
                ? (config.audioDeviceId
                    ? { deviceId: { exact: config.audioDeviceId } }
                    : true)
                : false;
            this.localStream = await navigator.mediaDevices.getUserMedia({
                video: config.video,
                audio: audioConstraint,
            });
            return this.localStream;
        } catch (error) {
            console.error('[PeerConnection] Failed to get local stream:', error);
            return null;
        }
    }

    /**
     * Switch microphone (audio input device)
     */
    async switchMicrophone(deviceId: string | null | undefined): Promise<MediaStream | null> {
        if (!deviceId) return this.localStream;
        try {
            const stream = await navigator.mediaDevices.getUserMedia({
                audio: { deviceId: { exact: deviceId } },
                video: false,
            });
            const newTrack = stream.getAudioTracks()[0];
            if (!newTrack) return this.localStream;

            if (!this.localStream) {
                this.localStream = new MediaStream();
            }

            // Replace audio track in local stream
            this.localStream.getAudioTracks().forEach((track) => {
                track.stop();
                this.localStream?.removeTrack(track);
            });
            this.localStream.addTrack(newTrack);

            // Replace audio track in peer connections
            this.peers.forEach((pc) => {
                const sender = pc.getSenders().find(s => s.track?.kind === 'audio');
                if (sender) {
                    sender.replaceTrack(newTrack);
                } else {
                    pc.addTrack(newTrack, this.localStream!);
                }
            });

            return this.localStream;
        } catch (error) {
            console.error('[PeerConnection] Failed to switch microphone:', error);
            return this.localStream;
        }
    }

    /**
     * Start Screen Share
     */
    async startScreenShare(): Promise<MediaStream | null> {
        console.log('[PeerConnection] Starting screen share');
        try {
            if (this.screenStream) {
                this.screenStream.getTracks().forEach(track => track.stop());
            }

            this.screenStream = await navigator.mediaDevices.getDisplayMedia({
                video: true,
                audio: false
            });

            // Replace video track in all peer connections
            const videoTrack = this.screenStream.getVideoTracks()[0];

            if (videoTrack) {
                // Handle stream stop (user clicks "Stop sharing" in browser UI)
                videoTrack.onended = () => {
                    this.stopScreenShare();
                };

                this.peers.forEach((pc) => {
                    const senders = pc.getSenders();
                    const videoSender = senders.find(s => s.track?.kind === 'video');
                    if (videoSender) {
                        videoSender.replaceTrack(videoTrack);
                    }
                });
            }

            return this.screenStream;
        } catch (error) {
            console.error('[PeerConnection] Failed to start screen share:', error);
            return null;
        }
    }

    /**
     * Stop Screen Share & Revert to Camera
     */
    stopScreenShare(): void {
        console.log('[PeerConnection] Stopping screen share');
        if (this.screenStream) {
            this.screenStream.getTracks().forEach(track => track.stop());
            this.screenStream = null;

            // Revert to local camera if available
            if (this.localStream) {
                const videoTrack = this.localStream.getVideoTracks()[0];
                if (videoTrack) {
                    this.peers.forEach((pc) => {
                        const senders = pc.getSenders();
                        const videoSender = senders.find(s => s.track?.kind === 'video');
                        if (videoSender) {
                            videoSender.replaceTrack(videoTrack);
                        }
                    });
                }
            }
        }
    }

    /**
     * Create Peer Connection
     */
    createPeerConnection(peerId: string): RTCPeerConnection {
        if (this.peers.has(peerId)) {
            console.warn(`[PeerConnection] Peer connection for ${peerId} already exists`);
            return this.peers.get(peerId)!;
        }

        console.log(`[PeerConnection] Creating peer connection for: ${peerId}`);

        const pc = new RTCPeerConnection({
            iceServers: DEFAULT_ICE_SERVERS
        });

        this.peers.set(peerId, pc);

        // Add local tracks
        if (this.localStream) {
            this.localStream.getTracks().forEach(track => {
                pc.addTrack(track, this.localStream!);
            });
        }

        // ICE Candidate handling
        pc.onicecandidate = (event) => {
            if (event.candidate && this.callbacks) {
                this.callbacks.onIceCandidate(event.candidate, peerId);
            }
        };

        // Connection state changes
        pc.onconnectionstatechange = () => {
            console.log(`[PeerConnection] State change for ${peerId}: ${pc.connectionState}`);
            if (this.callbacks) {
                this.callbacks.onConnectionStateChange(pc.connectionState, peerId);
            }
            if (pc.connectionState === 'disconnected' || pc.connectionState === 'failed' || pc.connectionState === 'closed') {
                this.removePeer(peerId);
            }
        };

        // Track handling (Remote stream)
        pc.ontrack = (event) => {
            console.log(`[PeerConnection] Received track from ${peerId}:`, event.streams[0]);
            if (this.callbacks && event.streams[0]) {
                this.callbacks.onTrack(event.streams[0], peerId);
            }
        };

        return pc;
    }

    /**
     * Create Offer
     */
    async createOffer(peerId: string): Promise<RTCSessionDescriptionInit | null> {
        const pc = this.peers.get(peerId);
        if (!pc) return null;

        try {
            const offer = await pc.createOffer();
            await pc.setLocalDescription(offer);
            return offer;
        } catch (error) {
            console.error(`[PeerConnection] Failed to create offer for ${peerId}:`, error);
            return null;
        }
    }

    /**
     * Handle Answer
     */
    async setRemoteDescription(peerId: string, sdp: RTCSessionDescriptionInit): Promise<void> {
        const pc = this.peers.get(peerId);
        if (!pc) {
            console.warn(`[PeerConnection] Peer ${peerId} not found for setting remote description`);
            return;
        }
        try {
            await pc.setRemoteDescription(new RTCSessionDescription(sdp));
        } catch (error) {
            console.error(`[PeerConnection] Failed to set remote description for ${peerId}:`, error);
        }
    }

    /**
     * Create Answer
     */
    async createAnswer(peerId: string): Promise<RTCSessionDescriptionInit | null> {
        const pc = this.peers.get(peerId);
        if (!pc) return null;

        try {
            const answer = await pc.createAnswer();
            await pc.setLocalDescription(answer);
            return answer;
        } catch (error) {
            console.error(`[PeerConnection] Failed to create answer for ${peerId}:`, error);
            return null;
        }
    }

    /**
     * Add ICE Candidate
     */
    async addIceCandidate(peerId: string, candidate: RTCIceCandidateInit): Promise<void> {
        const pc = this.peers.get(peerId);
        if (!pc) {
            console.warn(`[PeerConnection] Peer ${peerId} not found for adding ICE candidate`);
            return;
        }
        try {
            await pc.addIceCandidate(new RTCIceCandidate(candidate));
        } catch (error) {
            console.error(`[PeerConnection] Failed to add ICE candidate for ${peerId}:`, error);
        }
    }

    /**
     * Remove Peer
     */
    removePeer(peerId: string): void {
        const pc = this.peers.get(peerId);
        if (pc) {
            console.log(`[PeerConnection] Removing peer: ${peerId}`);
            pc.onicecandidate = null;
            pc.ontrack = null;
            pc.close();
            this.peers.delete(peerId);
        }
    }

    /**
     * Close All
     */
    closeAll(): void {
        console.log('[PeerConnection] Closing all connections');
        this.peers.forEach(pc => pc.close());
        this.peers.clear();

        if (this.localStream) {
            this.localStream.getTracks().forEach(track => track.stop());
            this.localStream = null;
        }
        if (this.screenStream) {
            this.screenStream.getTracks().forEach(track => track.stop());
            this.screenStream = null;
        }
    }

    /**
     * Toggle Mute
     */
    toggleMute(muted: boolean): void {
        if (this.localStream) {
            this.localStream.getAudioTracks().forEach(track => {
                track.enabled = !muted;
            });
        }
    }

    /**
     * Toggle Video
     */
    toggleVideo(videoOff: boolean): void {
        if (this.localStream) {
            this.localStream.getVideoTracks().forEach(track => {
                track.enabled = !videoOff;
            });
        }
    }
}

// Singleton instance
export const peerConnectionService = new PeerConnectionService();
