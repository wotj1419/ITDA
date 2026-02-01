<script setup lang="ts">
import CursorOverlay from './CursorOverlay.vue'
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
