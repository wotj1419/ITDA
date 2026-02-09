<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch, type ComponentPublicInstance } from 'vue'
import { useCollabStore } from '../../stores/collab'
import type { CollabParticipant } from '../../types/ui/collab'
import { ChevronDown, ChevronUp, MessageCircle, Mic, MicOff, Phone, PhoneOff, Search, Send, X } from 'lucide-vue-next'
import Avatar from '../common/Avatar.vue'
import { resolveApiUrl } from '../../services/api/urls'

interface Props {
  projectId?: number | null
  expanded?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  projectId: null,
  expanded: false,
})

const collabStore = useCollabStore()
const messageInput = ref('')
const chatMessagesRef = ref<HTMLElement | null>(null)
const menuRef = ref<HTMLElement | null>(null)
const chatButtonRef = ref<HTMLElement | null>(null)
const panelRef = ref<HTMLElement | null>(null)
const callPanelRef = ref<HTMLElement | null>(null)
const searchInputRef = ref<HTMLInputElement | null>(null)
const isChatOpen = ref(false)
const isChatDimmed = ref(false)
const panelPosition = ref({ x: 0, y: 0 })
const dragOffset = ref({ x: 0, y: 0 })
const isDragging = ref(false)
const callDragOffset = ref({ x: 0, y: 0 })
const isCallDragging = ref(false)
const callPanelObserver = ref<ResizeObserver | null>(null)
let observedCallPanel: HTMLElement | null = null
const lastDragEndAt = ref(0)
const hasCustomPosition = ref(false)
const isSearchOpen = ref(false)
const searchTerm = ref('')
const searchIndex = ref(0)
const messageRefs = new Map<string, HTMLElement>()
const searchBasePosition = ref<{ x: number; y: number } | null>(null)
const searchShifted = ref(false)
const isSearchActive = ref(false)

const PANEL_PADDING = 8
const PANEL_OFFSET = 12
const CALL_PANEL_PADDING = 8
const CALL_PANEL_OFFSET = 8
const SIDEBAR_GUTTER = 12
const SIDEBAR_EXPANDED_WIDTH = 260
const SIDEBAR_COLLAPSED_WIDTH = 72

const isSidebarShifting = ref(false)
const sidebarShiftMs = ref(300)
let sidebarShiftTimer: number | null = null

const isConnected = computed(() => collabStore.isConnected)
const statusText = computed(() => {
  if (collabStore.status === 'connecting') return '연결 중'
  if (collabStore.status === 'connected') return '온라인'
  if (collabStore.status === 'error') return '오류'
  return '오프라인'
})

const isCallConnecting = computed(() => collabStore.isCallConnecting && !collabStore.isMediaConnected)
const isCallOpen = computed(() => collabStore.isCallPanelOpen)
const isCallCollapsed = computed(() => collabStore.isCallPanelCollapsed)
const canEndCall = computed(() => collabStore.isMediaConnected || collabStore.isCallConnecting)
const chatDisplayPosition = computed(() => {
  void props.expanded
  return clampPanelPosition(panelPosition.value.x, panelPosition.value.y)
})
const callDisplayPosition = computed(() => {
  void props.expanded
  return clampCallPanelPosition(collabStore.callPanelPosition.x, collabStore.callPanelPosition.y)
})

type CallParticipant = CollabParticipant & { isMe?: boolean }

const callParticipants = computed<CallParticipant[]>(() => {
  if (!collabStore.isMediaConnected && !collabStore.isCallConnecting) return []
  const ids = new Set(collabStore.rtcPeerIds)
  const list: CallParticipant[] = []
  const local = collabStore.localParticipant
  if (local?.odps) {
    list.push({ ...local, isMe: true })
  }
  collabStore.participants.forEach((p) => {
    if (ids.has(p.odps)) {
      list.push({ ...p, isMe: false })
    }
  })
  ids.forEach((peerId) => {
    if (!list.some((p) => p.odps === peerId)) {
      list.push({ odps: peerId, name: 'Guest', isMe: false })
    }
  })
  return list
})

function ensureJoined() {
  const pid = Number(props.projectId)
  if (!Number.isFinite(pid)) return
  if (collabStore.status === 'disconnected') {
    void collabStore.joinRoom(pid)
  }
}

function toggleCallList() {
  if (collabStore.isCallPanelOpen) {
    return
  }
  const pid = Number(props.projectId)
  if (!Number.isFinite(pid)) return
  if (!collabStore.isMediaConnected && !collabStore.isCallConnecting) {
    collabStore.startCall(pid)
  }
  collabStore.hasCallPanelCustomPosition = false
  collabStore.isCallPanelCollapsed = false
  collabStore.isCallPanelDimmed = false
  positionCallPanelFromButton()
  collabStore.isCallPanelOpen = true
  void nextTick().then(() => {
    positionCallPanelFromButton()
  })
}

function toggleCallCollapse() {
  collabStore.isCallPanelCollapsed = !collabStore.isCallPanelCollapsed
}

async function toggleChat() {
  if (!isChatOpen.value) {
    ensureJoined()
  }
  isChatOpen.value = !isChatOpen.value
  if (isChatOpen.value) {
    isChatDimmed.value = false
    await nextTick()
    positionPanelFromButton()
    collabStore.markChatRead()
    if (chatMessagesRef.value) {
      chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
    }
  }
}

