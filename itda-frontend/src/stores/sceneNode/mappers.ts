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
import {
  DEFAULT_GRID_LAYOUT,
  DEFAULT_GRID_SHOT_TYPES,
  DEFAULT_MASTER_MOOD,
  DEFAULT_MASTER_STYLE,
  DEFAULT_MASTER_TIME_OF_DAY,
  DEFAULT_VIDEO_ASPECT_RATIO,
  DEFAULT_VIDEO_CAMERA_MOTION,
  DEFAULT_VIDEO_DURATION,
  normalizeAspectRatio,
} from '../../utils/nodeDefaults';
import {
  mapShotTypeLabelsToKeys,
  resolveCameraMotionKey,
  resolveExpressionKey,
  resolveMoodKey,
  resolveShotTypeKey,
  resolveStyleKey,
  resolveTimeOfDayKey,
} from '../../utils/nodeSettings';

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
    generationState: null,
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
      {
        const styleKey = resolveStyleKey((data as MasterImageNodeData).style);
        const timeOfDayKey = resolveTimeOfDayKey((data as MasterImageNodeData).timeOfDay);
        const moodKey =
          resolveMoodKey((data as MasterImageNodeData).mood) ?? 'NEUTRAL';
        return {
          styleKey,
          timeOfDayKey,
          moodKey,
          objectIds: (data as MasterImageNodeData).objectIds,
        };
      }
    case NodeType.STORYBOARD_GRID:
      {
        const gridData = data as StoryboardGridNodeData;
        const gridMode = gridData.gridMode ?? 'SHOT_VARIATIONS';
        if (gridMode === 'STORY_BEATS') {
          return {
            gridMode,
            layout: gridData.layout,
            beatsKo: gridData.beats ?? [],
            continuityRulesKo: gridData.continuityRules ?? '',
          };
        }
        return {
          gridMode,
          layout: gridData.layout,
          shotTypes: mapShotTypeLabelsToKeys(gridData.shotTypes),
          compositionHintKo: gridData.compositionHint,
        };
      }
    case NodeType.SHOT:
      {
        const shotData = data as ShotNodeData;
        const fallbackShotType =
          shotData.shotType?.split(',')[0]?.trim() ||
          shotData.shotTypes?.[0];
        const shotTypeKey = resolveShotTypeKey(fallbackShotType);
        const expressionKey = resolveExpressionKey(shotData.expression);
        return {
          gridCellIndex: shotData.gridCellIndex,
          shotType: shotTypeKey,
          expressionKey,
          detailKo: shotData.additionalDetail,
        };
      }
    case NodeType.VIDEO:
      {
        const videoData = data as VideoNodeData;
        return {
          startShotNodeId: Number(videoData.startShotId),
          endShotNodeId: videoData.endShotId ? Number(videoData.endShotId) : null,
          cameraMotionKey: resolveCameraMotionKey(videoData.cameraMotion),
          motionDescriptionKo: videoData.motionDescription,
          duration: videoData.duration,
          aspectRatio: normalizeAspectRatio(videoData.aspectRatio),
          timelineOrder: videoData.timelineOrder,
        };
      }
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
  sceneInfo?: { title: string; description: string; order: number },
  sceneHeaderId?: string | null
): SceneNode {
  const uiType = API_TO_UI_NODE_TYPE[node.type] ?? NodeType.VIDEO;
  const id = String(node.nodeId);
  const fallbackHeaderId = sceneHeaderId ?? String(-Number(sceneId));
  const resolvedParentId = node.parentNodeId
    ? String(node.parentNodeId)
    : uiType === NodeType.MASTER_IMAGE
      ? fallbackHeaderId
      : null;
  const base = createBaseNodeData(id, uiType, resolvedParentId);
  base.jobStatus = toJobStatus(node.status ?? null);
  base.parentNodeId = resolvedParentId;

  let data: AnyNodeData;
  const isSucceeded = String(node.status ?? '').toUpperCase() === 'SUCCEEDED';
  const resolvedContentUrl = isSucceeded ? resolveApiUrl(node.contentUrl ?? null) : null;
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
        style: DEFAULT_MASTER_STYLE,
        timeOfDay: DEFAULT_MASTER_TIME_OF_DAY,
        mood: DEFAULT_MASTER_MOOD,
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
        layout: DEFAULT_GRID_LAYOUT,
        shotTypes: [...DEFAULT_GRID_SHOT_TYPES],
        compositionHint: '',
        gridMode: 'SHOT_VARIATIONS',
        beats: [],
        continuityRules: '',
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
        duration: DEFAULT_VIDEO_DURATION,
        aspectRatio: DEFAULT_VIDEO_ASPECT_RATIO,
        isConfirmed: !!node.isConfirmed,
        prompt: '',
        cameraMotion: DEFAULT_VIDEO_CAMERA_MOTION,
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
