<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useCollabStore } from '../../stores/collab'
import CursorOverlay from './CursorOverlay.vue'

const collabStore = useCollabStore()
function handleMouseMove(event: MouseEvent) {
    // Calculate percentage position
    const x = (event.clientX / window.innerWidth) * 100
    const y = (event.clientY / window.innerHeight) * 100
    
    collabStore.updateCursor(x, y)
}

onMounted(() => {
    window.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
    window.removeEventListener('mousemove', handleMouseMove)
})
</script>

<template>
  <div class="collab-container-root">
      <CursorOverlay />
      <slot></slot>
  </div>
</template>

<style scoped>
/* Only functional styles, no visuals */
.collab-container-root {
    position: absolute;
    top: 0;
    left: 0;
    width: 0;
    height: 0;
    overflow: visible;
    z-index: 9999;
}

</style>