function closeChat() {
  isChatOpen.value = false
  isChatDimmed.value = false
  clearSearch()
  if (isDragging.value) {
    endDrag()
  }
}

async function handleSendMessage() {
  const content = messageInput.value.trim()
  if (!content) return
  if (!isConnected.value) return
  collabStore.sendMessage(content)
  messageInput.value = ''
  await nextTick()
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
  }
}

function formatTime(timestamp: number): string {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

function normalize(text: string): string {
  return text.trim().toLowerCase()
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function getHighlightedHtml(content: string): string {
  const raw = content ?? ''
  const term = searchTerm.value.trim()
  if (!isSearchOpen.value || !isSearchActive.value || !term) return escapeHtml(raw)
  const regex = new RegExp(escapeRegExp(term), 'gi')
  let result = ''
  let lastIndex = 0
  let match: RegExpExecArray | null
  while ((match = regex.exec(raw))) {
    const start = match.index ?? 0
    const end = start + match[0].length
    result += escapeHtml(raw.slice(lastIndex, start))
    result += `<span class="chat-panel__highlight">${escapeHtml(raw.slice(start, end))}</span>`
    lastIndex = end
  }
  result += escapeHtml(raw.slice(lastIndex))
  return result
}

const searchMatches = computed(() => {
  const term = normalize(searchTerm.value)
  if (!term) return []
  const matches = []
  for (let i = collabStore.messages.length - 1; i >= 0; i -= 1) {
    const message = collabStore.messages[i]
    if (normalize(message?.content ?? '').includes(term)) {
      matches.push(message)
    }
  }
  return matches
})

const currentMatchId = computed(() => {
  if (!isSearchActive.value || !searchMatches.value.length) return ''
  const idx = Math.min(searchIndex.value, searchMatches.value.length - 1)
  return searchMatches.value[idx]?.messageId ?? ''
})

function getAvatarUrl(senderId: string | undefined) {
  if (!senderId) return '';
  const participant = collabStore.participants.find((p) => p.odps === senderId);
  return resolveApiUrl(participant?.avatarUrl) ?? '';
}

function isParticipantSpeaking(id: string | undefined) {
  if (!id) return false
  return collabStore.isSpeaking(id)
}

function getSidebarRight(): number {
  const sidebar = document.querySelector<HTMLElement>('.sidebar')
  if (!sidebar) return 0
  const rect = sidebar.getBoundingClientRect()
  const isCollapsed = sidebar.classList.contains('sidebar-collapsed')
  const width = isCollapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH
  return rect.left + width
}

function getHeaderBottom(): number {
  const header =
    document.querySelector<HTMLElement>('.header') ||
    document.querySelector<HTMLElement>('.editor-header')
  if (!header) return 16
  return header.getBoundingClientRect().bottom
}

function parseDurationMs(value: string): number {
  const trimmed = value.trim()
  if (!trimmed) return 0
  if (trimmed.endsWith('ms')) {
    const ms = Number(trimmed.replace('ms', '').trim())
    return Number.isFinite(ms) ? ms : 0
  }
  if (trimmed.endsWith('s')) {
    const seconds = Number(trimmed.replace('s', '').trim())
    return Number.isFinite(seconds) ? seconds * 1000 : 0
  }
  const raw = Number(trimmed)
  return Number.isFinite(raw) ? raw : 0
}

function getSidebarTransitionMs(): number {
  const sidebar = document.querySelector<HTMLElement>('.sidebar')
  if (!sidebar) return 300
  const durations = getComputedStyle(sidebar).transitionDuration
    .split(',')
    .map((part) => parseDurationMs(part))
    .filter((value) => value > 0)
  if (!durations.length) return 300
  return Math.max(...durations)
}

function startSidebarShift() {
  if (sidebarShiftTimer) {
    window.clearTimeout(sidebarShiftTimer)
    sidebarShiftTimer = null
  }
  sidebarShiftMs.value = getSidebarTransitionMs()
  isSidebarShifting.value = true
  sidebarShiftTimer = window.setTimeout(() => {
    isSidebarShifting.value = false
    sidebarShiftTimer = null
  }, sidebarShiftMs.value)
}

watch(
  () => collabStore.messages.length,
  async () => {
    await nextTick()
    if (isChatOpen.value) {
      collabStore.markChatRead()
    }
    if (searchMatches.value.length && currentMatchId.value) {
      scrollToMatch(currentMatchId.value)
    }
    if (chatMessagesRef.value) {
      chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
    }
  }
)

watch(
  () => props.expanded,
  async () => {
    if (!isChatOpen.value) return
    startSidebarShift()
    await nextTick()
    panelPosition.value = clampPanelPosition(panelPosition.value.x, panelPosition.value.y, false)
  }
)

watch(
  () => props.expanded,
  async () => {
    if (!isCallOpen.value) return
    startSidebarShift()
    if (collabStore.hasCallPanelCustomPosition) {
      clampCallPanelToViewport()
      return
    }
    await nextTick()
    positionCallPanelFromButton()
  }
)

watch(
  [() => collabStore.isMediaConnected, () => collabStore.isCallConnecting],
  ([mediaConnected, callConnecting]) => {
    if (!mediaConnected && !callConnecting) {
      collabStore.isCallPanelOpen = false
    }
  }
)

watch(
  () => isCallOpen.value,
  async (open) => {
    if (!open) return
    await nextTick()
    if (collabStore.hasCallPanelCustomPosition) {
      clampCallPanelToViewport()
      return
    }
    positionCallPanelFromButton()
  }
  ,
  { immediate: true }
)

watch(
  () => callPanelRef.value,
  async () => {
    observeCallPanel()
    if (!isCallOpen.value) return
    await nextTick()
    clampCallPanelToViewport()
  }
)

watch(
  () => [callParticipants.value.length, isCallCollapsed.value, isCallConnecting.value],
  async () => {
    if (!isCallOpen.value) return
    await nextTick()
    clampCallPanelToViewport()
  }
)

function getPanelSize() {
  const rect = panelRef.value?.getBoundingClientRect()
  return {
    width: rect?.width ?? 340,
    height: rect?.height ?? 420,
  }
}

function clampPanelPosition(nextX: number, nextY: number, respectSidebar = true) {
  const { width, height } = getPanelSize()
  const sidebarRight = getSidebarRight()
  const rawMinX = Math.max(PANEL_PADDING, sidebarRight + SIDEBAR_GUTTER)
  const maxX = Math.max(PANEL_PADDING, window.innerWidth - width - PANEL_PADDING)
  const minX = respectSidebar ? Math.min(rawMinX, maxX) : Math.min(PANEL_PADDING, maxX)
  const maxY = Math.max(PANEL_PADDING, window.innerHeight - height - PANEL_PADDING)
  return {
    x: Math.min(Math.max(minX, nextX), maxX),
    y: Math.min(Math.max(PANEL_PADDING, nextY), maxY),
  }
}

function positionPanelFromButton() {
  const anchor = chatButtonRef.value?.getBoundingClientRect()
  if (!anchor) return
  const { height } = getPanelSize()
  const nextX = anchor.right + PANEL_OFFSET
  const nextY = anchor.top - height - PANEL_OFFSET + 40
  panelPosition.value = clampPanelPosition(nextX, nextY, false)
  hasCustomPosition.value = false
}

function getCallPanelSize() {
  const rect = callPanelRef.value?.getBoundingClientRect()
  return {
    width: rect?.width ?? 240,
    height: rect?.height ?? 200,
  }
}

function clampCallPanelPosition(nextX: number, nextY: number, respectSidebar = true) {
  const { width, height } = getCallPanelSize()
  const sidebarRight = getSidebarRight()
  const rawMinX = Math.max(CALL_PANEL_PADDING, sidebarRight + SIDEBAR_GUTTER)
  const maxX = Math.max(CALL_PANEL_PADDING, window.innerWidth - width - CALL_PANEL_PADDING)
  const minX = respectSidebar ? Math.min(rawMinX, maxX) : Math.min(CALL_PANEL_PADDING, maxX)
  const maxY = Math.max(CALL_PANEL_PADDING, window.innerHeight - height - CALL_PANEL_PADDING)
  return {
    x: Math.min(Math.max(minX, nextX), maxX),
    y: Math.min(Math.max(CALL_PANEL_PADDING, nextY), maxY),
  }
}

function clampCallPanelToViewport() {
  collabStore.callPanelPosition = clampCallPanelPosition(
    collabStore.callPanelPosition.x,
    collabStore.callPanelPosition.y,
    false
  )
}

function observeCallPanel() {
  const observer = callPanelObserver.value
  if (!observer) return
  if (observedCallPanel && observedCallPanel !== callPanelRef.value) {
    observer.unobserve(observedCallPanel)
  }
  if (callPanelRef.value && observedCallPanel !== callPanelRef.value) {
    observer.observe(callPanelRef.value)
    observedCallPanel = callPanelRef.value
  }
}

function positionCallPanelFromButton() {
  const sidebarRight = getSidebarRight()
  const headerBottom = getHeaderBottom()
  const nextX = sidebarRight + SIDEBAR_GUTTER
  const nextY = headerBottom + CALL_PANEL_OFFSET
  collabStore.callPanelPosition = clampCallPanelPosition(nextX, nextY, false)
}

function startCallDrag(event: PointerEvent) {
  if (!callPanelRef.value) return
  if (event.button !== 0) return
  const target = event.target as HTMLElement | null
  collabStore.isCallPanelDimmed = false
  if (target?.closest('button') || target?.closest('input') || target?.closest('textarea')) return
  isCallDragging.value = true
  collabStore.hasCallPanelCustomPosition = true
  const rect = callPanelRef.value.getBoundingClientRect()
  callDragOffset.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top,
  }
  event.preventDefault()
  window.addEventListener('pointermove', handleCallDragMove)
  window.addEventListener('pointerup', endCallDrag)
}

function handleCallDragMove(event: PointerEvent) {
  if (!isCallDragging.value) return
  const nextX = event.clientX - callDragOffset.value.x
  const nextY = event.clientY - callDragOffset.value.y
  collabStore.callPanelPosition = clampCallPanelPosition(nextX, nextY, false)
}

function endCallDrag() {
  if (!isCallDragging.value) return
  isCallDragging.value = false
  lastDragEndAt.value = Date.now()
  window.removeEventListener('pointermove', handleCallDragMove)
  window.removeEventListener('pointerup', endCallDrag)
}

function handleCallMuteToggle() {
  if (!collabStore.isMediaConnected) return
  collabStore.isCallPanelDimmed = false
  collabStore.toggleMute()
}

function handleCallEnd() {
  if (!canEndCall.value) return
  collabStore.isCallPanelDimmed = false
  collabStore.disableMedia()
  collabStore.isCallPanelOpen = false
}

function startDrag(event: PointerEvent) {
  if (!panelRef.value) return
  isDragging.value = true
  hasCustomPosition.value = true
  const rect = panelRef.value.getBoundingClientRect()
  dragOffset.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top,
  }
  event.preventDefault()
  window.addEventListener('pointermove', handleDragMove)
  window.addEventListener('pointerup', endDrag)
}

