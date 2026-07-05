<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import CollabContainer from './CollabContainer.vue'
import { useCollabStore } from '../../stores/collab'

const collabStore = useCollabStore()

onMounted(() => {
  collabStore.rejoinIfNeeded()
})

onUnmounted(() => {
  if (collabStore.isConnected) {
    collabStore.leaveRoom()
  }
})
</script>

<template>
  <CollabContainer />
</template>
