<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  variant?: 'primary' | 'secondary' | 'ghost' | 'outline'
  size?: 'sm' | 'md' | 'lg'
  icon?: boolean
  disabled?: boolean
  loading?: boolean
  type?: 'button' | 'submit' | 'reset'
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  icon: false,
  disabled: false,
  loading: false,
  type: 'button',
})

const classes = computed(() => [
  'btn',
  `btn-${props.variant}`,
  `btn-${props.size}`,
  {
    'btn-icon': props.icon,
    'btn-loading': props.loading,
  },
])
</script>

<template>
  <button
    :type="type"
    :class="classes"
    :disabled="disabled || loading"
  >
    <span v-if="loading" class="btn-spinner"></span>
    <slot />
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-weight: 500;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Sizes */
.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.75rem;
}

.btn-md {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
}

.btn-lg {
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
}

/* Variants */
.btn-primary {
  background: linear-gradient(135deg, var(--rose-400), var(--rose-500));
  color: white;
  box-shadow: 0 2px 8px rgba(244, 63, 94, 0.25);
}

.btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--rose-500), var(--rose-600));
  box-shadow: 0 4px 12px rgba(244, 63, 94, 0.35);
  transform: translateY(-1px);
}

.btn-secondary {
  background: var(--rose-50);
  color: var(--rose-500);
  border: 1px solid var(--rose-200);
}

.btn-secondary:hover:not(:disabled) {
  background: var(--rose-100);
}

.btn-ghost {
  background: transparent;
  color: var(--gray-600);
}

.btn-ghost:hover:not(:disabled) {
  background: var(--gray-100);
}

.btn-outline {
  background: transparent;
  color: var(--gray-700);
  border: 1px solid var(--gray-200);
}

.btn-outline:hover:not(:disabled) {
  background: var(--gray-50);
  border-color: var(--gray-300);
}

/* Icon button */
.btn-icon {
  padding: 0.5rem;
  aspect-ratio: 1;
}

.btn-icon.btn-sm {
  padding: 0.375rem;
}

.btn-icon.btn-lg {
  padding: 0.75rem;
}

/* Loading spinner */
.btn-loading {
  position: relative;
  color: transparent;
}

.btn-spinner {
  position: absolute;
  width: 1em;
  height: 1em;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