function handleDragMove(event: PointerEvent) {
  if (!isDragging.value) return
  const nextX = event.clientX - dragOffset.value.x
  const nextY = event.clientY - dragOffset.value.y
  panelPosition.value = clampPanelPosition(nextX, nextY, false)
}

function endDrag() {
  if (!isDragging.value) return
  isDragging.value = false
  lastDragEndAt.value = Date.now()
  window.removeEventListener('pointermove', handleDragMove)
  window.removeEventListener('pointerup', endDrag)
}

function handleResize() {
  if (!isChatOpen.value) return
  if (hasCustomPosition.value) {
    panelPosition.value = clampPanelPosition(panelPosition.value.x, panelPosition.value.y, false)
    return
  }
  positionPanelFromButton()
}

function handleCallResize() {
  if (!isCallOpen.value) return
  if (collabStore.hasCallPanelCustomPosition) {
    collabStore.callPanelPosition = clampCallPanelPosition(
      collabStore.callPanelPosition.x,
      collabStore.callPanelPosition.y,
      false
    )
    return
  }
  positionCallPanelFromButton()
}

function handleOutsidePointerDown(event: PointerEvent) {
  if (isDragging.value || isCallDragging.value) return
  const target = event.target as Node | null
  if (!menuRef.value || !target) return
  if (!menuRef.value.contains(target)) {
    if (collabStore.isCallPanelOpen) {
      collabStore.isCallPanelDimmed = true
    }
    if (isChatOpen.value) {
      isChatDimmed.value = true
    }
    return
  }
  collabStore.isCallPanelDimmed = false
  isChatDimmed.value = false
}

