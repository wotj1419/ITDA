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

const hideCollabUI = computed(() => route.name === 'landing' || route.name === 'auth')

onMounted(() => {
  if (!hideCollabUI.value) {
    collabStore.rejoinIfNeeded()
  }
})

watch(hideCollabUI, (hide) => {
  if (!hide) {
    collabStore.rejoinIfNeeded()
  }
})
</script>

<template>
  <RouterView />
  <ToastContainer />
  <CollabContainer v-if="!hideCollabUI" />
  <FloatingControlBar v-if="!hideCollabUI" />
</template>

<style>
/* Additional global styles if needed */
</style>

