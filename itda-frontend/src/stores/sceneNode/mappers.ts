import type { SceneNode } from './types';
import type {
  BaseNodeData,
  AnyNodeData,
  SceneHeaderNodeData,
  MasterImageNodeData,
  StoryboardGridNodeData,
  ShotNodeData,
  VideoNodeData,
} from '../../types/ui/sceneNodes';
import {
  NodeType,
  PromptStatus,
  JobStatus,
  NODE_HEIGHTS,
  NODE_WIDTHS,
} from '../../types/ui/sceneNodes';
import type { NodeSummary, ApiNodeType } from '../../services/api/nodes';
import { resolveApiUrl } from '../../services/api/urls';

export const API_TO_UI_NODE_TYPE: Record<ApiNodeType, NodeType> = {
  SCENE_HEADER: NodeType.SCENE_HEADER,
  MASTER: NodeType.MASTER_IMAGE,
  GRID: NodeType.STORYBOARD_GRID,
  SHOT: NodeType.SHOT,
  VIDEO: NodeType.VIDEO,
};

export const UI_TO_API_NODE_TYPE: Record<NodeType, ApiNodeType> = {
  [NodeType.SCENE_HEADER]: 'SCENE_HEADER',
  [NodeType.MASTER_IMAGE]: 'MASTER',
  [NodeType.STORYBOARD_GRID]: 'GRID',
  [NodeType.SHOT]: 'SHOT',
  [NodeType.VIDEO]: 'VIDEO',
};

export function createBaseNodeData(
  id: string,
  type: NodeType,
  parentNodeId: string | null = null,
  version: number = 1
): BaseNodeData {
  const now = new Date().toISOString();
  return {
    id,
    type,
    jobStatus: null,
    promptStatus: PromptStatus.DRAFT,
    createdAt: now,
    updatedAt: now,
    versionGroupId: id,
    version,
    parentNodeId,
    isCollapsed: false,
    childCount: 0,
  };
}

export function getDefaultNodeDimensions(type: NodeType): { width: number; height: number } {
  return {
    width: NODE_WIDTHS[type] ?? 200,
    height: NODE_HEIGHTS[type] ?? 150,
  };
}

export function toJobStatus(status?: string | null): JobStatus | null {
  if (!status) return null;
  const normalized = status.toUpperCase();
  switch (normalized) {
    case 'PENDING':
      return JobStatus.PENDING;
    case 'RUNNING':
      return JobStatus.RUNNING;
    case 'SUCCEEDED':
      return JobStatus.SUCCEEDED;
    case 'FAILED':
      return JobStatus.FAILED;
    default:
      return null;
  }
}

export function toFiniteNumber(value: string | number): number | null {
  const numeric = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numeric) ? numeric : null;
}

export function buildNodeSettings(data: AnyNodeData): Record<string, unknown> {
  switch (data.type) {
    case NodeType.MASTER_IMAGE:
      return {
        style: (data as MasterImageNodeData).style,
        timeOfDay: (data as MasterImageNodeData).timeOfDay,
        mood: (data as MasterImageNodeData).mood,
        objectIds: (data as MasterImageNodeData).objectIds,
      };
    case NodeType.STORYBOARD_GRID:
      return {
        layout: (data as StoryboardGridNodeData).layout,
        shotTypes: (data as StoryboardGridNodeData).shotTypes,
        compositionHint: (data as StoryboardGridNodeData).compositionHint,
      };
    case NodeType.SHOT:
      return {
        gridCellIndex: (data as ShotNodeData).gridCellIndex,
        shotTypes: (data as ShotNodeData).shotTypes,
        shotType: (data as ShotNodeData).shotType,
        expression: (data as ShotNodeData).expression,
        additionalDetail: (data as ShotNodeData).additionalDetail,
      };
    case NodeType.VIDEO:
      return {
        startShotNodeId: (data as VideoNodeData).startShotId,
        endShotNodeId: (data as VideoNodeData).endShotId,
        cameraMotion: (data as VideoNodeData).cameraMotion,
        motionDescription: (data as VideoNodeData).motionDescription,
        duration: (data as VideoNodeData).duration,
        timelineOrder: (data as VideoNodeData).timelineOrder,
      };
    default:
      return {};
  }
}