onMounted(() => {
  window.addEventListener('pointerdown', handleOutsidePointerDown, true)
  window.addEventListener('resize', handleResize)
  window.addEventListener('resize', handleCallResize)
  if (typeof ResizeObserver !== 'undefined') {
    callPanelObserver.value = new ResizeObserver(() => {
      if (!isCallOpen.value) return
      clampCallPanelToViewport()
    })
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('pointerdown', handleOutsidePointerDown, true)
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('resize', handleCallResize)
  window.removeEventListener('pointermove', handleDragMove)
  window.removeEventListener('pointerup', endDrag)
  window.removeEventListener('pointermove', handleCallDragMove)
  window.removeEventListener('pointerup', endCallDrag)
  if (sidebarShiftTimer) {
    window.clearTimeout(sidebarShiftTimer)
    sidebarShiftTimer = null
  }
  if (observedCallPanel && callPanelObserver.value) {
    callPanelObserver.value.unobserve(observedCallPanel)
  }
  callPanelObserver.value?.disconnect()
})

function setMessageRef(el: Element | ComponentPublicInstance | null, id: string) {
  if (!el) {
    messageRefs.delete(id)
    return
  }
  messageRefs.set(id, el as HTMLElement)
}

function scrollToMatch(id: string) {
  const el = messageRefs.get(id)
  if (!el) return
  el.scrollIntoView({ block: 'center', behavior: 'smooth' })
}

function handleSearchEnter(event: KeyboardEvent) {
  if ((event as KeyboardEvent & { isComposing?: boolean }).isComposing) return
  if ((event as KeyboardEvent & { keyCode?: number }).keyCode === 229) return
  if (!searchTerm.value.trim()) return
  if (!isSearchActive.value) {
    isSearchActive.value = true
    if (!searchMatches.value.length) return
    scrollToMatch(currentMatchId.value)
    return
  }
  void jumpToNextMatch()
}

async function toggleSearch() {
  if (isSearchOpen.value) {
    closeSearch()
    return
  }

  const prevHeight = getPanelSize().height
  searchBasePosition.value = { ...panelPosition.value }
  searchShifted.value = false
  isSearchOpen.value = true
  await nextTick()
  if (isSearchOpen.value) {
    searchIndex.value = 0
    isSearchActive.value = false
    searchInputRef.value?.focus()
    searchInputRef.value?.select()
    if (searchMatches.value.length) {
      scrollToMatch(currentMatchId.value)
    }
    const newHeight = getPanelSize().height
    const deltaHeight = newHeight - prevHeight
    const rect = panelRef.value?.getBoundingClientRect()
    if (rect && deltaHeight > 0) {
      const overflowBottom = rect.bottom - (window.innerHeight - PANEL_PADDING)
      if (overflowBottom > 0) {
        panelPosition.value = clampPanelPosition(panelPosition.value.x, panelPosition.value.y - overflowBottom, false)
        searchShifted.value = true
      }
    }
    if (!searchShifted.value) {
      searchBasePosition.value = null
    }
    return
  }
}

async function jumpToNextMatch() {
  if (!isSearchActive.value) {
    isSearchActive.value = true
    if (!searchMatches.value.length) return
    await nextTick()
    scrollToMatch(currentMatchId.value)
    return
  }
  if (!searchMatches.value.length) return
  searchIndex.value = (searchIndex.value + 1) % searchMatches.value.length
  await nextTick()
  scrollToMatch(currentMatchId.value)
}

async function jumpToPrevMatch() {
  if (!isSearchActive.value) {
    isSearchActive.value = true
    if (!searchMatches.value.length) return
    await nextTick()
    scrollToMatch(currentMatchId.value)
    return
  }
  if (!searchMatches.value.length) return
  searchIndex.value = (searchIndex.value - 1 + searchMatches.value.length) % searchMatches.value.length
  await nextTick()
  scrollToMatch(currentMatchId.value)
}

function closeSearch() {
  if (!isSearchOpen.value) return
  isSearchOpen.value = false
  if (searchShifted.value && searchBasePosition.value && !hasCustomPosition.value) {
    panelPosition.value = clampPanelPosition(searchBasePosition.value.x, searchBasePosition.value.y, false)
  }
  searchBasePosition.value = null
  searchShifted.value = false
}

function resetSearchState() {
  searchTerm.value = ''
  searchIndex.value = 0
  isSearchActive.value = false
}

function clearSearch() {
  resetSearchState()
  closeSearch()
}

watch(
  () => searchTerm.value,
  async () => {
    searchIndex.value = 0
    isSearchActive.value = false
    if (!searchTerm.value.trim()) return
    if (!searchMatches.value.length) return
  }
)
</script>

<template>
  <div
    ref="menuRef"
    class="sidebar-collab-actions"
    :class="{ expanded: props.expanded }"
    aria-label="사이드바 통화/채팅"
  >
    <div class="collab-expanded-shell" :class="{ open: props.expanded }">
      <div class="collab-expanded">
        <div class="collab-expanded__title">협업 시작</div>
        <div class="collab-expanded__actions">
          <div class="action-popover-wrap">
            <button class="collab-action" type="button" @click.stop="toggleCallList">
              <span class="action-icon action-icon--call">
                <Phone class="icon" />
              </span>
              <span class="collab-action__label">통화</span>
            </button>
          </div>

          <div class="action-popover-wrap">
            <button
              ref="chatButtonRef"
              class="collab-action"
              type="button"
              @click.stop="toggleChat"
            >
              <span class="action-icon action-icon--chat">
                <MessageCircle class="icon" />
                <span v-if="collabStore.hasUnreadMessages" class="unread-dot"></span>
              </span>
              <span class="collab-action__label">채팅</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="collab-collapsed-shell" :class="{ open: !props.expanded }">
      <div class="action-popover-wrap">
        <button
          class="action-button"
          type="button"
          title="통화"
          aria-label="통화"
          @click.stop="toggleCallList"
        >
          <span class="action-icon action-icon--call">
            <Phone class="icon" />
          </span>
        </button>
      </div>

      <div class="action-popover-wrap">
        <button
          ref="chatButtonRef"
          class="action-button"
          type="button"
          title="채팅"
          aria-label="채팅"
          @click.stop="toggleChat"
        >
          <span class="action-icon action-icon--chat">
            <MessageCircle class="icon" />
            <span v-if="collabStore.hasUnreadMessages" class="unread-dot"></span>
          </span>
        </button>
      </div>
    </div>

    <div
      v-if="isCallOpen"
      ref="callPanelRef"
      class="call-popover"
      :class="{
        dragging: isCallDragging,
        dimmed: collabStore.isCallPanelDimmed,
        shifting: isSidebarShifting,
      }"
      :style="{
        left: `${callDisplayPosition.x}px`,
        top: `${callDisplayPosition.y}px`,
        '--sidebar-shift-ms': `${sidebarShiftMs}ms`,
      }"
      @pointerdown="startCallDrag"
    >
      <div class="call-panel">
        <div class="call-panel__header">
          <div class="call-panel__title">
            <Phone class="icon-sm" />
            <span>통화 참여자</span>
          </div>
          <button
            class="call-panel__collapse"
            type="button"
            @click.stop="toggleCallCollapse"
            aria-label="접기/펼치기"
          >
            <ChevronUp v-if="!isCallCollapsed" class="icon-xs" />
            <ChevronDown v-else class="icon-xs" />
          </button>
        </div>
        <template v-if="!isCallCollapsed">
          <div v-if="isCallConnecting" class="call-panel__status">통화 연결 중…</div>
          <ul v-else class="call-panel__list">
            <li v-for="member in callParticipants" :key="member.odps" class="call-panel__item">
              <span
                class="call-avatar"
                :class="{ speaking: isParticipantSpeaking(member.odps) }"
              >
                <Avatar
                  class="call-avatar__image"
                  :src="member.avatarUrl || ''"
                  :alt="member.name || 'Guest'"
                  :user-id="parseInt(member.odps, 10) || 0"
                  size="sm"
                />
                <span v-if="member.isMuted" class="call-avatar__mute">
                  <MicOff class="icon-xs" />
                </span>
              </span>
              <span class="call-name">
                {{ member.name || 'Guest' }}
                <span v-if="member.isMe" class="call-me">(me)</span>
              </span>
            </li>
          </ul>
          <div class="call-panel__controls">
            <button
              class="call-control-btn"
              :class="{ active: collabStore.isMuted }"
              type="button"
              :disabled="!collabStore.isMediaConnected"
              @click.stop="handleCallMuteToggle"
            >
              <MicOff v-if="collabStore.isMuted" class="icon-xs" />
              <Mic v-else class="icon-xs" />
              <span>{{ collabStore.isMuted ? '음소거 해제' : '음소거' }}</span>
            </button>
            <button
              class="call-control-btn danger"
              type="button"
              :disabled="!canEndCall"
              @click.stop="handleCallEnd"
            >
              <PhoneOff class="icon-xs" />
              <span>통화 종료</span>
            </button>
          </div>
        </template>
      </div>
    </div>

    <div
      v-if="isChatOpen"
      ref="panelRef"
      class="action-popover"
      :class="{ dragging: isDragging, dimmed: isChatDimmed, shifting: isSidebarShifting }"
      :style="{
        left: `${chatDisplayPosition.x}px`,
        top: `${chatDisplayPosition.y}px`,
        '--sidebar-shift-ms': `${sidebarShiftMs}ms`,
      }"
      @pointerdown="isChatDimmed = false"
    >
      <div class="chat-panel">
        <div class="chat-panel__header drag-handle" @pointerdown="startDrag">
          <div class="chat-panel__title">
            <MessageCircle class="icon" />
            <span>채팅</span>
            <span class="chat-panel__status" :class="{ online: isConnected }">
              {{ statusText }}
            </span>
          </div>
        <div class="chat-panel__actions">
          <button class="chat-panel__search" type="button" @click="toggleSearch" aria-label="검색">
            <Search class="icon-sm" />
          </button>
          <button class="chat-panel__close" type="button" @click="closeChat" aria-label="닫기">
            <X class="icon-sm" />
          </button>
        </div>
      </div>

      <div v-if="isSearchOpen" class="chat-panel__search-row">
        <input
          ref="searchInputRef"
          v-model="searchTerm"
          class="chat-panel__search-input"
          type="text"
          placeholder="대화 내용 검색..."
          @keydown.enter.prevent="handleSearchEnter"
        />
        <button
          class="chat-panel__search-nav"
          type="button"
          @click="jumpToNextMatch"
          :disabled="!isSearchActive || !searchMatches.length"
          aria-label="위로 탐색"
        >
          <ChevronUp class="icon-sm" />
        </button>
        <button
          class="chat-panel__search-nav"
          type="button"
          @click="jumpToPrevMatch"
          :disabled="!isSearchActive || !searchMatches.length"
          aria-label="아래로 탐색"
        >
          <ChevronDown class="icon-sm" />
        </button>
        <div class="chat-panel__search-count">
          {{ isSearchActive ? (searchMatches.length ? `${searchIndex + 1}/${searchMatches.length}` : '0/0') : '0/0' }}
        </div>
        <button class="chat-panel__search-cancel" type="button" @click.stop="clearSearch">
          취소
        </button>
      </div>
      <div v-if="isSearchOpen && searchTerm && isSearchActive && !searchMatches.length" class="chat-panel__search-empty">
        일치하는 대화가 없어요.
      </div>

        <ul ref="chatMessagesRef" class="chat-panel__list">
          <li v-if="collabStore.messages.length === 0" class="chat-panel__empty">
            아직 메시지가 없어요.
          </li>
          <li
            v-for="msg in collabStore.messages"
            :key="msg.messageId"
            :class="[
              'chat-panel__item',
              { 'chat-panel__item--mine': msg.senderId === collabStore.localParticipant.odps },
              { 'chat-panel__item--highlight': msg.messageId === currentMatchId },
            ]"
            :ref="(el) => setMessageRef(el, msg.messageId)"
          >
            <div v-if="msg.senderId !== collabStore.localParticipant.odps" class="chat-panel__row">
              <Avatar
                class="chat-panel__avatar"
                :src="getAvatarUrl(msg.senderId) || ''"
                :alt="msg.senderName || 'Guest'"
                :user-id="parseInt(msg.senderId, 10) || 0"
                size="sm"
              />
              <div class="chat-panel__body">
                <div class="chat-panel__name">{{ msg.senderName }}</div>
                <div class="chat-panel__bubble-row">
                  <div class="chat-panel__bubble">
                    <div class="chat-panel__text" v-html="getHighlightedHtml(msg.content)"></div>
                  </div>
                  <div class="chat-panel__time">{{ formatTime(msg.timestamp) }}</div>
                </div>
              </div>
            </div>
            <div v-else class="chat-panel__row chat-panel__row--mine">
              <div class="chat-panel__bubble-row chat-panel__bubble-row--mine">
                <div class="chat-panel__time">{{ formatTime(msg.timestamp) }}</div>
                <div class="chat-panel__bubble chat-panel__bubble--mine">
                  <div class="chat-panel__text" v-html="getHighlightedHtml(msg.content)"></div>
                </div>
              </div>
            </div>
          </li>
        </ul>

        <form class="chat-panel__input" @submit.prevent="handleSendMessage">
          <input
            v-model="messageInput"
            :disabled="!isConnected"
            type="text"
            class="chat-panel__field"
            :placeholder="isConnected ? '메시지 입력...' : '연결 중...'"
          />
          <button class="chat-panel__send" type="submit" :disabled="!isConnected">
            <Send class="icon" />
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sidebar-collab-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  position: relative;
}

