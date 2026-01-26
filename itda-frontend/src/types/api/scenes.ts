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
