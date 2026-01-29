<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import ToastContainer from './components/common/ToastContainer.vue'
import CollabContainer from './components/collab/CollabContainer.vue'
import FloatingControlBar from './components/common/FloatingControlBar.vue'
import { useCollabStore } from './stores/collab'

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
    return
  }
  collabStore.leaveRoom()
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

