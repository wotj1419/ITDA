# Phase 6: WebRTC 협업 구현 계획서

> **버전**: 1.0 | **작성일**: 2026-01-17  
> **목표**: 플로팅 협업 바 구현 (화상통화, 채팅, 화면공유)

---

## 1. 개요

### 목표
- 플로팅 협업 바 (Discord 허들 스타일)
- 화면 이동해도 통화 유지
- 마이크/카메라/화면공유 토글
- 실시간 채팅
- **UI만 먼저 구현**, 실제 WebRTC 연결은 백엔드 완료 후

### 레퍼런스
- HTML: `ams-v2_5-rose/assets/css/components/collab.css`
- 위치: `scene-edit.html`, `timeline.html` 하단

---

## 2. 파일 목록

### 새로 생성 (8개)

| # | 경로 | 설명 |
|---|------|------|
| 1 | `src/services/webrtc/signaling.ts` | 시그널링 WebSocket (Stub) |
| 2 | `src/services/webrtc/peerConnection.ts` | PeerConnection (Stub) |
| 3 | `src/stores/collab.ts` | 협업 상태 관리 |
| 4 | `src/composables/useWebRTC.ts` | WebRTC 훅 (Stub) |
| 5 | `src/components/collab/CollabContainer.vue` | 플로팅 컨테이너 |
| 6 | `src/components/collab/CollabPanel.vue` | 확장 패널 |
| 7 | `src/components/collab/CollabPill.vue` | 축소 필 |
| 8 | `src/components/collab/ParticipantAvatar.vue` | 참여자 아바타 |

### 수정 (1개)

| 경로 | 변경 내용 |
|------|----------|
| `src/App.vue` | CollabContainer 추가 (전역) |

---

## 3. 타입 정의

`src/types/index.ts`에 추가:

```typescript
// ================================
// Collaboration Types (Phase 6)
// ================================

export interface CollabParticipant {
  odps: string
  name: string
  avatarUrl?: string
  isMuted: boolean
  isVideoOff: boolean
  isScreenSharing: boolean
  currentLocation?: string // "Scene 1 편집 중"
}

export interface CollabMessage {
  messageId: string
  senderId: string
  senderName: string
  content: string
  timestamp: number
}

export type CollabStatus = 'disconnected' | 'connecting' | 'connected' | 'error'
```

---

## 4. Pinia 스토어

### `src/stores/collab.ts`

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CollabParticipant, CollabMessage, CollabStatus } from '../types'

