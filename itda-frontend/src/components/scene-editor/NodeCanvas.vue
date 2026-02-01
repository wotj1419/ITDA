<script setup lang="ts">
/**
 * NodeCanvas - Vue Flow 기반 메인 캔버스
 *
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 2
 */
import { markRaw, onMounted, onUnmounted, ref, watch } from 'vue';
import { VueFlow, useVueFlow } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import { nodeTypes } from './nodes';
import FlowingEdge from './edges/FlowingEdge.vue';
import { useSceneNodeStore } from '../../stores/sceneNode';
import { useCollabStore } from '../../stores/collab';
import { useAutoLayout } from '../../composables/useAutoLayout';
import { NodeType } from '../../types/ui/sceneNodes';
import type { AnyNodeData } from '../../types/ui/sceneNodes';

// Vue Flow 스타일 import
import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import '@vue-flow/controls/dist/style.css';

// =============================================================================
// Props & Emits
// =============================================================================

interface Props {
  sceneId: string;
}

defineProps<Props>();

const emit = defineEmits<{
  (e: 'node-select', nodeId: string | null): void;
}>();

// =============================================================================
// Store & Composables
// =============================================================================

const nodeStore = useSceneNodeStore();
const collabStore = useCollabStore();
const { getLayoutedElements } = useAutoLayout();
const {
  fitView,
  onNodeClick,
  onNodeDrag,
  onNodeDragStart,
  onNodeDragStop,
  onSelectionDragStart,
  onSelectionDragStop,
  screenToFlowCoordinate,
  vueFlowRef,
  viewport,
} = useVueFlow();

type FitViewOptions = {
  padding?: number;
  minZoom?: number;
  maxZoom?: number;
};

const DEFAULT_HEADER_POSITION = { x: 0, y: -200 };
const INITIAL_ZOOM = 0.7;
const INITIAL_FIT_OPTIONS: FitViewOptions = {
  padding: 0.2,
  minZoom: 0.25,
  maxZoom: INITIAL_ZOOM,
};

const edgeTypes = markRaw({
  flowing: FlowingEdge,
} as const);

const canvasRef = ref<HTMLDivElement | null>(null);

function handleWindowMouseMove(event: MouseEvent): void {
  const el = vueFlowRef?.value;
  if (!el) return;
  const rect = el.getBoundingClientRect();
  if (rect.width <= 0 || rect.height <= 0) return;
  if (
    event.clientX < rect.left ||
    event.clientX > rect.right ||
    event.clientY < rect.top ||
    event.clientY > rect.bottom
  ) {
    return;
  }
  const flowPoint = screenToFlowCoordinate({ x: event.clientX, y: event.clientY });
  if (!Number.isFinite(flowPoint.x) || !Number.isFinite(flowPoint.y)) return;
  collabStore.updateCursor(flowPoint.x, flowPoint.y);
}

function flowToScreenCoordinate(pos: { x: number; y: number }): { x: number; y: number } {
  const el = vueFlowRef?.value;
  if (!el) return { x: 0, y: 0 };
  const rect = el.getBoundingClientRect();
  const view = viewport.value;
  const zoom = view?.zoom ?? 1;
  const tx = view?.x ?? 0;
  const ty = view?.y ?? 0;
  return {
    x: pos.x * zoom + tx + rect.left,
    y: pos.y * zoom + ty + rect.top,
  };
}

onMounted(() => {
  collabStore.setFlowToScreenCoordinate(flowToScreenCoordinate);
  window.addEventListener('mousemove', handleWindowMouseMove);
});

onUnmounted(() => {
  collabStore.setFlowToScreenCoordinate(null);
  window.removeEventListener('mousemove', handleWindowMouseMove);
});

// =============================================================================
// Lifecycle
// =============================================================================

const hasAppliedInitialLayout = ref(false);

