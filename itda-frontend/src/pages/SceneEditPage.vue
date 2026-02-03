<script setup lang="ts">
/**
 * SceneEditPage - Vue Flow 노드 기반 씬 에디터 페이지
 * Master → Grid → Shot → Video 흐름의 노드 편집
 */
import { computed, onMounted, onUnmounted, provide, watch, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useProjectStore } from '../stores/project';
import { useSceneStore } from '../stores/scene';
import { useSceneNodeStore } from '../stores/sceneNode';
import { useTimelineStore } from '../stores/timeline';
import { useObjectStore } from '../stores/object';
import { useUIStore } from '../stores/ui';
import { useCollabStore } from '../stores/collab';
import { TIMELINE_PLAYBACK_MODAL_ID } from '../constants/ui';
import type { ProjectDetail } from '../types/api/projects';

import EditorLayout from '../layouts/EditorLayout.vue';
import EditorHeader from '../components/editor/EditorHeader.vue';
import NodeCanvas from '../components/scene-editor/NodeCanvas.vue';
import NodePanelContainer from '../components/scene-editor/panels/NodePanelContainer.vue';
import AutoLayoutButton from '../components/scene-editor/AutoLayoutButton.vue';
import MiniTimeline from '../components/editor/MiniTimeline.vue';
import TimelinePlaybackModal from '../components/timeline/TimelinePlaybackModal.vue';
import NodeDeleteConfirmModal from '../components/scene-editor/NodeDeleteConfirmModal.vue';
import { useLayoutButtonPosition } from '../composables/useLayoutButtonPosition';
import { useSceneEditorEvents } from '../composables/useSceneEditorEvents';
import { getSceneNodeDisplayName } from '../utils/sceneNodeLabels';

// =============================================================================
// Composables & Stores
// =============================================================================

const route = useRoute();
const projectStore = useProjectStore();
const sceneStore = useSceneStore();
const nodeStore = useSceneNodeStore();
const timelineStore = useTimelineStore();
const objectStore = useObjectStore();
const uiStore = useUIStore();
const collabStore = useCollabStore();

const nodeCanvasRef = ref<InstanceType<typeof NodeCanvas> | null>(null);
const pendingDeleteNodeId = ref<string | null>(null);
const pendingDeleteHasChildren = ref(false);

const NODE_DELETE_MODAL_ID = 'node-delete-confirm';

/**
 * 오른쪽 속성 패널 표시 여부
 */
const isPanelOpen = computed(() => !!nodeStore.selectedNodeId);
const isMockMode = computed(() => import.meta.env.DEV && String(route.query.mock ?? '') === 'true');

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
  if (!scene) return '씬';
  return `씬 ${scene.order}: ${scene.title}`;
});

// Keep mini timeline in sync with /projects/:id/scenes/:sceneId/timeline data source.
const timelineClips = computed(() => timelineStore.orderedClips);
const totalDuration = computed(() => timelineStore.totalDuration);

/**
 * 레이아웃 정렬 버튼의 동적 bottom 위치
 * 타임라인에 클립이 있으면 타임라인 높이만큼 위로 이동
 */
const { layoutButtonBottom } = useLayoutButtonPosition(timelineClips);

// =============================================================================
// Lifecycle
// =============================================================================

onMounted(async () => {
  window.addEventListener('keydown', handleEditorKeydown);
  window.addEventListener('beforeunload', handleBeforeUnload);
  if (projectId.value && sceneId.value) {
    if (isMockMode.value) {
      const now = new Date().toISOString();

      if (!projectStore.currentProject) {
        projectStore.currentProject = {
          projectId: projectId.value,
          title: '해커톤 데모 프로젝트 1440',
          description: 'Mock Project (DEV)',
          genre: 'Mock',
          thumbnailUrl: '',
          role: 'OWNER',
          myRole: 'OWNER',
          memberCount: 1,
          sceneCount: 1,
          updatedAt: now,
          createdAt: now,
          ownerId: 0,
          members: [],
          isDeleted: false,
        } satisfies ProjectDetail;
      }

      sceneStore.currentProjectId = projectId.value;
      sceneStore.scenes = [
        {
          sceneId: Number(sceneId.value) || 1,
          title: '새 씬 1',
          description: '설명 없음',
          order: 1,
          status: 'IN_PROGRESS',
          thumbnailUrl: '',
        },
      ];

      await objectStore.loadObjects(projectId.value);
      nodeStore.loadMockSceneNodes(sceneId.value, true);
      return;
    }

    await Promise.all([
      projectStore.loadProject(projectId.value),
      sceneStore.loadScenes(projectId.value),
      objectStore.loadObjects(projectId.value),
      timelineStore.loadClips(projectId.value, Number(sceneId.value)),
    ]);

    // Vue Flow 노드 로드 (씬 정보 함께 전달)
    const scene = currentScene.value;
    await nodeStore.loadSceneNodes(
      sceneId.value,
      scene
        ? {
            title: scene.title,
            description: scene.description || '',
            order: scene.order,
          }
        : undefined
    );

    // 협업 방 입장
    collabStore.joinRoom(projectId.value);
    collabStore.updateLocation('SCENE_EDIT', Number(sceneId.value));
  }
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleEditorKeydown);
  window.removeEventListener('beforeunload', handleBeforeUnload);
  // 페이지 이탈 시 협업 방 퇴장
  nodeStore.clearNodes();
  timelineStore.clearTimeline();
});

