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
/* 
 * Button.vue - 컴포넌트 전용 스타일만 정의
 * 기본 .btn, .btn-primary 등은 base.css 전역 스타일 사용
 */

/* Icon button - 컴포넌트 전용 */
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

/* Loading state - 컴포넌트 전용 */
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
