<script setup lang="ts">
import { ref, nextTick } from 'vue';
import { useCollabStore } from '../../stores/collab';
import ParticipantAvatar from './ParticipantAvatar.vue';
import Button from '../common/Button.vue';
import {
  X,
  Mic,
  MicOff,
  Send,
  Plus,
} from 'lucide-vue-next';

const collabStore = useCollabStore();
const messageInput = ref('');
const chatMessagesRef = ref<HTMLElement | null>(null);

/**
 * 메시지 전송 핸들러
 */
async function handleSendMessage() {
  if (!messageInput.value.trim()) return;
  
  collabStore.sendMessage(messageInput.value);
  messageInput.value = '';
  
  // 스크롤을 아래로 이동
  await nextTick();
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight;
  }
}

/**
 * 시간 포맷팅
 */
function formatTime(timestamp: number): string {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('ko-KR', { 
    hour: '2-digit', 
    minute: '2-digit' 
  });
}
</script>

<template>
  <div class="collab-panel">
    <!-- Header -->
    <div class="panel-header drag-handle">
      <div class="panel-title">
        <span class="live-dot"></span>
        브레인스토밍 허들
      </div>
      <button class="close-btn" @click="collabStore.togglePanel" title="최소화">
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
        :key="p.odps"
        :participant="p"
      />
      <button class="add-participant" title="참여자 초대">
        <Plus class="icon" />
      </button>
    </div>

    <!-- Chat Area -->
    <div class="chat-area">
      <div class="chat-label">실시간 대화</div>
      <div ref="chatMessagesRef" class="chat-messages">
        <div v-if="collabStore.messages.length === 0" class="no-messages">
          아직 메시지가 없습니다
        </div>
        <div
          v-for="msg in collabStore.messages"
          :key="msg.messageId"
          class="message"
        >
          <span class="message-sender">{{ msg.senderName }}</span>
          <span class="message-content">{{ msg.content }}</span>
          <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
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
          <Send class="icon-sm" />
        </Button>
      </form>

      <div class="controls">
        <div class="media-controls">
          <button
            :class="['media-btn', { muted: collabStore.isMuted }]"
            @click="collabStore.toggleMute"
            :title="collabStore.isMuted ? 'Unmute' : 'Mute'"
          >
            <MicOff v-if="collabStore.isMuted" class="icon" />
            <Mic v-else class="icon" />
          </button>
        </div>
        <button class="leave-btn" @click="collabStore.leaveRoom">
          &#53685;&#54868; &#51333;&#47308;
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
  animation: slideUp 0.2s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Header */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border-bottom: 1px solid var(--rose-100);
  background: linear-gradient(135deg, var(--rose-50), white);
}

.drag-handle {
  cursor: grab;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--gray-800);
}

.live-dot {
  width: 8px;
  height: 8px;
  background: var(--success);
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
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: var(--rose-100);
  color: var(--rose-500);
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
  overflow-x: auto;
}

.add-participant {
  width: 40px;
  height: 40px;
  min-width: 40px;
  border: 2px dashed var(--gray-200);
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-300);
  transition: all 0.2s;
}

.add-participant:hover {
  border-color: var(--rose-300);
  color: var(--rose-400);
  background: var(--rose-50);
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
  letter-spacing: 0.05em;
  margin-bottom: 0.5rem;
}

.chat-messages {
  height: calc(100% - 20px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.no-messages {
  color: var(--gray-400);
  font-size: 0.75rem;
  text-align: center;
  padding: 1rem 0;
}

.message {
  font-size: 0.75rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  align-items: baseline;
}

.message-sender {
  font-weight: 600;
  color: var(--rose-500);
}

.message-content {
  color: var(--gray-700);
  flex: 1;
}

.message-time {
  font-size: 0.625rem;
  color: var(--gray-400);
}

/* Input */
.input-area {
  padding: 0.75rem 1rem;
  background: var(--gray-50);
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
  transition: all 0.2s;
}

.message-input:focus {
  outline: none;
  border-color: var(--rose-400);
  box-shadow: 0 0 0 3px rgba(255, 133, 161, 0.1);
}

/* Uses global .icon-sm from base.css */

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
  background: var(--rose-50);
}

.media-btn.muted {
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
  transition: all 0.2s;
}

.leave-btn:hover {
  background: var(--error-soft);
}
</style>