export const useCollabStore = defineStore('collab', () => {
  // State
  const status = ref<CollabStatus>('disconnected')
  const participants = ref<CollabParticipant[]>([])
  const messages = ref<CollabMessage[]>([])
  const isPanelOpen = ref(false)

  // Local user state
  const isMuted = ref(true)
  const isVideoOff = ref(true)
  const isScreenSharing = ref(false)
  const currentLocation = ref('')

  // Room
  const roomId = ref<string | null>(null)

  // Getters
  const isConnected = computed(() => status.value === 'connected')
  const participantCount = computed(() => participants.value.length)
  const hasUnreadMessages = computed(() => messages.value.length > 0)

  const localParticipant = computed<CollabParticipant>(() => ({
    odps: 'me',
    name: 'ME',
    isMuted: isMuted.value,
    isVideoOff: isVideoOff.value,
    isScreenSharing: isScreenSharing.value,
    currentLocation: currentLocation.value,
  }))

  // Actions
  function joinRoom(projectId: number): void {
    roomId.value = `project-${projectId}`
    status.value = 'connecting'

    // Simulate connection
    setTimeout(() => {
      status.value = 'connected'
      // Add mock participants
      participants.value = [
        {
          userId: 'user-sj',
          name: 'SJ',
          isMuted: false,
          isVideoOff: false,
          isScreenSharing: false,
          currentLocation: 'Scene 1 편집 중',
        },
      ]
    }, 500)
  }

  function leaveRoom(): void {
    status.value = 'disconnected'
    roomId.value = null
    participants.value = []
    messages.value = []
    isPanelOpen.value = false
  }

  function toggleMute(): void {
    isMuted.value = !isMuted.value
  }

  function toggleVideo(): void {
    isVideoOff.value = !isVideoOff.value
  }

  function toggleScreenShare(): void {
    isScreenSharing.value = !isScreenSharing.value
  }

  function togglePanel(): void {
    isPanelOpen.value = !isPanelOpen.value
  }

  function sendMessage(content: string): void {
    if (!content.trim()) return

    const message: CollabMessage = {
      messageId: `msg-${Date.now()}`,
      senderId: 'me',
      senderName: 'ME',
      content: content.trim(),
      timestamp: Date.now(),
    }
    messages.value.push(message)
  }

  function updateLocation(location: string): void {
    currentLocation.value = location
    // TODO: Broadcast to other participants
  }

  return {
    // State
    status,
    participants,
    messages,
    isPanelOpen,
    isMuted,
    isVideoOff,
    isScreenSharing,
    currentLocation,
    roomId,
    // Getters
    isConnected,
    participantCount,
    hasUnreadMessages,
    localParticipant,
    // Actions
    joinRoom,
    leaveRoom,
    toggleMute,
    toggleVideo,
    toggleScreenShare,
    togglePanel,
    sendMessage,
    updateLocation,
  }
})
```

---

## 5. 컴포넌트

### 5.1 CollabContainer.vue

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { useCollabStore } from '../../stores/collab'
import CollabPanel from './CollabPanel.vue'
import CollabPill from './CollabPill.vue'

const collabStore = useCollabStore()

const showCollab = computed(() => collabStore.isConnected)
</script>

<template>
  <div v-if="showCollab" class="collab-container">
    <!-- Expanded Panel -->
    <CollabPanel v-if="collabStore.isPanelOpen" />

    <!-- Collapsed Pill -->
    <CollabPill v-else />
  </div>
</template>

<style scoped>
.collab-container {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  z-index: 1000;
}
</style>
```

### 5.2 CollabPanel.vue

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useCollabStore } from '../../stores/collab'
import ParticipantAvatar from './ParticipantAvatar.vue'
import Button from '../common/Button.vue'
import {
  X,
  Mic,
  MicOff,
  Video,
  VideoOff,
  Monitor,
  Send,
  Plus,
} from 'lucide-vue-next'

const collabStore = useCollabStore()
const messageInput = ref('')

function handleSendMessage() {
  collabStore.sendMessage(messageInput.value)
  messageInput.value = ''
}
</script>

<template>
  <div class="collab-panel">
    <!-- Header -->
    <div class="panel-header">
      <div class="panel-title">
        <span class="live-dot"></span>
        브레인스토밍 허들
      </div>
      <button class="close-btn" @click="collabStore.togglePanel">
        <X class="icon" />
      </button>
    </div>

    <!-- Participants -->
    <div class="participants">
      <ParticipantAvatar
        :participant="collabStore.localParticipant"
        is-me
      />
      <ParticipantAvatar
        v-for="p in collabStore.participants"
        :key="p.userId"
        :participant="p"
      />
      <button class="add-participant">
        <Plus class="icon" />
      </button>
    </div>

    <!-- Chat Area -->
    <div class="chat-area">
      <div class="chat-label">실시간 대화</div>
      <div class="chat-messages">
        <div
          v-for="msg in collabStore.messages"
          :key="msg.messageId"
          class="message"
        >
          <span class="message-sender">{{ msg.senderName }}</span>
          <span class="message-content">{{ msg.content }}</span>
        </div>
      </div>
    </div>

    <!-- Input -->
    <div class="input-area">
      <form class="input-row" @submit.prevent="handleSendMessage">
        <input
          v-model="messageInput"
          type="text"
          class="message-input"
          placeholder="메시지 입력..."
        />
        <Button type="submit" variant="primary" size="sm">
          <Send class="btn-icon" />
        </Button>
      </form>

      <div class="controls">
        <div class="media-controls">
          <button
            :class="['media-btn', { active: !collabStore.isMuted }]"
            @click="collabStore.toggleMute"
          >
            <MicOff v-if="collabStore.isMuted" class="icon" />
            <Mic v-else class="icon" />
          </button>
          <button
            :class="['media-btn', { active: !collabStore.isVideoOff }]"
            @click="collabStore.toggleVideo"
          >
            <VideoOff v-if="collabStore.isVideoOff" class="icon" />
            <Video v-else class="icon" />
          </button>
          <button
            :class="['media-btn', { active: collabStore.isScreenSharing }]"
            @click="collabStore.toggleScreenShare"
          >
            <Monitor class="icon" />
          </button>
        </div>
        <button class="leave-btn" @click="collabStore.leaveRoom">
          나가기
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.collab-panel {
  width: 320px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border-bottom: 1px solid var(--rose-100);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  font-size: 0.875rem;
}

