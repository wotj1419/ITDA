/**
 * Node Registry - 커스텀 노드 컴포넌트 레지스트리
 * @module components/scene-editor/nodes
 */
import type { Component } from 'vue';
import SceneHeaderNode from './SceneHeaderNode.vue';
import MasterImageNode from './MasterImageNode.vue';
import StoryboardGridNode from './StoryboardGridNode.vue';
import ShotNode from './ShotNode.vue';
import VideoNode from './VideoNode.vue';

/** 노드 타입별 컴포넌트 매핑 */
export const nodeTypes: Record<string, Component> = {
    sceneHeader: SceneHeaderNode,
    masterImage: MasterImageNode,
    storyboardGrid: StoryboardGridNode,
    shot: ShotNode,
    video: VideoNode,
};

export {
    SceneHeaderNode,
    MasterImageNode,
    StoryboardGridNode,
    ShotNode,
    VideoNode,
};
