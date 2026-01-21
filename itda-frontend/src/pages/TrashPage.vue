<script setup lang="ts">
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { Trash2 } from 'lucide-vue-next'
</script>

<template>
  <DefaultLayout>
    <div class="placeholder-container">
      <div class="content">
        <div class="icon-wrapper">
          <Trash2 class="icon-lg" />
        </div>
        <h1 class="title">Trash</h1>
        <p class="description">
          Deleted projects are moved here and can be restored within 30 days.<br />
          This feature is coming soon!
        </p>
      </div>
    </div>
  </DefaultLayout>
</template>

<style scoped>
.placeholder-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

.content {
  text-align: center;
  max-width: 400px;
}

.icon-wrapper {
  width: 80px;
  height: 80px;
  background: var(--rose-50);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
}

.icon-lg {
  width: 40px;
  height: 40px;
  color: var(--rose-500);
}

.title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--gray-900);
  margin: 0 0 0.75rem;
}

.description {
  color: var(--gray-500);
  line-height: 1.6;
}
</style>
