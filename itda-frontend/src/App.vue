<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import ToastContainer from './components/common/ToastContainer.vue'
import CollabContainer from './components/collab/CollabContainer.vue'
import FloatingControlBar from './components/common/FloatingControlBar.vue'
import { socketManager } from './services/ws/socket'
import { useCollabStore } from './stores/collab'

// Initialize WebSocket connection (Singleton)
// In a real app, you might want to wait until login
socketManager.connect();

const collabStore = useCollabStore()
const route = useRoute()

const showCollabUI = computed(() => {
  if (!route.name) return false
  return route.path.startsWith('/projects')
})

const shouldLeaveOnRoute = computed(() => {
  if (!route.name) return false
  return !route.path.startsWith('/projects')
})

onMounted(() => {
  if (showCollabUI.value) {
    collabStore.rejoinIfNeeded()
  }
})

watch(showCollabUI, (show) => {
  if (show) {
    collabStore.rejoinIfNeeded()
  }
})

watch(shouldLeaveOnRoute, (shouldLeave) => {
  if (shouldLeave && collabStore.isConnected) {
    collabStore.leaveRoom()
  }
})
</script>

<template>
  <RouterView />
  <ToastContainer />
  <CollabContainer v-if="showCollabUI" />
  <FloatingControlBar v-if="showCollabUI" />
</template>

<style>
/* Additional global styles if needed */
</style>

