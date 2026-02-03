<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import ToastContainer from './components/common/ToastContainer.vue'
import CollabContainer from './components/collab/CollabContainer.vue'
import { useCollabStore } from './stores/collab'

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
    return
  }
  collabStore.leaveRoom()
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
</template>

<style>
/* Additional global styles if needed */
</style>
