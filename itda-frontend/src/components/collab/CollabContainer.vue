<script setup lang="ts">
import { computed } from 'vue';
import { useCollabStore } from '../../stores/collab';
import CollabPanel from './CollabPanel.vue';
import CollabPill from './CollabPill.vue';

const collabStore = useCollabStore();

/**
 * 협업 UI 표시 여부
 * connected 상태일 때만 표시
 */
const showCollab = computed(() => collabStore.isConnected);
</script>

<template>
  <Transition name="fade">
    <div v-if="showCollab" class="collab-container">
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
  bottom: 1.5rem;
  right: 1.5rem;
  z-index: 1000;
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
