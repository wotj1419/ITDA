export type CollabStatus = 'disconnected' | 'connecting' | 'connected' | 'error';

export interface CollabParticipant {
  odps: string;
  name: string;
  avatarUrl?: string;
  color?: string;
  isHost?: boolean;
  isActive?: boolean;
  isMuted?: boolean;
  isVideoOff?: boolean;
  isScreenSharing?: boolean;
  currentLocation?: string;
  sceneId?: number | null;
  nodeId?: number | null;
  cursor?: {
    x: number;
    y: number;
    targetNode?: string;
  };
  audioEnabled?: boolean;
  videoEnabled?: boolean;
}

export interface CollabMessage {
  messageId: string;
  senderId: string;
  senderName: string;
  content: string;
  timestamp: number;
  type?: 'chat' | 'system' | 'action';
}
