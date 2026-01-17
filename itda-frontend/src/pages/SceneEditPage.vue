<script setup lang="ts">
/**
 * SceneEditPage - 노드 기반 씬 에디터 페이지
 * Master → Grid → Shot → Video 흐름의 노드 편집
 */
import { computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useProjectStore } from '../stores/project';
import { useSceneStore } from '../stores/scene';
import { useNodeStore } from '../stores/node';
import { useUIStore } from '../stores/ui';
import { useCollabStore } from '../stores/collab';
import { useZoom } from '../composables/useZoom';

import EditorLayout from '../layouts/EditorLayout.vue';
import EditorHeader from '../components/editor/EditorHeader.vue';
import NodeCanvas from '../components/editor/NodeCanvas.vue';
import PropertyPanel from '../components/editor/PropertyPanel.vue';
import MiniTimeline from '../components/editor/MiniTimeline.vue';
import ZoomControls from '../components/editor/ZoomControls.vue';

// =============================================================================
// Composables & Stores
// =============================================================================

const route = useRoute();
const projectStore = useProjectStore();
const sceneStore = useSceneStore();
const nodeStore = useNodeStore();
const uiStore = useUIStore();
const collabStore = useCollabStore();

const {
  zoomPercentage,
  zoomScale,
  zoomIn,
  zoomOut,
  zoomFit,
} = useZoom();

// =============================================================================
// Route Parameters
// =============================================================================

const projectId = computed(() => Number(route.params.projectId));
const sceneId = computed(() => Number(route.params.sceneId));

// =============================================================================
// Data
// =============================================================================

const project = computed(() => projectStore.currentProject);

const currentScene = computed(() =>
  sceneStore.scenes.find((s) => s.sceneId === sceneId.value)
);

const sceneTitle = computed(() => {
  const scene = currentScene.value;
  if (!scene) return 'Scene';
  return `Scene ${scene.order}: ${scene.title}`;
});

// =============================================================================
// Lifecycle
// =============================================================================

onMounted(async () => {
  if (projectId.value && sceneId.value) {
    await Promise.all([
      projectStore.loadProject(projectId.value),
      sceneStore.loadScenes(projectId.value),
      nodeStore.loadNodes(projectId.value, sceneId.value),
    ]);
    
    // 협업 방 입장
    collabStore.joinRoom(projectId.value);
    collabStore.updateLocation(sceneTitle.value);
  }
});

onUnmounted(() => {
  // 페이지 이탈 시 협업 방 퇴장
  collabStore.leaveRoom();
});

// Route 변경 시 노드 다시 로드
watch([projectId, sceneId], async ([newProjectId, newSceneId]) => {
  if (newProjectId && newSceneId) {
    await nodeStore.loadNodes(newProjectId, newSceneId);
  }
});

// =============================================================================
// Event Handlers
// =============================================================================

/**
 * 노드 선택
 */
function handleSelectNode(nodeId: number): void {
  nodeStore.selectNode(nodeId);
}

/**
 * 노드 설정 업데이트
 */
function handleUpdateSettings(settings: Record<string, unknown>): void {
  if (nodeStore.selectedNodeId) {
    nodeStore.updateSettings(nodeStore.selectedNodeId, settings);
  }
}

/**
 * 비디오 확정
 */
async function handleConfirmVideo(nodeId: number): Promise<void> {
  const success = await nodeStore.confirmVideo(nodeId);
  if (success) {
    uiStore.showToast({
      type: 'success',
      title: '확정 완료',
      message: '영상이 타임라인에 추가되었습니다.',
    });
  }
}

/**
 * 노드 삭제 (현재는 토스트만 표시)
 */
function handleDeleteNode(): void {
  uiStore.showToast({
    type: 'info',
    title: '삭제',
    message: '노드 삭제 기능은 준비 중입니다.',
  });
}
</script>

<template>
  <EditorLayout
    :project-title="project?.title || 'Project'"
    :scene-title="sceneTitle"
    scene-badge="Scene Editor"
  >
    <!-- Header Slot -->
    <template #header>
      <EditorHeader
        :project-title="project?.title || 'Project'"
        :scene-title="sceneTitle"
        :zoom-level="zoomPercentage"
        :project-id="projectId"
        :scene-id="sceneId"
      />
    </template>

    <!-- Canvas Slot -->
    <template #canvas>
      <NodeCanvas
        :nodes="nodeStore.nodes"
        :selected-node-id="nodeStore.selectedNodeId"
        :zoom-scale="zoomScale"
        @select-node="handleSelectNode"
        @confirm-video="handleConfirmVideo"
      />
    </template>

    <!-- Properties Slot -->
    <template #properties>
      <PropertyPanel
        :node="nodeStore.selectedNode"
        @update-settings="handleUpdateSettings"
        @delete-node="handleDeleteNode"
      />
    </template>

    <!-- Bottom Slot -->
    <template #bottom>
      <MiniTimeline
        :clips="nodeStore.timelineClips"
        :total-duration="nodeStore.totalDuration"
        :project-id="projectId"
        :scene-id="sceneId"
      />
    </template>
  </EditorLayout>

  <!-- Zoom Controls (Fixed Position) -->
  <ZoomControls
    @zoom-in="zoomIn"
    @zoom-out="zoomOut"
    @zoom-fit="zoomFit"
  />
</template>
