<script setup lang="ts">
/**
 * SceneEditPage - Vue Flow 노드 기반 씬 에디터 페이지
 * Master → Grid → Shot → Video 흐름의 노드 편집
 */
import { computed, onMounted, onUnmounted, watch, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useProjectStore } from '../stores/project';
import { useSceneStore } from '../stores/scene';
import { useSceneNodeStore } from '../stores/sceneNode';
import { useUIStore } from '../stores/ui';
import { useCollabStore } from '../stores/collab';

import EditorLayout from '../layouts/EditorLayout.vue';
import EditorHeader from '../components/editor/EditorHeader.vue';
import NodeCanvas from '../components/scene-editor/NodeCanvas.vue';
import NodePanelContainer from '../components/scene-editor/panels/NodePanelContainer.vue';
import MiniTimeline from '../components/editor/MiniTimeline.vue';

// =============================================================================
// Composables & Stores
// =============================================================================

const route = useRoute();
const projectStore = useProjectStore();
const sceneStore = useSceneStore();
const nodeStore = useSceneNodeStore();
const uiStore = useUIStore();
const collabStore = useCollabStore();

const nodeCanvasRef = ref<InstanceType<typeof NodeCanvas> | null>(null);

// =============================================================================
// Route Parameters
// =============================================================================

const projectId = computed(() => Number(route.params.projectId));
const sceneId = computed(() => route.params.sceneId as string);

// =============================================================================
// Data
// =============================================================================

const project = computed(() => projectStore.currentProject);

const currentScene = computed(() =>
  sceneStore.scenes.find((s) => s.sceneId === Number(sceneId.value))
);

const sceneTitle = computed(() => {
  const scene = currentScene.value;
  if (!scene) return 'Scene';
  return `Scene ${scene.order}: ${scene.title}`;
});

// Timeline clips from confirmed videos
const timelineClips = computed(() => {
  return nodeStore.confirmedVideos.map((n, index) => ({
    clipId: n.id,
    nodeId: parseInt(n.id.replace(/\D/g, '')) || index + 1,
    thumbnailUrl: (n.data as any)?.thumbnailUrl || '',
    duration: (n.data as any)?.duration || 5,
    order: index + 1,
    label: `Video v${n.data?.version || 1}`,
  }));
});

const totalDuration = computed(() => {
  return timelineClips.value.reduce((sum, clip) => sum + clip.duration, 0);
});

// =============================================================================
// Lifecycle
// =============================================================================

onMounted(async () => {
  if (projectId.value && sceneId.value) {
    await Promise.all([
      projectStore.loadProject(projectId.value),
      sceneStore.loadScenes(projectId.value),
    ]);
    
    // Vue Flow 노드 로드
    await nodeStore.loadSceneNodes(sceneId.value);
    
    // 협업 방 입장
    collabStore.joinRoom(projectId.value);
    collabStore.updateLocation(sceneTitle.value);
  }
});

onUnmounted(() => {
  // 페이지 이탈 시 협업 방 퇴장
  collabStore.leaveRoom();
  nodeStore.clearNodes();
});

// Route 변경 시 노드 다시 로드
watch([projectId, sceneId], async ([, newSceneId]) => {
  if (newSceneId) {
    await nodeStore.loadSceneNodes(newSceneId as string);
  }
});

// =============================================================================
// Event Handlers
// =============================================================================

/**
 * 노드 선택
 */
function handleNodeSelect(nodeId: string | null): void {
  nodeStore.selectNode(nodeId);
  if (nodeId) {
    uiStore.showToast({
      type: 'info',
      title: '노드 선택',
      message: `노드 ${nodeId} 선택됨`,
      duration: 2000,
    });
  }
}

/**
 * 자동 레이아웃 적용
 */
function handleAutoLayout(): void {
  nodeCanvasRef.value?.applyLayout();
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
        :zoom-level="'100%'"
        :project-id="projectId"
        :scene-id="Number(sceneId)"
      />
    </template>

    <!-- Canvas Slot -->
    <template #canvas>
      <NodeCanvas
        ref="nodeCanvasRef"
        :scene-id="sceneId"
        @node-select="handleNodeSelect"
      />
    </template>

    <!-- Properties Slot -->
    <template #properties>
      <NodePanelContainer />
    </template>

    <!-- Bottom Slot -->
    <template #bottom>
      <MiniTimeline
        :clips="timelineClips"
        :total-duration="totalDuration"
        :project-id="projectId"
        :scene-id="Number(sceneId)"
      />
    </template>
  </EditorLayout>

  <!-- Auto Layout Button (Fixed Position) -->
  <button
    class="auto-layout-btn"
    @click="handleAutoLayout"
    title="자동 정렬"
  >
    🔄 정렬
  </button>
</template>

<style scoped>
.auto-layout-btn {
  position: fixed;
  bottom: 100px;
  right: 24px;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  background: white;
  color: var(--gray-700);
  border: 1px solid var(--rose-200);
  border-radius: var(--radius-full, 9999px);
  box-shadow: var(--shadow-md);
  cursor: pointer;
  z-index: 100;
  transition: all 0.2s ease;
}

.auto-layout-btn:hover {
  background: var(--rose-50);
  border-color: var(--rose-500);
  color: var(--rose-600);
}
</style>
