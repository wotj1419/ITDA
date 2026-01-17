<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  src?: string
  alt?: string
  size?: 'sm' | 'md' | 'lg'
  fallback?: string
  showMore?: number
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  alt: 'Avatar',
})

const classes = computed(() => [
  'avatar',
  `avatar-${props.size}`,
])

const initials = computed(() => {
  if (props.fallback) return props.fallback
  if (props.alt) {
    return props.alt
      .split(' ')
      .map((word) => word[0])
      .slice(0, 2)
      .join('')
      .toUpperCase()
  }
  return '?'
})
</script>

<template>
  <div :class="classes">
    <img
      v-if="src"
      :src="src"
      :alt="alt"
      class="avatar-image"
    />
    <span v-else-if="showMore" class="avatar-more">+{{ showMore }}</span>
    <span v-else class="avatar-fallback">{{ initials }}</span>
  </div>
</template>

<style scoped>
.avatar {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--rose-100);
  color: var(--rose-600);
  font-weight: 600;
  overflow: hidden;
  flex-shrink: 0;
}

/* Sizes */
.avatar-sm {
  width: 28px;
  height: 28px;
  font-size: 0.625rem;
}

.avatar-md {
  width: 36px;
  height: 36px;
  font-size: 0.75rem;
}

.avatar-lg {
  width: 48px;
  height: 48px;
  font-size: 1rem;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  text-transform: uppercase;
}

.avatar-more {
  font-size: 0.625rem;
  color: var(--gray-500);
  background: var(--gray-100);
}
</style>