.live-dot {
  width: 8px;
  height: 8px;
  background: #22c55e;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.25rem;
  color: var(--gray-400);
}

.icon {
  width: 16px;
  height: 16px;
}

/* Participants */
.participants {
  display: flex;
  gap: 0.5rem;
  padding: 1rem;
  border-bottom: 1px solid var(--rose-100);
}

.add-participant {
  width: 40px;
  height: 40px;
  border: 2px dashed var(--gray-200);
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-300);
}

/* Chat */
.chat-area {
  height: 150px;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--rose-100);
}

.chat-label {
  font-size: 0.625rem;
  font-weight: 600;
  color: var(--gray-400);
  text-transform: uppercase;
  margin-bottom: 0.5rem;
}

.chat-messages {
  height: calc(100% - 20px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.message {
  font-size: 0.75rem;
}

.message-sender {
  font-weight: 600;
  color: var(--rose-500);
  margin-right: 0.5rem;
}

.message-content {
  color: var(--gray-700);
}

/* Input */
.input-area {
  padding: 0.75rem 1rem;
}

.input-row {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.message-input {
  flex: 1;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--rose-200);
  border-radius: 8px;
  font-size: 0.875rem;
}

.btn-icon {
  width: 14px;
  height: 14px;
}

.controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.media-controls {
  display: flex;
  gap: 0.5rem;
}

.media-btn {
  width: 32px;
  height: 32px;
  border: 1px solid var(--rose-200);
  border-radius: 8px;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-500);
  transition: all 0.2s;
}

.media-btn:hover {
  border-color: var(--rose-300);
}

.media-btn.active {
  background: var(--rose-500);
  border-color: var(--rose-500);
  color: white;
}

.leave-btn {
  padding: 0.375rem 0.75rem;
  background: var(--error-bg);
  color: var(--error);
  border: none;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
}
</style>
```

### 5.3 CollabPill.vue

```vue
<script setup lang="ts">
import { useCollabStore } from '../../stores/collab'
import {
  Mic,
  MicOff,
  Video,
  VideoOff,
  Monitor,
  MessageCircle,
} from 'lucide-vue-next'

const collabStore = useCollabStore()
</script>

<template>
  <div class="collab-pill" @click="collabStore.togglePanel">
    <div class="indicator">
      <span class="dot"></span>
      Live
    </div>
    <div class="divider"></div>
    <button class="pill-btn" @click.stop="collabStore.toggleMute">
      <MicOff v-if="collabStore.isMuted" class="icon" />
      <Mic v-else class="icon" />
    </button>
    <button class="pill-btn" @click.stop="collabStore.toggleVideo">
      <VideoOff v-if="collabStore.isVideoOff" class="icon" />
      <Video v-else class="icon" />
    </button>
    <button class="pill-btn" @click.stop="collabStore.toggleScreenShare">
      <Monitor class="icon" />
    </button>
    <button class="pill-btn">
      <MessageCircle class="icon" />
    </button>
  </div>
</template>

<style scoped>
.collab-pill {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5rem 0.75rem;
  background: white;
  border: 1px solid var(--rose-200);
  border-radius: 9999px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  cursor: pointer;
}

.indicator {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--gray-700);
}

