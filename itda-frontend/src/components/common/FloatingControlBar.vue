<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useCollabStore } from '../../stores/collab'
import CollabPanel from '../collab/CollabPanel.vue'
import { Mic, MicOff, MessageCircle, PhoneOff } from 'lucide-vue-next'

const collabStore = useCollabStore()
const floatRef = ref<HTMLElement | null>(null)
const panelWrapRef = ref<HTMLDivElement | null>(null)
const panelPlacement = ref<'up' | 'down'>('up')
const panelMaxHeight = ref<number | null>(null)
const panelOffsetX = ref(0)
const route = useRoute()
const DEFAULT_BOTTOM_OFFSET = 60

const isProjectPage = computed(() => {
  // Only show on project-related pages, never on dashboard
  return route.path.startsWith('/projects')
})

const isDashboardPage = computed(() => route.path === '/dashboard')

const isConnected = computed(() => collabStore.isConnected)
const statusText = computed(() => {
  if (isConnected.value) {
    return collabStore.isMuted ? 'Online (Mic Off)' : 'Online (Mic On)'
  }
  if (collabStore.status === 'connecting') return 'Connecting...'
  return 'Offline'
})

const statusColor = computed(() => {
  if (isConnected.value) return 'bg-green-500'
  if (collabStore.status === 'connecting') return 'bg-yellow-500'
  return 'bg-gray-500'
})

const STORAGE_KEY = 'collab:floatingPos'
const position = ref({ x: 0, y: 0 })
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

function toggleMute() {
  collabStore.toggleMute()
}

function togglePanel() {
  collabStore.togglePanel()
}

// Reverting to simple, working state
function clampPosition(nextX: number, nextY: number) {
  const rect = floatRef.value?.getBoundingClientRect()
  const width = rect?.width ?? 280
  const height = rect?.height ?? 64
  const maxX = Math.max(8, window.innerWidth - width - 8)
  const maxY = Math.max(8, window.innerHeight - height - 8)
  return {
    x: Math.min(Math.max(8, nextX), maxX),
    y: Math.min(Math.max(8, nextY), maxY),
  }
}

function setDefaultPosition() {
  /* Button is smaller than Bar. 
    If bar is visible -> fallback 280.
    If button -> fallback ~150.
  */
  const defaultWidth = collabStore.isFloatingBarVisible ? 280 : 150
  
  const rect = floatRef.value?.getBoundingClientRect()
  const measuredWidth = (rect?.width && rect.width > 0) ? rect.width : defaultWidth
  const width = collabStore.isFloatingBarVisible ? measuredWidth : defaultWidth
  const height = (rect?.height && rect.height > 0) ? rect.height : 64
  
  const rightOffset = 2
  const x = Math.max(8, window.innerWidth - width - rightOffset)
  const y = Math.max(8, window.innerHeight - height - DEFAULT_BOTTOM_OFFSET)
  position.value = { x, y }
}

function onPointerDown(event: PointerEvent) {
  const target = event.target as HTMLElement
  const isDragHandle = !!target.closest('.drag-handle') || !!target.closest('.floating-bar')
  if (!isDragHandle) return
  if (target.closest('button') || target.closest('input') || target.closest('textarea')) return

  if (!floatRef.value) return
  isDragging.value = true
  const rect = floatRef.value.getBoundingClientRect()
  dragOffset.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top,
  }
  floatRef.value.setPointerCapture(event.pointerId)
}

function onPointerMove(event: PointerEvent) {
  if (!isDragging.value) return
  const nextX = event.clientX - dragOffset.value.x
  const nextY = event.clientY - dragOffset.value.y
  position.value = clampPosition(nextX, nextY)
  if (collabStore.isPanelOpen) {
    updatePanelPlacement()
  }
}

function onPointerUp(event: PointerEvent) {
  if (!isDragging.value) return
  isDragging.value = false
  floatRef.value?.releasePointerCapture(event.pointerId)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(position.value))
  if (collabStore.isPanelOpen) {
    updatePanelPlacement()
  }
}