.sidebar-collab-actions.expanded {
  align-items: stretch;
}

.collab-expanded-shell {
  opacity: 0;
  pointer-events: none;
  transition:
    opacity 0.1s ease;
}

.collab-expanded-shell.open {
  opacity: 1;
  pointer-events: auto;
  transition:
    opacity 0.15s ease 0.2s;
}

.collab-collapsed-shell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.1s ease;
}

.collab-collapsed-shell.open {
  opacity: 1;
  pointer-events: auto;
  transition:
    opacity 0.1s ease;
}

.collab-expanded {
  border: 1px solid var(--rose-100);
  background: var(--rose-50);
  border-radius: 16px;
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: 0 10px 20px rgba(255, 133, 161, 0.08);
}

.collab-expanded__title {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--gray-700);
}

.collab-expanded__actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.6rem;
}

.collab-expanded__actions > * {
  width: 100%;
}

.collab-expanded__actions .action-popover-wrap {
  width: 100%;
}

.collab-action {
  border: 1px solid var(--rose-200);
  background: white;
  border-radius: 12px;
  padding: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  min-height: 48px;
  justify-content: flex-start;
  cursor: pointer;
  transition: box-shadow 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
}

.collab-action:hover {
  border-color: var(--rose-300);
  box-shadow: 0 8px 16px rgba(255, 133, 161, 0.12);
  transform: translateY(-1px);
}

