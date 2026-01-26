export type NodeType = 'SCENE_HEADER' | 'MASTER' | 'GRID' | 'SHOT' | 'VIDEO';
export type NodeStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';

export interface NodePosition {
  x: number;
  y: number;
}

export type CameraMotion =
  | 'ZOOM_IN'
  | 'ZOOM_OUT'
  | 'PAN_LEFT'
  | 'PAN_RIGHT'
  | 'TILT_UP'
  | 'TILT_DOWN'
  | 'STATIC';

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
