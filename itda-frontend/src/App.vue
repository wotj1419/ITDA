<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import ToastContainer from './components/common/ToastContainer.vue'

const route = useRoute()
const CollabRouteLayer = defineAsyncComponent(() => import('./components/collab/CollabRouteLayer.vue'))

const showCollabUI = computed(() => {
  if (!route.name) return false
  return route.path.startsWith('/projects')
})
</script>

<template>
  <RouterView />
  <ToastContainer />
  <CollabRouteLayer v-if="showCollabUI" />
</template>

<style>
/* Additional global styles if needed */
</style>
