<script setup lang="ts">
/**
 * ZoomControls - 캔버스 줌 컨트롤 버튼
 */
import { Plus, Minus, Maximize2 } from 'lucide-vue-next';

// =============================================================================
// Emits
// =============================================================================

const emit = defineEmits<{
  (e: 'zoom-in'): void;
  (e: 'zoom-out'): void;
  (e: 'zoom-fit'): void;
}>();
</script>

<template>
  <div class="zoom-controls">
    <button
      class="zoom-btn"
      title="Zoom In"
      @click="emit('zoom-in')"
    >
      <Plus class="icon" />
    </button>

    <button
      class="zoom-btn"
      title="Zoom Out"
      @click="emit('zoom-out')"
    >
      <Minus class="icon" />
    </button>

    <button
      class="zoom-btn"
      title="Fit to Screen"
      @click="emit('zoom-fit')"
    >
      <Maximize2 class="icon" />
    </button>
  </div>
</template>

<style scoped>
.zoom-controls {
  position: fixed;
  bottom: 80px;
  left: calc(260px + 1.5rem);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  z-index: 30;
}

.zoom-btn {
  width: 40px;
  height: 40px;
  background: white;
  border: 1px solid var(--rose-200);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.zoom-btn:hover {
  border-color: var(--rose-300);
  background: var(--rose-50);
  transform: scale(1.05);
}

.zoom-btn:active {
  transform: scale(0.95);
}

.icon {
  width: 20px;
  height: 20px;
  color: var(--gray-600);
}

.zoom-btn:hover .icon {
  color: var(--rose-500);
}
</style>
