import type { Scene as ApiScene, SceneStatus as ApiSceneStatus } from '../api/scenes';

const sceneStatusSet = new Set<ApiSceneStatus>(['DRAFT', 'IN_PROGRESS', 'COMPLETED']);

export function normalizeSceneStatus(status: string | null | undefined): ApiSceneStatus {
  return sceneStatusSet.has(status as ApiSceneStatus) ? (status as ApiSceneStatus) : 'DRAFT';
}

export function normalizeScene(scene: ApiScene): ApiScene {
  return { ...scene, status: normalizeSceneStatus(scene.status) };
}
