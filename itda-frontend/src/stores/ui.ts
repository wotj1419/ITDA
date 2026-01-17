import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Toast } from '../types'

export const useUIStore = defineStore('ui', () => {
  // State
  const sidebarExpanded = ref(true)
  const toasts = ref<Toast[]>([])
  const activeModal = ref<string | null>(null)
  const modalData = ref<unknown>(null)

  // Actions
  function toggleSidebar(): void {
    sidebarExpanded.value = !sidebarExpanded.value
  }

  function setSidebarExpanded(expanded: boolean): void {
    sidebarExpanded.value = expanded
  }

  function showToast(toast: Omit<Toast, 'id'>): void {
    const id = 'toast_' + Date.now()
    const newToast: Toast = {
      id,
      duration: 4000,
      ...toast,
    }

    toasts.value.push(newToast)

    // Auto remove
    setTimeout(() => {
      removeToast(id)
    }, newToast.duration)
  }

  function removeToast(id: string): void {
    const index = toasts.value.findIndex((t) => t.id === id)
    if (index > -1) {
      toasts.value.splice(index, 1)
    }
  }

  function openModal(modalId: string, data?: unknown): void {
    activeModal.value = modalId
    modalData.value = data
  }

  function closeModal(): void {
    activeModal.value = null
    modalData.value = null
  }

  return {
    sidebarExpanded,
    toasts,
    activeModal,
    modalData,
    toggleSidebar,
    setSidebarExpanded,
    showToast,
    removeToast,
    openModal,
    closeModal,
  }
})
