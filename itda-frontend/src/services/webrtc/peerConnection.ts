/**
 * WebRTC PeerConnection Service (Mesh, audio-first)
 */

export interface PeerConnectionConfig {
  iceServers?: RTCIceServer[]
}

export interface MediaStreamConfig {
  video: boolean
  audio: boolean
}

export interface PeerConnectionCallbacks {
  onTrack: (stream: MediaStream, peerId: string) => void
  onIceCandidate: (candidate: RTCIceCandidate, peerId: string) => void
  onConnectionStateChange: (state: RTCPeerConnectionState, peerId: string) => void
}

class PeerConnectionService {
  private localStream: MediaStream | null = null
  private screenStream: MediaStream | null = null
  private peers: Map<string, RTCPeerConnection> = new Map()

  async getLocalStream(config: MediaStreamConfig): Promise<MediaStream | null> {
    if (this.localStream) return this.localStream

    try {
      this.localStream = await navigator.mediaDevices.getUserMedia({
        video: config.video,
        audio: config.audio,
      })
      return this.localStream
    } catch (error) {
      console.error('Failed to get local stream:', error)
      return null
    }
  }

  getLocalStreamSync(): MediaStream | null {
    return this.localStream
  }

  async startScreenShare(): Promise<MediaStream | null> {
    try {
      // @ts-expect-error getDisplayMedia exists in modern browsers
      this.screenStream = await navigator.mediaDevices.getDisplayMedia({
        video: true,
      })
      return this.screenStream
    } catch (error) {
      console.error('Failed to start screen share:', error)
      return null
    }
  }

  stopScreenShare(): void {
    this.screenStream?.getTracks().forEach((track) => track.stop())
    this.screenStream = null
  }

  createPeerConnection(peerId: string, config: PeerConnectionConfig, callbacks: PeerConnectionCallbacks): RTCPeerConnection {
    const existing = this.peers.get(peerId)
    if (existing) return existing

    const pc = new RTCPeerConnection({
      iceServers: config.iceServers ?? [{ urls: 'stun:stun.l.google.com:19302' }],
    })

    const local = this.localStream
    if (local) {
      local.getTracks().forEach((track) => pc.addTrack(track, local))
    }

    pc.onicecandidate = (event) => {
      if (event.candidate) callbacks.onIceCandidate(event.candidate, peerId)
    }

    pc.ontrack = (event) => {
      const [stream] = event.streams
      if (stream) callbacks.onTrack(stream, peerId)
    }

    pc.onconnectionstatechange = () => {
      callbacks.onConnectionStateChange(pc.connectionState, peerId)
    }

    this.peers.set(peerId, pc)
    return pc
  }

  getPeer(peerId: string): RTCPeerConnection | undefined {
    return this.peers.get(peerId)
  }

  async createOffer(peerId: string): Promise<RTCSessionDescriptionInit> {
    const pc = this.peers.get(peerId)
    if (!pc) throw new Error(`PeerConnection not found for ${peerId}`)
    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)
    return offer
  }

  async handleOffer(peerId: string, offer: RTCSessionDescriptionInit): Promise<RTCSessionDescriptionInit> {
    const pc = this.peers.get(peerId)
    if (!pc) throw new Error(`PeerConnection not found for ${peerId}`)
    await pc.setRemoteDescription(new RTCSessionDescription(offer))
    const answer = await pc.createAnswer()
    await pc.setLocalDescription(answer)
    return answer
  }

  async handleAnswer(peerId: string, answer: RTCSessionDescriptionInit): Promise<void> {
    const pc = this.peers.get(peerId)
    if (!pc) throw new Error(`PeerConnection not found for ${peerId}`)
    await pc.setRemoteDescription(new RTCSessionDescription(answer))
  }

  async addIceCandidate(peerId: string, candidate: RTCIceCandidateInit): Promise<void> {
    const pc = this.peers.get(peerId)
    if (!pc) return
    try {
      await pc.addIceCandidate(new RTCIceCandidate(candidate))
    } catch (e) {
      console.warn('addIceCandidate failed', e)
    }
  }

  closePeer(peerId: string): void {
    const pc = this.peers.get(peerId)
    if (!pc) return
    try {
      pc.close()
    } finally {
      this.peers.delete(peerId)
    }
  }

  closeAll(): void {
    this.peers.forEach((pc) => pc.close())
    this.peers.clear()
    this.localStream?.getTracks().forEach((track) => track.stop())
    this.localStream = null
    this.stopScreenShare()
  }

  toggleMute(muted: boolean): void {
    this.localStream?.getAudioTracks().forEach((track) => {
      track.enabled = !muted
    })
  }

  toggleVideo(videoOff: boolean): void {
    this.localStream?.getVideoTracks().forEach((track) => {
      track.enabled = !videoOff
    })
  }
}

export const peerConnectionService = new PeerConnectionService()