function handleResize() {
  if (!collabStore.isFloatingBarVisible) {
    setDefaultPosition()
  } else {
    position.value = clampPosition(position.value.x, position.value.y)
  }
  if (collabStore.isPanelOpen) {
    updatePanelPlacement()
  }
}

function updatePanelPlacement() {
  const barRect = floatRef.value?.getBoundingClientRect()
  const panelEl = panelWrapRef.value?.querySelector('.collab-panel') as HTMLElement | null
  if (!barRect || !panelEl) return

  const spaceAbove = barRect.top - 12
  const spaceBelow = window.innerHeight - barRect.bottom - 12
  const panelHeight = panelEl.offsetHeight || 0
  const panelWidth = panelEl.offsetWidth || 0

  if (panelHeight <= spaceAbove || spaceAbove >= spaceBelow) {
    panelPlacement.value = 'up'
    panelMaxHeight.value = Math.max(180, spaceAbove)
  } else {
    panelPlacement.value = 'down'
    panelMaxHeight.value = Math.max(180, spaceBelow)
  }

  // Keep panel horizontally within viewport
  const minLeft = 8
  const maxLeft = Math.max(8, window.innerWidth - panelWidth - 8)
  const desiredLeft = barRect.right - panelWidth
  const clampedLeft = Math.min(Math.max(desiredLeft, minLeft), maxLeft)
  panelOffsetX.value = clampedLeft - barRect.left
}

onMounted(() => {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    try {
      const parsed = JSON.parse(saved)
      if (typeof parsed?.x === 'number' && typeof parsed?.y === 'number') {
        position.value = clampPosition(parsed.x, parsed.y)
      } else {
        setDefaultPosition()
      }
    } catch {
      setDefaultPosition()
    }
  } else {
    setDefaultPosition()
  }

  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
  window.addEventListener('resize', handleResize)

  nextTick(() => {
    position.value = clampPosition(position.value.x, position.value.y)
    // Double check initial position just in case
    if (!collabStore.isFloatingBarVisible) {
      setDefaultPosition()
    }
  })
})

watch(
  () => collabStore.isPanelOpen,
  async (open) => {
    if (!open) return
    await nextTick()
    updatePanelPlacement()
  }
)

watch(
  () => collabStore.isFloatingBarVisible,
  async (visible) => {
    await nextTick()
    if (!visible) {
      setDefaultPosition()
      return
    }
    // Always reset to default position when opening the bar.
    setDefaultPosition()
    localStorage.setItem(STORAGE_KEY, JSON.stringify(position.value))
    if (collabStore.isPanelOpen) {
      updatePanelPlacement()
    }
  }
)

watch(
  () => isProjectPage.value,
  async (isProject) => {
    if (!isProject) return
    if (collabStore.isFloatingBarVisible) return
    await nextTick()
    setDefaultPosition()
  }
)

watch(
  () => collabStore.floatingBarResetToken,
  async () => {
    await nextTick()
    setDefaultPosition()
    localStorage.setItem(STORAGE_KEY, JSON.stringify(position.value))
    if (collabStore.isPanelOpen) {
      updatePanelPlacement()
    }
  }
)