function hasMeaningfulPositions(): boolean {
  return nodeStore.nodes.some((node) => {
    const position = node.position ?? { x: 0, y: 0 };
    if (node.data?.type === NodeType.SCENE_HEADER) {
      return position.x !== DEFAULT_HEADER_POSITION.x || position.y !== DEFAULT_HEADER_POSITION.y;
    }
    return position.x !== 0 || position.y !== 0;
  });
}

watch(
  () => [nodeStore.isLoading, nodeStore.nodes.length, nodeStore.edges.length] as const,
  ([isLoading, nodeCount, edgeCount]) => {
    const nodeTotal = typeof nodeCount === 'number' ? nodeCount : 0;
    const edgeTotal = typeof edgeCount === 'number' ? edgeCount : 0;
    if (isLoading) return;
    if (nodeTotal <= 1) return;
    if (edgeTotal === 0) return;
    if (hasAppliedInitialLayout.value) return;

    if (hasMeaningfulPositions()) {
      hasAppliedInitialLayout.value = true;
      return;
    }

    hasAppliedInitialLayout.value = true;
    applyLayout(INITIAL_FIT_OPTIONS);
  },
  { immediate: true }
);

watch(
  () => nodeStore.sceneId,
  () => {
    hasAppliedInitialLayout.value = false;
  }
);

// =============================================================================
// Layout
// =============================================================================

function applyLayout(fitOptions?: FitViewOptions): void {
  const { nodes: layoutedNodes } = getLayoutedElements(
    nodeStore.nodes,
    nodeStore.edges,
    // 커스텀 자동 정렬용 간격 설정 (형제 노드 균등 간격 분배)
    { direction: 'TB', nodeSep: 80, rankSep: 100 }
  );

  layoutedNodes.forEach((layoutedNode) => {
    const storeNode = nodeStore.nodes.find((n) => n.id === layoutedNode.id);
    if (storeNode) {
      storeNode.position = layoutedNode.position;
    }
  });

  // 약간의 지연 후 fitView
  setTimeout(() => {
    const resolvedOptions = fitOptions ? { ...fitOptions } : { padding: 0.2 };
    if (resolvedOptions.padding === undefined) {
      resolvedOptions.padding = 0.2;
    }
    fitView(resolvedOptions);
  }, 100);

  nodeStore.persistNodePositions();
}

// =============================================================================
// Event Handlers
// =============================================================================

onNodeClick(({ node }) => {
  // end shot 선택 모드 처리
  if (nodeStore.selectionMode === 'selectEndShot') {
    const nodeData = node.data as AnyNodeData;
    if (nodeData.type === NodeType.SHOT) {
      nodeStore.setEndShot(node.id);
      return;
    }
  }

  // 일반 선택
  nodeStore.selectNode(node.id);
  emit('node-select', node.id);
});

const handleNodeDragStart = (event: any) => {
  nodeStore.pushPositionSnapshot();
  const node = event?.node;
  if (node?.id) {
    collabStore.startNodeDrag(String(node.id));
  }
};

const handleNodeDrag = (event: any) => {
  const node = event?.node;
  if (!node?.id) return;
  const x = node.position?.x ?? 0;
  const y = node.position?.y ?? 0;
  collabStore.updateNodeMove(String(node.id), x, y);
};

const handleNodeDragStop = (event: any) => {
  nodeStore.persistNodePositions();
  const node = event?.node;
  if (node?.id) {
    const x = node.position?.x ?? 0;
    const y = node.position?.y ?? 0;
    collabStore.finishNodeDrag(String(node.id), x, y);
  }
};

onNodeDragStart(handleNodeDragStart);
onNodeDrag(handleNodeDrag);
onNodeDragStop(handleNodeDragStop);
onSelectionDragStart(handleNodeDragStart);
onSelectionDragStop(handleNodeDragStop);

function handlePaneClick(): void {
  // 캔버스 빈 영역 클릭 시 선택 해제
  nodeStore.selectNode(null);
  emit('node-select', null);
}


// =============================================================================
// Node Actions
// =============================================================================

