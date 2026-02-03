<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch, type ComponentPublicInstance } from 'vue'
import { useCollabStore } from '../../stores/collab'
import { ChevronDown, ChevronUp, MessageCircle, Phone, Search, Send, X } from 'lucide-vue-next'

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
const searchInputRef = ref<HTMLInputElement | null>(null)
const isChatOpen = ref(false)
const panelPosition = ref({ x: 0, y: 0 })
const dragOffset = ref({ x: 0, y: 0 })
const isDragging = ref(false)
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

const isConnected = computed(() => collabStore.isConnected)
const statusText = computed(() => {
  if (collabStore.status === 'connecting') return '연결 중'
  if (collabStore.status === 'connected') return '온라인'
  if (collabStore.status === 'error') return '오류'
  return '오프라인'
})

function ensureJoined() {
  const pid = Number(props.projectId)
  if (!Number.isFinite(pid)) return
  if (collabStore.status === 'disconnected') {
    void collabStore.joinRoom(pid)
  }
}

function handleStartCall() {
  const pid = Number(props.projectId)
  if (!Number.isFinite(pid)) return
  isChatOpen.value = false
  collabStore.startCall(pid)
}

async function toggleChat() {
  if (!isChatOpen.value) {
    ensureJoined()
  }
  isChatOpen.value = !isChatOpen.value
  if (isChatOpen.value) {
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
  return participant?.avatarUrl ?? '';
}

function getSenderInitial(name: string | undefined) {
  if (!name) return '?';
  return name.trim().slice(0, 1).toUpperCase();
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
    if (hasCustomPosition.value) {
      panelPosition.value = clampPanelPosition(panelPosition.value.x, panelPosition.value.y)
      return
    }
    await nextTick()
    positionPanelFromButton()
  }
)

function getPanelSize() {
  const rect = panelRef.value?.getBoundingClientRect()
  return {
    width: rect?.width ?? 340,
    height: rect?.height ?? 360,
  }
}

function clampPanelPosition(nextX: number, nextY: number) {
  const { width, height } = getPanelSize()
  const maxX = Math.max(PANEL_PADDING, window.innerWidth - width - PANEL_PADDING)
  const maxY = Math.max(PANEL_PADDING, window.innerHeight - height - PANEL_PADDING)
  return {
    x: Math.min(Math.max(PANEL_PADDING, nextX), maxX),
    y: Math.min(Math.max(PANEL_PADDING, nextY), maxY),
  }
}

function positionPanelFromButton() {
  const anchor = chatButtonRef.value?.getBoundingClientRect()
  if (!anchor) return
  const { height } = getPanelSize()
  const nextX = anchor.right + PANEL_OFFSET
  const nextY = anchor.top - height - PANEL_OFFSET + 40
  panelPosition.value = clampPanelPosition(nextX, nextY)
  hasCustomPosition.value = false
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
  panelPosition.value = clampPanelPosition(nextX, nextY)
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
    panelPosition.value = clampPanelPosition(panelPosition.value.x, panelPosition.value.y)
    return
  }
  positionPanelFromButton()
}

function handleOutsideClick(event: MouseEvent) {
  if (isDragging.value) return
  if (Date.now() - lastDragEndAt.value < 200) return
  const target = event.target as Node | null
  if (!menuRef.value || !target) return
  if (!menuRef.value.contains(target)) {
    isChatOpen.value = false
    clearSearch()
  }
}

onMounted(() => {
  window.addEventListener('click', handleOutsideClick)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', handleOutsideClick)
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('pointermove', handleDragMove)
  window.removeEventListener('pointerup', endDrag)
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
        panelPosition.value = clampPanelPosition(panelPosition.value.x, panelPosition.value.y - overflowBottom)
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
    panelPosition.value = clampPanelPosition(searchBasePosition.value.x, searchBasePosition.value.y)
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
          <button class="collab-action" type="button" @click.stop="handleStartCall">
            <span class="action-icon action-icon--call">
              <Phone class="icon" />
            </span>
            <span class="collab-action__label">통화</span>
          </button>

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
      <button
        class="action-button"
        type="button"
        title="통화"
        aria-label="통화"
        @click.stop="handleStartCall"
      >
        <span class="action-icon action-icon--call">
          <Phone class="icon" />
        </span>
      </button>

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
      v-if="isChatOpen"
      ref="panelRef"
      class="action-popover"
      :class="{ dragging: isDragging }"
      :style="{ left: `${panelPosition.x}px`, top: `${panelPosition.y}px` }"
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
              <span
                class="chat-panel__avatar"
                :class="{ 'chat-panel__avatar--image': !!getAvatarUrl(msg.senderId) }"
                :style="getAvatarUrl(msg.senderId) ? { backgroundImage: `url(${getAvatarUrl(msg.senderId)})` } : {}"
              >
                <span v-if="!getAvatarUrl(msg.senderId)" class="chat-panel__avatar-text">
                  {{ getSenderInitial(msg.senderName) }}
                </span>
              </span>
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
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 14px;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.12);
  z-index: 40;
  padding: 0.75rem;
  animation: pop-in 0.18s ease-out;
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
}

.drag-handle {
  cursor: grab;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
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
  max-height: 240px;
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
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: var(--gray-200);
  color: var(--gray-700);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.6rem;
  font-weight: 600;
  overflow: hidden;
  background-position: center;
  background-size: cover;
}

.chat-panel__avatar--image {
  background-color: transparent;
}

.chat-panel__avatar-text {
  line-height: 1;
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

.icon {
  width: 18px;
  height: 18px;
}
</style>
