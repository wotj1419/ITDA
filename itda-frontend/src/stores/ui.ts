import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Toast } from '../types'

export const useUIStore = defineStore('ui', () => {
  // State
  const sidebarExpanded = ref(true)
  const sidebarPinned = ref(false) // Disabled - always allow auto-collapse
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

  // For peek mode: temporarily expand without pinning
  function peekSidebar(): void {
    if (!sidebarPinned.value) {
      sidebarExpanded.value = true
    }
  }

  // For peek mode: collapse if not pinned
  function unpeekSidebar(): void {
    if (!sidebarPinned.value) {
      sidebarExpanded.value = false
    }
  }

  function showToast(toast: Omit<Toast, 'id'>): string {
    const id = `toast_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
    const newToast: Toast = {
      id,
      duration: 4000,
      position: 'top-right',
      autoClose: true,
      ...toast,
    }

    toasts.value.push(newToast)

    if (newToast.autoClose !== false) {
      const duration = typeof newToast.duration === 'number' ? newToast.duration : 4000
      setTimeout(() => {
        removeToast(id)
      }, duration)
    }

    return id
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
    sidebarPinned,
    toasts,
    activeModal,
    modalData,
    toggleSidebar,
    setSidebarExpanded,
    peekSidebar,
    unpeekSidebar,
    showToast,
    removeToast,
    openModal,
    closeModal,
  }
})
