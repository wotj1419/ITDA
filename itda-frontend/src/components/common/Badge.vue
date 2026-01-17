<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  variant?: 'default' | 'rose' | 'success' | 'warning' | 'error' | 'info'
  size?: 'sm' | 'md'
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  size: 'sm',
})

const classes = computed(() => [
  'badge',
  `badge-${props.variant}`,
  `badge-${props.size}`,
])
</script>

<template>
  <span :class="classes">
    <slot />
  </span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  font-weight: 500;
  border-radius: 9999px;
  white-space: nowrap;
}

/* Sizes */
.badge-sm {
  padding: 0.125rem 0.5rem;
  font-size: 0.625rem;
}

.badge-md {
  padding: 0.25rem 0.75rem;
  font-size: 0.75rem;
}

/* Variants */
.badge-default {
  background: var(--gray-100);
  color: var(--gray-600);
}

.badge-rose {
  background: var(--rose-100);
  color: var(--rose-600);
}

.badge-success {
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}

.badge-warning {
  background: rgba(234, 179, 8, 0.1);
  color: #ca8a04;
}

.badge-error {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.badge-info {
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
}
</style>
