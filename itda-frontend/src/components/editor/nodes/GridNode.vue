<script setup lang="ts">
/**
 * GridNode - 그리드 노드 컴포넌트
 * 2x2, 2x3, 3x3 레이아웃의 스토리보드 그리드
 */
import { computed } from 'vue';
import type { Node } from '../../../types';
import { Grid3X3, CheckCircle, Plus } from 'lucide-vue-next';

// =============================================================================
// Props & Emits
// =============================================================================

interface Props {
  node: Node;
  selected?: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'select'): void;
  (e: 'add-shot', cellIndex: number): void;
}>();

// =============================================================================
// Computed
// =============================================================================

const gridLayout = computed(() => props.node.settings?.gridLayout || '2x3');
const gridCells = computed(() => props.node.settings?.gridCells || []);
const gridClass = computed(() => `grid-${gridLayout.value.replace('x', '-')}`);
</script>

<template>
  <div
    :class="[
      'node',
      'node-grid',
      `state-${node.status.toLowerCase()}`,
      { selected },
    ]"
    @click="emit('select')"
  >
    <!-- Handles -->
    <div class="node-handle node-handle-top connected"></div>
    <div class="node-handle node-handle-bottom connected"></div>

    <!-- Header -->
    <div class="node-header">
      <div class="node-header-title">
        <Grid3X3 class="node-icon" />
        <span class="node-title">{{ node.title || `Grid (${gridLayout})` }}</span>
      </div>
      <span v-if="node.status === 'SUCCEEDED'" class="node-status node-status-done">
        <CheckCircle class="status-icon" />
        완료
      </span>
    </div>

    <!-- Body -->
    <div class="node-body">
      <!-- Grid Container -->
      <div :class="['grid-container', gridClass]">
        <div
          v-for="cell in gridCells"
          :key="cell.index"
          :class="['grid-cell', { selected: cell.selected }]"
          @click.stop="emit('add-shot', cell.index)"
        >
          <img
            v-if="cell.imageUrl"
            :src="cell.imageUrl"
            :alt="`Cell ${cell.index + 1}`"
            class="cell-image"
          />
          <div v-else class="cell-placeholder">
            <Plus class="placeholder-icon" />
          </div>

          <!-- Selection Indicator -->
          <div v-if="cell.selected" class="cell-selected-badge">
            <CheckCircle class="check-icon" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==========================================================================
   Node Base
   ========================================================================== */

.node {
  position: relative;
  width: 100%;
  background: white;
  border-radius: 12px;
  border: 2px solid var(--node-grid);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}

.node:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.node.selected {
  box-shadow: var(--node-grid-glow);
}

/* ==========================================================================
   Node Header
   ========================================================================== */

.node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: linear-gradient(135deg, var(--node-grid), var(--node-grid-light));
}

.node-header-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: white;
}

.node-icon {
  width: 16px;
  height: 16px;
}

.node-title {
  font-weight: 600;
  font-size: 0.875rem;
}

.node-status {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: white;
  opacity: 0.9;
}

.status-icon {
  width: 14px;
  height: 14px;
}

/* ==========================================================================
   Node Body
   ========================================================================== */

.node-body {
  padding: 1rem;
}

/* ==========================================================================
   Grid Container
   ========================================================================== */

.grid-container {
  display: grid;
  gap: 0.5rem;
}

.grid-2-2 {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
}

.grid-2-3 {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, 1fr);
}

.grid-3-3 {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
}

/* ==========================================================================
   Grid Cell
   ========================================================================== */

.grid-cell {
  position: relative;
  aspect-ratio: 16/9;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.grid-cell:hover {
  border-color: var(--rose-300);
}

.grid-cell.selected {
  border-color: var(--node-grid);
}

.cell-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cell-placeholder {
  width: 100%;
  height: 100%;
  background: var(--gray-100);
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-icon {
  width: 20px;
  height: 20px;
  color: var(--gray-400);
}

.cell-selected-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  background: var(--node-grid);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.check-icon {
  width: 12px;
  height: 12px;
  color: white;
}

/* ==========================================================================
   Node Handle
   ========================================================================== */

.node-handle {
  position: absolute;
  width: 12px;
  height: 12px;
  background: white;
  border: 2px solid var(--node-grid);
  border-radius: 50%;
  z-index: 5;
}

.node-handle-top {
  top: -6px;
  left: 50%;
  transform: translateX(-50%);
}

.node-handle-bottom {
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
}

.node-handle.connected {
  background: var(--node-grid);
}
</style>