// Route 변경 시 노드 다시 로드
watch([projectId, sceneId], async ([, newSceneId]) => {
  if (newSceneId) {
    if (nodeStore.sceneId && nodeStore.sceneId !== String(newSceneId)) {
      nodeStore.flushPersistNodePositions();
      nodeStore.flushSave();
    }
    if (isMockMode.value) {
      if (projectId.value) {
        await objectStore.loadObjects(projectId.value);
      }
      nodeStore.loadMockSceneNodes(newSceneId as string, true);
      return;
    }
    const scene = sceneStore.scenes.find((s) => s.sceneId === Number(newSceneId));
    await Promise.all([
      nodeStore.loadSceneNodes(newSceneId as string, scene ? {
        title: scene.title,
        description: scene.description || '',
        order: scene.order,
      } : undefined),
      timelineStore.loadClips(projectId.value, Number(newSceneId)),
    ]);
    collabStore.updateLocation('SCENE_EDIT', Number(newSceneId));
  }
});

// =============================================================================
// Event Handlers
// =============================================================================

/**
 * 노드 선택
 */
function handleNodeSelect(nodeId: string | null): void {
  const wasSelected = nodeId !== null && nodeId === nodeStore.selectedNodeId;
  nodeStore.selectNode(nodeId);
  if (nodeId && !wasSelected) {
    const node = nodeStore.nodes.find((n) => n.id === nodeId);
    const nodeLabel = node?.data ? getSceneNodeDisplayName(node.data) : `노드 ${nodeId}`;
    uiStore.showToast({
      type: 'info',
      title: '노드 선택',
      message: `${nodeLabel} 선택됨`,
      duration: 2000,
    });
  }
}

function requestDeleteNode(nodeId: string): void {
  if (!nodeStore.canDeleteNode(nodeId)) return;
  pendingDeleteNodeId.value = nodeId;
  pendingDeleteHasChildren.value = nodeStore.hasDescendants(nodeId);
  uiStore.openModal(NODE_DELETE_MODAL_ID);
}

provide('nodeDeleteRequest', requestDeleteNode);

function handleDeleteConfirm(): void {
  if (pendingDeleteNodeId.value) {
    nodeStore.deleteNode(pendingDeleteNodeId.value);
  }
  pendingDeleteNodeId.value = null;
  pendingDeleteHasChildren.value = false;
}

function handleDeleteCancel(): void {
  pendingDeleteNodeId.value = null;
  pendingDeleteHasChildren.value = false;
}

async function handleTimelineReorder(clipIds: string[]): Promise<void> {
  const success = await timelineStore.reorderClips(clipIds);
  if (!success) return;

  clipIds.forEach((clipId, index) => {
    const clip = timelineStore.clips.find((item) => item.clipId === clipId);
    if (!clip || typeof clip.nodeId !== 'number') return;
    nodeStore.updateNodeLocal(String(clip.nodeId), {
      timelineOrder: index + 1,
      isConfirmed: true,
    });
  });
}

async function handleTimelineRemove(clipId: string): Promise<void> {
  const target = timelineStore.clips.find((clip) => clip.clipId === clipId);
  const success = await timelineStore.removeClip(clipId);
  if (!success) return;

  if (target && typeof target.nodeId === 'number') {
    nodeStore.updateNodeLocal(String(target.nodeId), {
      isConfirmed: false,
      timelineOrder: undefined,
    });
  }
}

function handleTimelinePlay(): void {
  if (!timelineClips.value.length) {
    uiStore.showToast({
      type: 'error',
      title: '재생 불가',
      message: '재생 가능한 영상이 없습니다.',
    });
    return;
  }
  uiStore.openModal(TIMELINE_PLAYBACK_MODAL_ID);
}

/**
 * 자동 레이아웃 적용
 */
function handleAutoLayout(): void {
  nodeCanvasRef.value?.applyLayout();
}

const { handleBeforeUnload, handleEditorKeydown } = useSceneEditorEvents({
  nodeStore,
  uiStore,
  nodeDeleteModalId: NODE_DELETE_MODAL_ID,
  requestDeleteNode,
});
</script>

<template>
  <EditorLayout
    :project-title="project?.title || 'Project'"
    :scene-title="sceneTitle"
    scene-badge="씬 편집"
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
      <AutoLayoutButton
        :is-panel-open="isPanelOpen"
        :bottom-offset="layoutButtonBottom"
        :extra-bottom="12"
        @click="handleAutoLayout"
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
        @reorder="handleTimelineReorder"
        @remove="handleTimelineRemove"
        @play="handleTimelinePlay"
      />
    </template>
  </EditorLayout>

  <NodeDeleteConfirmModal
    :node-id="pendingDeleteNodeId"
    :has-children="pendingDeleteHasChildren"
    @confirm="handleDeleteConfirm"
    @cancel="handleDeleteCancel"
  />

  <TimelinePlaybackModal :clips="timelineClips" />

</template>
