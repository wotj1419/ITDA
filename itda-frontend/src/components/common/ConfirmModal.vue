<script setup lang="ts">
import { AlertTriangle } from 'lucide-vue-next'

interface Props {
  isOpen: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  isDangerous?: boolean
}

withDefaults(defineProps<Props>(), {
  confirmText: '확인',
  cancelText: '취소',
  isDangerous: false,
})

const emit = defineEmits<{
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()
</script>

<template>
  <teleport to="body">
    <div v-if="isOpen" class="modal-overlay" @click.self="emit('cancel')">
      <div class="modal-content">
        <div class="modal-icon-wrapper" :class="{ dangerous: isDangerous }">
          <AlertTriangle class="modal-icon" />
        </div>
        
        <h3 class="modal-title">{{ title }}</h3>
        <p class="modal-message">{{ message }}</p>

        <div class="modal-actions">
          <button class="btn btn-secondary" @click="emit('cancel')">
            {{ cancelText }}
          </button>
          <button 
            class="btn" 
            :class="isDangerous ? 'btn-danger' : 'btn-primary'"
            @click="emit('confirm')"
          >
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease-out;
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  width: 100%;
  max-width: 500px;
  text-align: center;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  animation: scaleIn 0.2s ease-out;
}

.modal-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--gray-100);
  color: var(--gray-600);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
}

.modal-icon-wrapper.dangerous {
  background: var(--red-50);
  color: var(--red-500);
}

.modal-icon {
  width: 24px;
  height: 24px;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--gray-900);
  margin-bottom: 0.5rem;
}

.modal-message {
  color: var(--gray-500);
  margin-bottom: 2rem;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

/* Button Styles */
.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  font-size: 0.875rem;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

.btn-primary {
  background: #f43f5e;
  color: white;
}

.btn-primary:hover {
  background: #e11d48;
}

.btn-danger {
  background: #ef4444;
  color: white;
}

.btn-danger:hover {
  background: #dc2626;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from { transform: scale(0.95); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>
