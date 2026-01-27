<script setup lang="ts">
import { computed } from 'vue'
import { useUIStore } from '../../stores/ui'
import { CheckCircle, XCircle, AlertTriangle, Info, X, Loader2 } from 'lucide-vue-next'

const uiStore = useUIStore()

const toastGroups = computed(() => [
  {
    key: 'top-right',
    className: 'toast-container--top-right',
    toasts: uiStore.toasts.filter((toast) => toast.position !== 'bottom-right'),
  },
  {
    key: 'bottom-right',
    className: 'toast-container--bottom-right',
    toasts: uiStore.toasts.filter((toast) => toast.position === 'bottom-right'),
  },
])

const getIcon = (type: string) => {
  switch (type) {
    case 'success':
      return CheckCircle
    case 'error':
      return XCircle
    case 'warning':
      return AlertTriangle
    case 'progress':
      return Loader2
    default:
      return Info
  }
}
</script>

<template>
  <div
    v-for="group in toastGroups"
    :key="group.key"
    class="toast-container"
    :class="group.className"
  >
    <TransitionGroup name="toast">
      <div
        v-for="toast in group.toasts"
        :key="toast.id"
        class="toast"
        :class="[`toast-${toast.type}`]"
      >
        <component
          :is="getIcon(toast.type)"
          :class="['toast-icon', { 'toast-icon--spin': toast.type === 'progress' }]"
        />
        <div class="toast-content">
          <div class="toast-header">
            <div class="toast-title">{{ toast.title }}</div>
            <div v-if="toast.meta" class="toast-meta">{{ toast.meta }}</div>
          </div>
          <div v-if="toast.message" class="toast-message">{{ toast.message }}</div>
        </div>
        <button class="toast-close" @click="uiStore.removeToast(toast.id)">
          <X class="w-4 h-4" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  right: 1.5rem;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-width: 400px;
}

.toast-container--top-right {
  top: 1.5rem;
}

.toast-container--bottom-right {
  bottom: 1.5rem;
  flex-direction: column-reverse;
}

.toast {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--rose-200);
}

.toast-icon {
  width: 1.25rem;
  height: 1.25rem;
  flex-shrink: 0;
}

.toast-success .toast-icon {
  color: var(--success);
}

.toast-error .toast-icon {
  color: var(--error);
}

.toast-warning .toast-icon {
  color: var(--warning);
}

.toast-info .toast-icon {
  color: var(--info);
}

.toast-progress .toast-icon {
  color: var(--rose-500);
}

.toast-content {
  flex: 1;
  min-width: 0;
}

.toast-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.75rem;
}

.toast-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--gray-900);
  flex: 1;
}

.toast-meta {
  font-size: 0.75rem;
  color: var(--gray-400);
  white-space: nowrap;
}

.toast-message {
  font-size: 0.75rem;
  color: var(--gray-500);
  margin-top: 0.25rem;
  white-space: pre-line;
}

.toast-close {
  background: none;
  border: none;
  padding: 0.25rem;
  cursor: pointer;
  color: var(--gray-400);
  border-radius: 0.25rem;
  transition: all var(--transition-fast);
}

.toast-close:hover {
  background: var(--gray-100);
  color: var(--gray-600);
}

.toast-icon--spin {
  animation: toast-spin 1s linear infinite;
}

@keyframes toast-spin {
  to {
    transform: rotate(360deg);
  }
}

/* Transition */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

.toast-move {
  transition: transform 0.3s ease;
}
</style>
