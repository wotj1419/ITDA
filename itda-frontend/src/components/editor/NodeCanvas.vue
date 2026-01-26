<script setup lang="ts">
/**
 * NodeCanvas - 메인 캔버스 컨테이너
 * 노드들을 세로로 배치하고 연결선을 표시
 */
import type { Node } from '../../types/api/nodes';
import NodeConnections from './NodeConnections.vue';
import MasterNode from './nodes/MasterNode.vue';
import GridNode from './nodes/GridNode.vue';
import ShotNode from './nodes/ShotNode.vue';
import VideoNode from './nodes/VideoNode.vue';

// =============================================================================
// Props
// =============================================================================

interface Props {
  nodes: Node[];
  selectedNodeId: number | null;
  zoomScale: number;
}

const props = defineProps<Props>();

// =============================================================================
// Emits
// =============================================================================

const emit = defineEmits<{
  (e: 'select-node', nodeId: number): void;
  (e: 'regenerate', nodeId: number): void;
  (e: 'add-grid', parentId: number): void;
  (e: 'add-shot', parentId: number, cellIndex: number): void;
  (e: 'add-video', parentId: number): void;
  (e: 'confirm-video', nodeId: number): void;
}>();

// =============================================================================
// Methods
// =============================================================================

/** 노드 타입에 따른 컴포넌트 반환 */
function getNodeComponent(type: string) {
  const components: Record<string, unknown> = {
    MASTER: MasterNode,
    GRID: GridNode,
    SHOT: ShotNode,
    VIDEO: VideoNode,
  };
  return components[type] || null;
}
</script>

<template>
  <div
    class="canvas-container"
    :style="{ transform: `scale(${zoomScale})` }"
  >
    <!-- SVG Connections -->
    <NodeConnections :nodes="nodes" />

    <!-- Nodes Column -->
    <div class="nodes-column">
      <component
        v-for="node in nodes"
        :key="node.nodeId"
        :is="getNodeComponent(node.type)"
        :node="node"
        :selected="selectedNodeId === node.nodeId"
        @select="emit('select-node', node.nodeId)"
        @regenerate="emit('regenerate', node.nodeId)"
        @add-grid="emit('add-grid', node.nodeId)"
        @add-shot="(cellIndex: number) => emit('add-shot', node.nodeId, cellIndex)"
        @add-video="emit('add-video', node.nodeId)"
        @confirm="emit('confirm-video', node.nodeId)"
      />
    </div>
  </div>
</template>

<style scoped>
.canvas-container {
  position: absolute;
  top: 40px;
  left: 50%;
  transform-origin: top center;
  transition: transform 0.2s ease;
}

.nodes-column {
  width: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 120px;
  padding: 20px 0;
  margin-left: -160px; /* center horizontally */
}
</style>
