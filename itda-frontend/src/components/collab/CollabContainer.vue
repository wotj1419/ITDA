<script setup lang="ts">
import { computed } from 'vue';
import { useCollabStore } from '../../stores/collab';
import { useDraggable } from '../../composables/useDraggable';
import CollabPanel from './CollabPanel.vue';
import CollabPill from './CollabPill.vue';
import CollabAudioSinks from './CollabAudioSinks.vue'

const collabStore = useCollabStore();

// 드래그 기능 적용
const { position, isDragging, onMouseDown, shouldPreventClick } = useDraggable({
  initialRight: 24,
  initialBottom: 24,
  storageKey: 'collab-container-position',
});

/**
 * 협업 UI 표시 여부
 * connected 상태일 때만 표시
 */
const showCollab = computed(() => collabStore.isConnected);

function handleContainerClick() {
  // 드래그 중이거나 방금 드래그가 끝났다면 클릭 무시
  if (shouldPreventClick()) return;
  
  // 패널이 닫혀있을 때만(즉, Pill 상태일 때만) 토글하여 켬
  if (!collabStore.isPanelOpen) {
    collabStore.togglePanel();
  }
}
</script>

<template>
  <Transition name="fade">
    <div 
      v-if="showCollab" 
      class="collab-container"
      :class="{ 'is-dragging': isDragging }"
      :style="{ right: position.right + 'px', bottom: position.bottom + 'px' }"
      @mousedown="onMouseDown"
      @click="handleContainerClick"
    >
      <CollabAudioSinks />
      <!-- Expanded Panel -->
      <Transition name="scale" mode="out-in">
        <CollabPanel v-if="collabStore.isPanelOpen" key="panel" />
        <!-- Collapsed Pill -->
        <CollabPill v-else key="pill" />
      </Transition>
    </div>
  </Transition>
</template>

<style scoped>
.collab-container {
  position: fixed;
  z-index: 1000;
  cursor: grab;
  user-select: none;
}

.collab-container.is-dragging {
  cursor: grabbing;
}

/* Fade transition for container */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Scale transition for panel/pill switch */
.scale-enter-active,
.scale-leave-active {
  transition: all 0.2s ease;
}

.scale-enter-from {
  opacity: 0;
  transform: scale(0.95);
}

.scale-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
