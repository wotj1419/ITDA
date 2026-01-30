<script setup lang="ts">
interface Props {
  name?: string | null
}

const props = defineProps<Props>()
</script>

<template>
  <h2 class="welcome-title">
    반가워요<span v-if="props.name">, <span class="user-name">{{ props.name }}</span>님</span> ✨
  </h2>
</template>

<style scoped>
.welcome-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-800);
  margin: 0;
  white-space: nowrap;
}

.user-name {
  color: var(--rose-600);
}
</style>
