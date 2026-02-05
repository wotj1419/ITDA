import { onBeforeUnmount, ref } from 'vue'

interface HoverPreviewPolicyOptions {
  delayMs?: number
}

export function useHoverPreviewPolicy(options: HoverPreviewPolicyOptions = {}) {
  const delayMs = options.delayMs ?? 150
  const activePreviewId = ref<string | null>(null)
  const pendingPreviewId = ref<string | null>(null)
  const prefersReducedMotion = ref(false)
  let hoverTimer: ReturnType<typeof setTimeout> | null = null
  let mediaQuery: MediaQueryList | null = null
  let mediaListener: ((event: MediaQueryListEvent) => void) | null = null

  function cancelTimer(): void {
    if (hoverTimer === null) return
    clearTimeout(hoverTimer)
    hoverTimer = null
  }

  function clearPreview(): void {
    cancelTimer()
    pendingPreviewId.value = null
    activePreviewId.value = null
  }

  function isPreviewing(id: string): boolean {
    return activePreviewId.value === id
  }

  function startPreview(id: string): void {
    if (!id || prefersReducedMotion.value) return

    cancelTimer()
    pendingPreviewId.value = id

    if (activePreviewId.value !== id) {
      activePreviewId.value = null
    }

    hoverTimer = setTimeout(() => {
      activePreviewId.value = id
      pendingPreviewId.value = null
      hoverTimer = null
    }, delayMs)
  }

  function stopPreview(id: string): void {
    if (!id) return
    if (pendingPreviewId.value === id) {
      pendingPreviewId.value = null
      cancelTimer()
    }
    if (activePreviewId.value === id) {
      activePreviewId.value = null
    }
  }

  if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
    mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    prefersReducedMotion.value = mediaQuery.matches
    mediaListener = (event: MediaQueryListEvent) => {
      prefersReducedMotion.value = event.matches
      if (event.matches) {
        clearPreview()
      }
    }
    mediaQuery.addEventListener('change', mediaListener)
  }

  onBeforeUnmount(() => {
    clearPreview()
    if (mediaQuery && mediaListener) {
      mediaQuery.removeEventListener('change', mediaListener)
    }
  })

  return {
    activePreviewId,
    clearPreview,
    isPreviewing,
    startPreview,
    stopPreview,
  }
}