.collab-action__label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--gray-700);
}

.action-button {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  border: 1px solid transparent;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.action-button:hover {
  background: var(--gray-50);
  border-color: var(--gray-200);
}

.action-icon {
  position: relative;
  width: 32px;
  height: 32px;
  border-radius: 12px;
  border: 1px solid var(--rose-200);
  background: var(--rose-50);
  color: var(--rose-600);
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-icon--call {
  border-color: var(--success-bg);
  background: var(--success-bg);
  color: var(--success-600);
}

.action-icon--chat {
  border-color: var(--rose-50);
  background: var(--rose-50);
  color: var(--rose-600);
}

.action-popover-wrap {
  position: relative;
}

.action-popover {
  position: fixed;
  left: 0;
  top: 0;
  width: min(340px, 72vw);
  height: min(420px, calc(100vh - 16px));
  max-height: calc(100vh - 16px);
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 14px;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.12);
  z-index: 40;
  padding: 0.75rem;
  animation: pop-in 0.18s ease-out;
  display: flex;
  overflow: hidden;
  transition: opacity 0.2s ease;
}

.action-popover.dimmed {
  opacity: 0.4;
}

.action-popover.shifting,
.call-popover.shifting {
  transition:
    left var(--sidebar-shift-ms, 300ms) cubic-bezier(0.4, 0, 0.2, 1),
    top var(--sidebar-shift-ms, 300ms) cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.2s ease;
}

.call-popover {
  position: fixed;
  width: min(240px, 70vw);
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 12px;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.14);
  padding: 0.6rem;
  z-index: 45;
  animation: pop-in 0.18s ease-out;
  cursor: grab;
  transition: opacity 0.2s ease;
}

