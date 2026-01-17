<script setup lang="ts">
/**
 * NodeConnections - 노드 간 연결선을 SVG로 렌더링
 */
import { computed } from 'vue';
import type { Node } from '../../types';

// =============================================================================
// Props
// =============================================================================

interface Props {
  nodes: Node[];
}

const props = defineProps<Props>();

// =============================================================================
// Constants
// =============================================================================

const NODE_HEIGHT = 280;
const NODE_GAP = 120;
const NODE_WIDTH = 320;

// =============================================================================
// Computed
// =============================================================================

interface Connection {
  id: string;
  path: string;
}

const connections = computed<Connection[]>(() => {
  const result: Connection[] = [];

  props.nodes.forEach((node, index) => {
    if (index === 0) return; // 첫 번째 노드는 연결선 없음

    const parentIndex = props.nodes.findIndex(
      (n) => n.nodeId === node.parentNodeId
    );
    if (parentIndex === -1) return;

    // 간단한 수직 연결선 계산
    const startY = (parentIndex + 1) * (NODE_HEIGHT + NODE_GAP) - NODE_GAP + 50;
    const endY = (index + 1) * (NODE_HEIGHT + NODE_GAP) - NODE_HEIGHT - NODE_GAP + 30;
    const midX = NODE_WIDTH / 2;

    // 베지어 곡선 경로
    const midY = (startY + endY) / 2;
    const path = `M ${midX} ${startY} C ${midX} ${midY}, ${midX} ${midY}, ${midX} ${endY}`;

    result.push({
      id: `conn-${node.parentNodeId}-${node.nodeId}`,
      path,
    });
  });

  return result;
});

const svgHeight = computed(() =>
  Math.max(800, props.nodes.length * (NODE_HEIGHT + NODE_GAP) + 100)
);
</script>

<template>
  <svg
    class="connections-svg"
    :width="NODE_WIDTH"
    :height="svgHeight"
    :viewBox="`0 0 ${NODE_WIDTH} ${svgHeight}`"
  >
    <defs>
      <!-- Gradient for connection lines -->
      <linearGradient id="line-gradient" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stop-color="var(--rose-300)" />
        <stop offset="100%" stop-color="var(--rose-400)" />
      </linearGradient>
    </defs>

    <!-- Connection Lines -->
    <path
      v-for="conn in connections"
      :key="conn.id"
      :d="conn.path"
      class="connection-line"
      fill="none"
      stroke="url(#line-gradient)"
      stroke-width="2"
      stroke-dasharray="6 4"
    />
  </svg>
</template>

<style scoped>
.connections-svg {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  z-index: 0;
}

.connection-line {
  opacity: 0.8;
  animation: dash 20s linear infinite;
}

@keyframes dash {
  to {
    stroke-dashoffset: -200;
  }
}
</style>