export function applyNodeResultUrl(node: SceneNode, url: string): void {
  if (!node.data) return;
  const resolvedUrl = resolveApiUrl(url);
  if (!resolvedUrl) return;
  if (node.data.type === NodeType.VIDEO) {
    const videoData = node.data as VideoNodeData;
    videoData.videoUrl = resolvedUrl;
    if (!videoData.thumbnailUrl) {
      videoData.thumbnailUrl = null;
    }
  } else if (node.data.type === NodeType.MASTER_IMAGE) {
    const masterData = node.data as MasterImageNodeData;
    masterData.imageUrl = resolvedUrl;
    masterData.thumbnailUrl = resolvedUrl;
  } else if (node.data.type === NodeType.STORYBOARD_GRID) {
    const gridData = node.data as StoryboardGridNodeData;
    gridData.imageUrl = resolvedUrl;
    gridData.thumbnailUrl = resolvedUrl;
  } else if (node.data.type === NodeType.SHOT) {
    const shotData = node.data as ShotNodeData;
    shotData.imageUrl = resolvedUrl;
    shotData.thumbnailUrl = resolvedUrl;
  }
}

export function createSceneNodeFromApi(
  node: NodeSummary,
  sceneId: string,
  sceneInfo?: { title: string; description: string; order: number }
): SceneNode {
  const uiType = API_TO_UI_NODE_TYPE[node.type] ?? NodeType.VIDEO;
  const id = String(node.nodeId);
  const fallbackHeaderId = String(-Number(sceneId));
  const resolvedParentId = node.parentNodeId
    ? String(node.parentNodeId)
    : uiType === NodeType.MASTER_IMAGE
      ? fallbackHeaderId
      : null;
  const base = createBaseNodeData(id, uiType, resolvedParentId);
  base.jobStatus = toJobStatus(node.status ?? null);
  base.parentNodeId = resolvedParentId;

  let data: AnyNodeData;
  const resolvedContentUrl = resolveApiUrl(node.contentUrl ?? null);
  switch (uiType) {
    case NodeType.SCENE_HEADER:
      data = {
        ...base,
        type: NodeType.SCENE_HEADER,
        sceneId,
        title: node.title || sceneInfo?.title || '새 씬',
        description: node.description || sceneInfo?.description || '',
        sceneOrder: sceneInfo?.order || 1,
      } as SceneHeaderNodeData;
      break;
    case NodeType.MASTER_IMAGE:
      data = {
        ...base,
        type: NodeType.MASTER_IMAGE,
        sceneId,
        isActive: !!node.isActive,
        imageUrl: resolvedContentUrl,
        thumbnailUrl: resolvedContentUrl,
        prompt: '',
        style: '',
        timeOfDay: '',
        mood: '',
        objectIds: [],
      } as MasterImageNodeData;
      break;
    case NodeType.STORYBOARD_GRID:
      data = {
        ...base,
        type: NodeType.STORYBOARD_GRID,
        imageUrl: resolvedContentUrl,
        thumbnailUrl: resolvedContentUrl,
        prompt: '',
        layout: '2x2',
        shotTypes: [],
        compositionHint: '',
      } as StoryboardGridNodeData;
      break;
    case NodeType.SHOT:
      data = {
        ...base,
        type: NodeType.SHOT,
        imageUrl: resolvedContentUrl,
        thumbnailUrl: resolvedContentUrl,
        prompt: '',
        gridCellIndex: 0,
        shotTypes: [],
        shotType: '',
        expression: '',
        additionalDetail: '',
      } as ShotNodeData;
      break;
    case NodeType.VIDEO:
    default:
      data = {
        ...base,
        type: NodeType.VIDEO,
        startShotId: base.parentNodeId || '',
        endShotId: null,
        videoUrl: resolvedContentUrl,
        thumbnailUrl: resolvedContentUrl,
        duration: 5,
        isConfirmed: !!node.isConfirmed,
        prompt: '',
        cameraMotion: 'staticCamera',
        motionDescription: '',
        timelineOrder: undefined,
      } as VideoNodeData;
      break;
  }

  const { width, height } = getDefaultNodeDimensions(uiType);
  return {
    id,
    type: uiType,
    position: {
      x: node.position?.x ?? 0,
      y: node.position?.y ?? 0,
    },
    width,
    height,
    data,
  };
}
