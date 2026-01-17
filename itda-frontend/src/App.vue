<script setup lang="ts">
import { RouterView } from 'vue-router'
import ToastContainer from './components/common/ToastContainer.vue'
import CollabContainer from './components/collab/CollabContainer.vue'
</script>

<template>
  <RouterView />
  <ToastContainer />
  <CollabContainer />
</template>

<style>
/* Additional global styles if needed */
</style>