async function handleAddChild(nodeId: string, nodeType: NodeType): Promise<void> {
  switch (nodeType) {
    case NodeType.SCENE_HEADER:
      await nodeStore.addMasterImageNode(nodeId);
      break;
    case NodeType.MASTER_IMAGE:
      await nodeStore.addStoryboardGridNode(nodeId);
      break;
    case NodeType.STORYBOARD_GRID:
      await nodeStore.addShotNode(nodeId);
      break;
    case NodeType.SHOT:
      await nodeStore.addVideoNode(nodeId);
      break;
  }
  // 레이아웃 재적용
  applyLayout();
}

async function handleConfirmVideo(nodeId: string): Promise<void> {
  await nodeStore.toggleVideoConfirm(nodeId);
}

// Expose for parent
defineExpose({
  applyLayout,
});
</script>

<template>
  <div
    ref="canvasRef"
    class="node-canvas"
    data-collab-canvas
    :class="{ 'selection-mode-active': nodeStore.selectionMode === 'selectEndShot' }"
  >
    <VueFlow
      v-model:nodes="nodeStore.nodes"
      v-model:edges="nodeStore.edges"
      :node-types="nodeTypes"
      :edge-types="edgeTypes"
      :default-viewport="{ x: 0, y: 0, zoom: INITIAL_ZOOM }"
      :min-zoom="0.25"
      :max-zoom="2"
      @pane-click="handlePaneClick"
      @node-resize-start="handleNodeDragStart"
    >
      <!-- Background -->
      <Background variant="dots" color="#FFD6E5" :gap="60" :size="5" />

      <!-- Controls -->
      <Controls position="bottom-left" />

      <!-- Custom Node Events -->
      <template #node-sceneHeader="nodeProps">
        <component
          :is="nodeTypes.sceneHeader"
          v-bind="nodeProps"
          @add-child="handleAddChild(nodeProps.id, NodeType.SCENE_HEADER)"
        />
      </template>

      <template #node-masterImage="nodeProps">
        <component
          :is="nodeTypes.masterImage"
          v-bind="nodeProps"
          @add-child="handleAddChild(nodeProps.id, NodeType.MASTER_IMAGE)"
        />
      </template>

      <template #node-storyboardGrid="nodeProps">
        <component
          :is="nodeTypes.storyboardGrid"
          v-bind="nodeProps"
          @add-child="handleAddChild(nodeProps.id, NodeType.STORYBOARD_GRID)"
        />
      </template>

      <template #node-shot="nodeProps">
        <component
          :is="nodeTypes.shot"
          v-bind="nodeProps"
          @add-child="handleAddChild(nodeProps.id, NodeType.SHOT)"
        />
      </template>

      <template #node-video="nodeProps">
        <component
          :is="nodeTypes.video"
          v-bind="nodeProps"
          @confirm="handleConfirmVideo(nodeProps.id)"
        />
      </template>
    </VueFlow>

    <!-- End Shot Selection Mode Overlay -->
    <div
      v-if="nodeStore.selectionMode === 'selectEndShot'"
      class="selection-mode-overlay"
    >
      <div class="selection-mode-message">
        <span>끝 샷을 선택하세요</span>
        <button @click="nodeStore.cancelSelectEndShot()">취소</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.node-canvas {
  width: 100%;
  height: 100%;
  position: relative;
  background: var(--rose-canvas, #FBFBFC);
}

.selection-mode-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 133, 161, 0.1);
  pointer-events: none;
  z-index: 100;
}

.selection-mode-message {
  position: absolute;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.5rem;
  background: white;
  border: 2px solid var(--rose-500, #FF85A1);
  border-radius: var(--radius-full, 9999px);
  box-shadow: var(--shadow-lg);
  pointer-events: auto;
}

.selection-mode-message span {
  font-weight: 600;
  color: var(--gray-700, #374151);
}

.selection-mode-message button {
  padding: 0.25rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--gray-600);
  background: var(--gray-100);
  border: 1px solid var(--gray-200);
  border-radius: 0.375rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.selection-mode-message button:hover {
  background: var(--gray-200);
}
</style>
