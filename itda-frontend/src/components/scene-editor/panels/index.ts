/**
 * Panel Registry - 사이드바 패널 컴포넌트 레지스트리
 * @module components/scene-editor/panels
 */
import type { Component } from 'vue';
import SceneHeaderPanel from './SceneHeaderPanel.vue';
import MasterImagePanel from './MasterImagePanel.vue';
import StoryboardGridPanel from './StoryboardGridPanel.vue';
import ShotPanel from './ShotPanel.vue';
import VideoPanel from './VideoPanel.vue';

/** 노드 타입별 패널 컴포넌트 매핑 */
export const panelRegistry: Record<string, Component> = {
    sceneHeader: SceneHeaderPanel,
    masterImage: MasterImagePanel,
    storyboardGrid: StoryboardGridPanel,
    shot: ShotPanel,
    video: VideoPanel,
};

export {
    SceneHeaderPanel,
    MasterImagePanel,
    StoryboardGridPanel,
    ShotPanel,
    VideoPanel,
};