.call-popover.dragging {
  cursor: grabbing;
  transition: none;
}

.call-popover.dimmed {
  opacity: 0.4;
}

.call-panel {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.call-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--gray-700);
}

.call-panel__title {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.call-panel__collapse {
  width: 26px;
  height: 26px;
  border-radius: 999px;
  border: 1px solid var(--rose-100);
  background: var(--rose-50);
  color: var(--gray-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.call-panel__collapse:hover {
  background: var(--rose-100);
  color: var(--rose-500);
  border-color: var(--rose-200);
}


.call-panel__status {
  font-size: 0.75rem;
  color: var(--gray-500);
  padding: 0.2rem 0.1rem;
}

.call-panel__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  max-height: 200px;
  overflow-y: auto;
}

.call-panel__controls {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding-top: 0.1rem;
}

.call-control-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  border: 1px solid var(--rose-200);
  background: var(--rose-50);
  color: var(--gray-700);
  border-radius: 8px;
  padding: 0.25rem 0.4rem;
  font-size: 0.7rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;
  white-space: nowrap;
}

.call-control-btn:hover:enabled {
  border-color: var(--rose-300);
  background: var(--rose-100);
  color: var(--gray-800);
}

.call-control-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.call-control-btn.active {
  border-color: var(--rose-400);
  background: var(--rose-500);
  color: white;
}

.call-control-btn.active:hover:enabled {
  border-color: var(--rose-400);
  background: var(--rose-500);
  color: white;
}

.call-control-btn.danger {
  border-color: var(--error-bg);
  background: var(--error-bg);
  color: var(--error);
  margin-left: auto;
}

.call-control-btn.danger:hover:enabled {
  background: var(--error-soft);
  border-color: var(--error-soft);
}

.call-panel__item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.call-avatar {
  position: relative;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  border: 2px solid transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  flex-shrink: 0;
}

.call-avatar :deep(.avatar) {
  width: 100%;
  height: 100%;
}

.call-avatar.speaking {
  border-color: #22c55e;
  box-shadow: none;
}

.call-avatar__mute {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: white;
  border: 1px solid var(--rose-200);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rose-500);
  box-shadow: 0 2px 4px rgba(255, 133, 161, 0.15);
}

.call-name {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--gray-700);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.call-me {
  font-size: 0.7rem;
  color: var(--rose-500);
  margin-left: 0.25rem;
}

@keyframes pop-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.action-popover.dragging {
  cursor: grabbing;
  user-select: none;
  transition: none;
}

.drag-handle {
  cursor: grab;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.chat-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--rose-100);
}

.chat-panel__title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: var(--gray-800);
  font-size: 0.9rem;
}

.chat-panel__title .icon {
  color: var(--rose-500);
}

