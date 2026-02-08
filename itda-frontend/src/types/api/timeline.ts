export interface TimelineItem {
  videoNodeId?: number;
  sceneVideoId?: number;
  sceneId: number;
  sceneTitle?: string;
  thumbnailUrl?: string;
  videoUrl?: string;
  url?: string;
  duration?: number | null;
  order: number;
}

export interface TimelineClip {
  clipId: string;
  nodeId: number | string;
  videoNodeId?: number;
  sceneVideoId?: number;
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

export interface SceneExportItem {
  sceneVideoId: number;
  sceneId: number;
  assetId?: number | null;
  previewUrl?: string | null;
  downloadUrl?: string | null;
  thumbnailUrl?: string | null;
  mergeSignature?: string | null;
  status: string;
  durationMs?: number | null;
  active: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface SceneExportListResponse {
  items: SceneExportItem[];
  page: number;
  size: number;
  totalCount: number;
  totalPages: number;
  activeItem?: SceneExportItem | null;
}
