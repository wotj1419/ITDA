// ================================
// API Response Types
// ================================
export interface ApiResponse<T = unknown> {
  code: string;
  message?: string;
  data?: T;
  details?: Record<string, unknown>;
}

// ================================
// User Types
// ================================
export interface User {
  userId: number;
  email: string;
  name: string;
  profileImage?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
}

// ================================
// Project Types
// ================================
export type ProjectRole = 'OWNER' | 'EDITOR' | 'VIEWER';

export interface Project {
  projectId: number;
  title: string;
  description?: string;
  genre?: string;
  thumbnailUrl?: string;
  role: ProjectRole;
  memberCount: number;
  sceneCount: number;
  updatedAt: string;
  createdAt?: string;
  isDeleted?: boolean;
  deletedAt?: string;
}

export interface ProjectListItem extends Project {
  // Same as Project for now
}

export interface ProjectDetail extends Project {
  myRole: ProjectRole;
  ownerId: number;
  members: ProjectMember[];
}

export interface ProjectMember {
  userId: number;
  email: string;
  name: string;
  role: ProjectRole;
  profileImage?: string;
}

export interface CreateProjectRequest {
  title: string;
  description?: string;
  genre?: string;
}

// ================================
// Scene Types
// ================================
export type SceneStatus = 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED';

export interface Scene {
  sceneId: number;
  title: string;
  description?: string;
  order: number;
  status: SceneStatus;
  thumbnailUrl?: string;
}

export interface CreateSceneRequest {
  title: string;
  description?: string;
}

// ================================
// Node Types
// ================================
export type NodeType = 'SCENE_HEADER' | 'MASTER' | 'GRID' | 'SHOT' | 'VIDEO';
export type NodeStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';

export interface NodePosition {
  x: number;
  y: number;
}

export interface Node {
  nodeId: number;
  type: NodeType;
  parentNodeId?: number;
  prompt?: string;
  status: NodeStatus;
  contentUrl?: string;
  thumbnailUrl?: string;
  position: NodePosition;
  isActive?: boolean;
  isConfirmed?: boolean;
  title?: string;
  description?: string;
  settings?: NodeSettings;
}

// ================================
// Editor Types (Phase 4)
// ================================
export type CameraMotion =
  | 'ZOOM_IN'
  | 'ZOOM_OUT'
  | 'PAN_LEFT'
  | 'PAN_RIGHT'
  | 'TILT_UP'
  | 'TILT_DOWN'
  | 'STATIC';

export interface TimelineClip {
  clipId: string;
  nodeId: number | string;
  sceneId?: number;
  sourceNodeId?: string;
  thumbnailUrl: string;
  videoUrl?: string;
  duration: number;
  order: number;
  label?: string;
}

export interface GridCell {
  index: number;
  imageUrl?: string;
  selected: boolean;
}

export type GridLayout = '2x2' | '2x3' | '3x3';

export interface NodeSettings {
  style?: string;
  ratio?: string;
  cameraMotion?: CameraMotion;
  duration?: number;
  startShotNodeId?: number;
  endShotNodeId?: number;
  motionDescription?: string;
  provider?: string;
  gridLayout?: GridLayout;
  gridCells?: GridCell[];
}

export interface CreateNodeRequest {
  type: NodeType;
  parentNodeId?: number;
  prompt?: string;
  settings?: NodeSettings;
}

// ================================
// Scenario Types
// ================================
export interface Scenario {
  projectId: number;
  version: number;
  currentStep: 'PROMPT' | 'PLOT' | 'SCENES';
  prompt: ScenarioPrompt;
  plot: ScenarioPlot;
  scenes: ScenarioScene[];
  updatedAt: string;
}

export interface ScenarioPrompt {
  text: string;
  status: 'DRAFT' | 'APPROVED';
}

export interface ScenarioPlot {
  text: string;
  status: 'DRAFT' | 'APPROVED';
}

export interface ScenarioScene {
  scenarioSceneId: number;
  sceneId: number | null;
  order: number;
  title: string;
  description: string;
}

export interface GenerateScenarioPromptRequest {
  genre: string;
  mood: string;
  keywords?: string[];
  sceneCount: number;
  characterHints?: string;
  backgroundHints?: string;
  referenceStyle?: string;
}

// ================================
// Object/Character Types
// ================================
export type ObjectType = 'CHARACTER' | 'PROP' | 'ETC';

export interface ObjectSheet {
  objectId: number;
  name: string;
  type: ObjectType;
  description: string;
  style?: string;
  sheetImageUrl?: string;
}

export interface CreateObjectRequest {
  name: string;
  type: ObjectType;
  description: string;
  style?: string;
}

// ================================
// Timeline Types
// ================================
export interface TimelineItem {
  videoNodeId?: number;
  sceneVideoId?: number;
  sceneId: number;
  sceneTitle?: string;
  thumbnailUrl: string;
  duration: number;
  order: number;
}

export interface TimelineClip {
  clipId: string;
  nodeId: number | string;
  sceneId?: number;
  sourceNodeId?: string;
  thumbnailUrl: string;
  videoUrl?: string;
  duration: number;
  order: number;
  label?: string;
}

export interface SceneTimeline {
  items: TimelineItem[];
  totalDuration: number;
}

export interface ProjectTimeline {
  items: TimelineItem[];
  totalDuration: number;
}

// ================================
// Job Types (AI Generation)
// ================================
export type JobType = 'VIDEO_GENERATION' | 'IMAGE_GENERATION' | 'SCENE_MERGE' | 'PROJECT_MERGE';
export type JobStatus = 'pending' | 'running' | 'done' | 'failed';

export interface Job {
  jobId: string;
  type: JobType;
  status: JobStatus;
  progress?: number | null;
  target: {
    type: 'NODE' | 'SCENE_VIDEO' | 'PROJECT';
    id: number;
  };
  resultUrl?: string;
  error?: {
    code: string;
    message: string;
  };
}

// ================================
// UI Types
// ================================
export interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info' | 'progress';
  title: string;
  message?: string;
  meta?: string;
  duration?: number;
  position?: 'top-right' | 'bottom-right';
  autoClose?: boolean;
}

export interface ModalConfig {
  id: string;
  isOpen: boolean;
  data?: unknown;
}

// ================================
// Collaboration Types (WebRTC)
// ================================
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



export * from './scene';