.chat-panel__status {
  font-size: 0.7rem;
  color: var(--gray-500);
}

.chat-panel__status.online {
  color: var(--success-600);
}

.chat-panel__actions {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.chat-panel__search,
.chat-panel__close {
  border: none;
  background: var(--rose-50);
  color: var(--rose-500);
  border-radius: 10px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.chat-panel__search:hover,
.chat-panel__close:hover {
  background: var(--rose-100);
  color: var(--rose-600);
}

.chat-panel__search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto auto;
  align-items: center;
  gap: 0.2rem;
  padding-bottom: 0.4rem;
}

.chat-panel__search-input {
  flex: 1;
  border: 1px solid var(--rose-200);
  background: var(--rose-50);
  border-radius: 10px;
  padding: 0.35rem 0.6rem;
  font-size: 0.75rem;
  color: var(--gray-800);
}

.chat-panel__search-input:focus {
  outline: 2px solid var(--rose-200);
  outline-offset: 1px;
}

.chat-panel__search-nav {
  border: none;
  background: var(--rose-100);
  color: var(--rose-600);
  border-radius: 8px;
  padding: 0.25rem;
  font-size: 0.7rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.2rem;
  min-width: 30px;
  height: 28px;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.chat-panel__search-nav:first-of-type {
  margin-left: 0.35rem;
}

.chat-panel__bubble--mine :deep(.chat-panel__highlight) {
  background: rgba(255, 255, 255, 0.35);
  border-radius: 4px;
  padding: 0 2px;
  color: var(--rose-600);
}

.chat-panel__search-nav:hover:enabled {
  background: var(--rose-200);
  color: var(--rose-600);
}

.chat-panel__search-nav:disabled {
  cursor: not-allowed;
  color: var(--gray-400);
  background: var(--rose-50);
}

.chat-panel__search-count {
  font-size: 0.7rem;
  color: var(--gray-400);
  min-width: 44px;
  text-align: center;
  white-space: nowrap;
}

.chat-panel__search-cancel {
  border: none;
  background: transparent;
  color: var(--gray-500);
  font-size: 0.72rem;
  border-radius: 8px;
  padding: 0.25rem 0.4rem;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s ease, color 0.2s ease;
}

.chat-panel__search-cancel:hover {
  background: var(--rose-50);
  color: var(--gray-700);
}

.chat-panel__search-empty {
  font-size: 0.72rem;
  color: var(--gray-500);
  padding: 0.1rem 0.1rem 0.2rem;
}

.chat-panel__list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: none;
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  padding-bottom: 0.5rem;
  scroll-padding-bottom: 12px;
}

.chat-panel__empty {
  font-size: 0.75rem;
  color: var(--gray-500);
  padding: 0.4rem 0.2rem;
}

.chat-panel__item {
  display: flex;
  flex-direction: column;
}

.chat-panel__item--highlight .chat-panel__bubble {
  box-shadow: 0 0 0 2px var(--rose-300);
}

.chat-panel__item--highlight .chat-panel__bubble--mine {
  box-shadow: 0 0 0 2px var(--rose-200);
}

.chat-panel__row {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
}

.chat-panel__row--mine {
  justify-content: flex-end;
}

.chat-panel__bubble {
  max-width: 220px;
  background: var(--rose-50);
  border: 1px solid var(--rose-100);
  border-radius: 12px 12px 12px 4px;
  padding: 0.5rem 0.7rem;
  font-size: 0.8rem;
  color: var(--gray-800);
  display: flex;
  flex-direction: column;
}

.chat-panel__bubble--mine {
  background: var(--rose-500);
  color: white;
  border: 1px solid var(--rose-600);
  border-radius: 12px 12px 4px 12px;
}

.chat-panel__avatar {
  display: inline-flex;
  flex-shrink: 0;
  width: 28px;
  height: 28px;
}

.chat-panel__body {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.chat-panel__name {
  font-weight: 600;
  font-size: 0.72rem;
  color: var(--gray-700);
}

.chat-panel__bubble-row {
  display: flex;
  align-items: flex-end;
  gap: 0.4rem;
}

.chat-panel__bubble-row--mine {
  justify-content: flex-end;
}

.chat-panel__text {
  word-break: break-word;
}

:deep(.chat-panel__highlight) {
  color: var(--rose-500);
  font-weight: 600;
}

.chat-panel__time {
  font-size: 0.7rem;
  color: var(--gray-400);
  white-space: nowrap;
}

.chat-panel__input {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.chat-panel__field {
  flex: 1;
  border: 1px solid var(--rose-200);
  background: var(--rose-50);
  border-radius: 10px;
  padding: 0.5rem 0.75rem;
  font-size: 0.8rem;
  color: var(--gray-800);
}

.chat-panel__field:disabled {
  background: var(--rose-50);
  color: var(--gray-400);
}

.chat-panel__send {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: var(--rose-500);
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.chat-panel__send:hover:enabled {
  background: var(--rose-100);
  color: var(--rose-600);
}

.chat-panel__send:disabled {
  cursor: not-allowed;
  color: var(--gray-400);
}

.unread-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  background: var(--rose-500);
  border-radius: 999px;
  border: 2px solid white;
}

.icon-sm {
  width: 16px;
  height: 16px;
}

.icon-xs {
  width: 10px;
  height: 10px;
}

.icon {
  width: 18px;
  height: 18px;
}
</style>
