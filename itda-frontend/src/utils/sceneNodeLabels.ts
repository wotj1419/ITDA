import {
  NodeType,
  type AnyNodeData,
  type SceneHeaderNodeData,
  type VideoNodeData,
} from '../types/ui/sceneNodes';

export function getSceneNodeDisplayName(data: AnyNodeData): string {
  switch (data.type) {
    case NodeType.SCENE_HEADER: {
      const header = data as SceneHeaderNodeData;
      return `씬 ${header.sceneOrder}: ${header.title}`;
    }
    case NodeType.MASTER_IMAGE:
      return `마스터 이미지 ${data.version}`;
    case NodeType.STORYBOARD_GRID:
      return `그리드 ${data.version}`;
    case NodeType.SHOT:
      return `샷 ${data.version}`;
    case NodeType.VIDEO: {
      const video = data as VideoNodeData;
      return `영상 ${video.version}${video.endShotId ? ' (트랜지션)' : ''}`;
    }
    default:
      return '노드';
  }
}