.dot {
  width: 8px;
  height: 8px;
  background: #22c55e;
  border-radius: 50%;
}

.divider {
  width: 1px;
  height: 24px;
  background: var(--rose-200);
  margin: 0 0.25rem;
}

.pill-btn {
  width: 28px;
  height: 28px;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-500);
  transition: all 0.2s;
}

.pill-btn:hover {
  background: var(--rose-50);
  color: var(--rose-500);
}

.icon {
  width: 14px;
  height: 14px;
}
</style>
```

### 5.4 ParticipantAvatar.vue

```vue
<script setup lang="ts">
import type { CollabParticipant } from '../../types'

interface Props {
  participant: CollabParticipant
  isMe?: boolean
}

withDefaults(defineProps<Props>(), {
  isMe: false,
})
</script>

<template>
  <div class="participant">
    <div :class="['avatar', { me: isMe }]">
      <img
        v-if="participant.avatarUrl"
        :src="participant.avatarUrl"
        :alt="participant.name"
      />
      <span v-else>{{ participant.name.substring(0, 2) }}</span>
    </div>
    <span class="name">{{ isMe ? 'ME' : participant.name }}</span>
  </div>
</template>

<style scoped>
.participant {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
}

.avatar {
  width: 40px;
  height: 40px;
  background: var(--rose-100);
  border: 2px solid var(--rose-300);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--rose-600);
  overflow: hidden;
}

.avatar.me {
  background: var(--rose-500);
  border-color: var(--rose-600);
  color: white;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.name {
  font-size: 0.625rem;
  font-weight: 500;
  color: var(--gray-500);
}
</style>
```

---

## 6. App.vue 수정

`src/App.vue`에 CollabContainer 추가:

```vue
<script setup lang="ts">
import { RouterView } from 'vue-router'
import ToastContainer from './components/common/ToastContainer.vue'
import CollabContainer from './components/collab/CollabContainer.vue'
</script>

<template>
  <RouterView />
  <ToastContainer />
  <CollabContainer />
</template>
```

---

## 7. 협업 시작 트리거

프로젝트 상세 페이지, 씬 에디터, 타임라인에서 협업 시작:

```typescript
// ProjectDetailPage.vue, SceneEditPage.vue, TimelinePage.vue
import { useCollabStore } from '../stores/collab'

const collabStore = useCollabStore()

// "협업 시작" 버튼 클릭 시
function handleStartCollab() {
  collabStore.joinRoom(projectId.value)
}
```

---

## 8. 구현 순서 체크리스트

1. [ ] `src/types/index.ts` - 협업 타입 추가
2. [ ] `src/services/webrtc/signaling.ts` (Stub)
3. [ ] `src/services/webrtc/peerConnection.ts` (Stub)
4. [ ] `src/stores/collab.ts`
5. [ ] `src/composables/useWebRTC.ts` (Stub)
6. [ ] `src/components/collab/ParticipantAvatar.vue`
7. [ ] `src/components/collab/CollabPill.vue`
8. [ ] `src/components/collab/CollabPanel.vue`
9. [ ] `src/components/collab/CollabContainer.vue`
10. [ ] `src/App.vue` 수정
11. [ ] 각 페이지에 협업 시작 버튼 연결
12. [ ] `npm run build`

---

## 9. 검증

1. 아무 페이지에서 "협업 시작" 클릭
2. 플로팅 필 표시 확인
3. 필 클릭 → 패널 확장
4. 마이크/카메라/화면공유 토글
5. 채팅 입력 → 메시지 표시
6. 다른 페이지로 이동 → 협업 바 유지
7. "나가기" 클릭 → 협업 종료

---

## 10. 향후 작업 (백엔드 완료 후)

- [ ] 실제 WebSocket 시그널링 연결
- [ ] WebRTC PeerConnection 구현
- [ ] TURN/STUN 서버 연동
- [ ] 미디어 스트림 처리
- [ ] Presence 브로드캐스트

