<script setup lang="ts">
import { Users } from 'lucide-vue-next'

const emit = defineEmits<{
  (e: 'click'): void
}>()
</script>

<template>
  <button class="collab-btn" @click="emit('click')">
    <Users class="icon" />
    <span class="text">협업 시작</span>
  </button>
</template>

<style scoped>
.collab-btn {
  /* Layout & Sizing */
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 1.2rem;
  height: 40px; /* Match header button height */
  min-width: 120px;
  
  /* Base: white background with rose border */
  background: white;
  border: 3px solid var(--rose-500);
  border-radius: 999px;
  cursor: pointer;
  outline: none;
  
  /* Text Styles */
  color: var(--rose-500);
  font-weight: 600;
  font-size: 0.95rem;
  text-decoration: none;
  
  /* Animation Base */
  position: relative;
  transition: background-color 0.2s ease, color 0.2s ease, border-color 0.2s ease;
  overflow: hidden;
  z-index: 0;
}

/* Icons */
.icon {
  width: 18px;
  height: 18px;
  color: currentColor;
  transition: none;
}

/* Hover Effects */
.collab-btn:hover {
  color: white;
  background: var(--rose-500);
}

.collab-btn:hover .icon {
  color: currentColor;
}

.collab-btn:active {
  background: var(--rose-600);
  border-color: var(--rose-600);
}
</style>