watch(
  [() => collabStore.isMediaConnected, () => collabStore.status, () => collabStore.isFloatingBarVisible],
  async () => {
    if (!collabStore.isFloatingBarVisible) return
    await nextTick()
    position.value = clampPosition(position.value.x, position.value.y)
    if (collabStore.isPanelOpen) {
      updatePanelPlacement()
    }
  }
)

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div
    class="floating-wrap"
    ref="floatRef"
    :class="{ dragging: isDragging }"
    :style="{ left: `${position.x}px`, top: `${position.y}px` }"
    @pointerdown="onPointerDown"
  >
    <!-- Active Bar (Expanded State) -->
    <div v-if="!isDashboardPage && collabStore.isFloatingBarVisible && collabStore.isMediaConnected" class="bar-container">
    <div
      ref="panelWrapRef"
      class="panel-pop"
      :class="{ open: collabStore.isPanelOpen, down: panelPlacement === 'down' }"
      :style="{
        '--panel-max-height': panelMaxHeight ? `${panelMaxHeight}px` : undefined,
        '--panel-offset-x': `${panelOffsetX}px`,
      }"
    >
      <CollabPanel v-show="collabStore.isPanelOpen" />
    </div>
    <!-- Live Mode (Media Connected) -->
    <div class="floating-bar" :class="{ hidden: collabStore.isPanelOpen }">
      <!-- Status Indicator -->
      <div class="status-indicator" :title="statusText">
        <div class="status-dot" :class="statusColor"></div>
        <span class="status-text">Live</span>
      </div>

      <div class="divider"></div>

      <!-- Controls -->
      <button
        class="control-btn"
        :class="{ 'is-muted-active': collabStore.isMuted && isConnected }"
        @click="toggleMute"
        :disabled="!isConnected"
        :title="!isConnected ? 'Connect first' : (collabStore.isMuted ? 'Unmute' : 'Mute')"
      >
        <MicOff v-if="collabStore.isMuted || !isConnected" class="icon" />
        <Mic v-else class="icon" />
      </button>

      <button
        class="control-btn"
        :class="{ 'active': collabStore.isPanelOpen }"
        @click="togglePanel"
        title="Toggle Chat/Collab Panel"
      >
        <MessageCircle class="icon" />
      </button>

      <button
        class="control-btn danger"
        @click="collabStore.disableMedia(); collabStore.hideFloatingBar()"
        :disabled="!isConnected"
        title="End Call"
      >
        <PhoneOff class="icon" />
      </button>
    </div>
  </div>
  </div>
</template>

<style scoped>
.floating-wrap {
  position: fixed;
  z-index: 900;
  touch-action: none;
  display: inline-block;
  width: max-content;
}


.bar-container {
  position: relative;
  width: max-content;
  display: inline-flex;
}

.panel-pop {
  position: absolute;
  left: 0;
  bottom: 100%;
  overflow: visible;
  transform-origin: bottom right;
  opacity: 0;
  transform: translateX(var(--panel-offset-x, 0px)) translateY(6px) scale(0.96);
  pointer-events: none;
  transition: all 0.18s ease;
  margin-bottom: 0;
}


.panel-pop.open {
  opacity: 1;
  transform: translateX(var(--panel-offset-x, 0px)) translateY(0) scale(1);
  pointer-events: auto;
  margin-bottom: 0;
}

.panel-pop.down {
  bottom: auto;
  top: 100%;
  transform-origin: top right;
}

.panel-pop :deep(.collab-panel) {
  max-height: var(--panel-max-height, 90vh);
  overflow: auto;
}
.floating-bar {
  background: white;
  border-radius: 999px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  padding: 0.5rem 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  border: 1px solid var(--rose-200);
  width: fit-content;
  cursor: grab;
}

.floating-wrap.dragging .floating-bar {
  cursor: grabbing;
}

.floating-bar button,
.floating-bar .control-btn {
  cursor: pointer;
}

.floating-bar.hidden {
  opacity: 0;
  pointer-events: none;
}

.status-indicator {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.75rem;
    font-weight: 600;
    color: var(--gray-700);
}

.status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
}

.divider {
    width: 1px;
    height: 20px;
    background-color: var(--rose-200);
    margin: 0 0.25rem;
}

.control-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--gray-500);
  cursor: pointer;
  transition: all 0.2s;
}

.control-btn:hover {
  background: var(--rose-50);
  color: var(--rose-500);
}

.control-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: var(--gray-100);
}

.control-btn.is-muted-active {
  background: var(--rose-500);
  color: white;
}

.control-btn.is-muted-active:hover {
  background: var(--rose-600);
}

.control-btn.danger {
  color: var(--error);
}

.control-btn.danger:hover {
  background: var(--error-bg);
  color: var(--error);
}

.icon {
  width: 14px;
  height: 14px;
}


.icon-sm {
  width: 14px;
  height: 14px;
}


/* Tailwind-like utilities since we might not have full tailwind configured as classes yet */
.bg-green-500 { background-color: #22c55e; }
.bg-yellow-500 { background-color: #eab308; }
.bg-gray-500 { background-color: #6b7280; }
</style>
