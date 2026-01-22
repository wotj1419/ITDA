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
import autolayoutIcon from '../assets/autolayout.svg';

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

/**
 * 오른쪽 속성 패널 표시 여부
 */
const isPanelOpen = computed(() => !!nodeStore.selectedNodeId);

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
  window.addEventListener('keydown', handleEditorKeydown);
  if (projectId.value && sceneId.value) {
    await Promise.all([
      projectStore.loadProject(projectId.value),
      sceneStore.loadScenes(projectId.value),
    ]);
    
    // Vue Flow 노드 로드 (씬 정보 함께 전달)
    const scene = currentScene.value;
    await nodeStore.loadSceneNodes(sceneId.value, scene ? {
      title: scene.title,
      description: scene.description || '',
      order: scene.order,
    } : undefined);
    
    // 협업 방 입장
    collabStore.joinRoom(projectId.value);
    collabStore.updateLocation(sceneTitle.value);
  }
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleEditorKeydown);
  // 페이지 이탈 시 협업 방 퇴장
  collabStore.leaveRoom();
  nodeStore.clearNodes();
});

// Route 변경 시 노드 다시 로드
watch([projectId, sceneId], async ([, newSceneId]) => {
  if (newSceneId) {
    const scene = sceneStore.scenes.find((s) => s.sceneId === Number(newSceneId));
    await nodeStore.loadSceneNodes(newSceneId as string, scene ? {
      title: scene.title,
      description: scene.description || '',
      order: scene.order,
    } : undefined);
  }
});

// =============================================================================
// Event Handlers
// =============================================================================

/**
 * 노드 선택
 */
function isEditableTarget(target: EventTarget | null): boolean {
  const element = target as HTMLElement | null;
  if (!element) return false;
  const tagName = element.tagName;
  return tagName === 'INPUT' || tagName === 'TEXTAREA' || element.isContentEditable;
}

function handleEditorKeydown(event: KeyboardEvent): void {
  if (isEditableTarget(event.target)) return;

  const key = event.key.toLowerCase();
  if (key === 'delete' || key === 'backspace') {
    const selectedId = nodeStore.selectedNodeId;
    if (selectedId) {
      event.preventDefault();
      nodeStore.deleteNode(selectedId);
    }
    return;
  }

  if ((event.ctrlKey || event.metaKey) && key === 'z') {
    event.preventDefault();
    nodeStore.undoLastMove();
  }
}

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


  <button 
    class="auto-layout-btn" 
    :class="{ 'panel-open': isPanelOpen }" 
    @click="handleAutoLayout"
  >
    <span class="icon-wrap">
      <img
        class="svgIcon icon-default"
        :src="autolayoutIcon"
        alt="Auto layout"
      />
      <img
        class="svgIcon icon-refresh"
        :src="autolayoutIcon"
        alt=""
        aria-hidden="true"
      />
    </span>
    레이아웃 정렬
  </button>
</template>

<style scoped>
.auto-layout-btn {
  --btn-size: 50px;
  --btn-half: 25px;
  --icon-size: 40px;
  --bottom: 80px;

  width: var(--btn-size);
  height: var(--btn-size);
  border-radius: 9999px;
  background: var(--rose-300, #FFD9E8);
  border: 2px solid var(--node-glass-border, rgba(255, 179, 198, 0.8));
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  position: fixed;
  right: 100px;
  bottom: var(--bottom);
  transform: translateX(50%);
  overflow: hidden;
  color: #fff;
  font-size: 0;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  z-index: 50;
  transition: 
    right 0.4s cubic-bezier(0.4, 0, 0.2, 1),
    width 0.3s ease, 
    border-radius 0.3s ease, 
    background-color 0.3s ease;
}

.auto-layout-btn.panel-open {
  right: 480px; /* Panel 380 + Original Right 100 */
}

.icon-wrap {
  position: relative;
  width: var(--icon-size);
  height: var(--icon-size);
  transition: opacity 0.1s ease, width 0.1s ease, height 0.2s ease;
}

.svgIcon {
  width: var(--icon-size);
  height: var(--icon-size);
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-40%, -50%);
  transition: opacity 0.2s ease;
  display: block;
  object-fit: contain;
}

.auto-layout-btn:hover {
  width: 140px;
  border-radius: 50px;
  background: var(--rose-300, #FFD9E8);
  font-size: 13px;
  gap: 0;
}

.auto-layout-btn:hover .icon-wrap {
  opacity: 0;
  width: 0;
  height: 0;
}
</style>
