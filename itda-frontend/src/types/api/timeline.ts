export interface TimelineItem {
  videoNodeId?: number;
  sceneVideoId?: number;
  sceneId: number;
  sceneTitle?: string;
  thumbnailUrl?: string;
  url?: string;
  duration: number;
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
