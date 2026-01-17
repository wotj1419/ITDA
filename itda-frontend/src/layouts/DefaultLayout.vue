<script setup lang="ts">
import AppSidebar from '../components/common/AppSidebar.vue'
import AppHeader from '../components/common/AppHeader.vue'

interface Props {
  showHeader?: boolean
  showCollaborators?: boolean
  showShareButton?: boolean
  showCollabButton?: boolean
}

withDefaults(defineProps<Props>(), {
  showHeader: true,
  showCollaborators: true,
  showShareButton: true,
  showCollabButton: true,
})
</script>

<template>
  <div class="app-container">
    <AppSidebar />

    <main class="main-wrapper">
      <AppHeader
        v-if="showHeader"
        :show-collaborators="showCollaborators"
        :show-share-button="showShareButton"
        :show-collab-button="showCollabButton"
      >
        <template #actions>
          <slot name="header-actions" />
        </template>
      </AppHeader>

      <div class="main-content">
        <slot />
      </div>
    </main>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  min-height: 100vh;
  background: var(--rose-canvas);
}

.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 2rem;
  overflow-y: auto;
}
</style>
