<script setup lang="ts">
import { useCollabStore } from '../../stores/collab';
import {
  Mic,
  MicOff,
  Video,
  VideoOff,
  Monitor,
  MessageCircle,
} from 'lucide-vue-next';

const collabStore = useCollabStore();
</script>

<template>
  <div class="collab-pill">
    <!-- Live Indicator -->
    <div class="indicator">
      <span class="dot"></span>
      Live
    </div>
    
    <div class="divider"></div>
    
    <!-- Media Controls -->
    <button 
      class="pill-btn" 
      :class="{ active: !collabStore.isMuted }"
      @click.stop="collabStore.toggleMute"
      title="마이크 토글"
    >
      <MicOff v-if="collabStore.isMuted" class="icon" />
      <Mic v-else class="icon" />
    </button>
    
    <button 
      class="pill-btn" 
      :class="{ active: !collabStore.isVideoOff }"
      @click.stop="collabStore.toggleVideo"
      title="비디오 토글"
    >
      <VideoOff v-if="collabStore.isVideoOff" class="icon" />
      <Video v-else class="icon" />
    </button>
    
    <button 
      class="pill-btn" 
      :class="{ active: collabStore.isScreenSharing }"
      @click.stop="collabStore.toggleScreenShare"
      title="화면 공유"
    >
      <Monitor class="icon" />
    </button>
    
    <button class="pill-btn" title="채팅">
      <MessageCircle class="icon" />
      <span v-if="collabStore.hasUnreadMessages" class="unread-badge"></span>
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
  transition: all 0.2s ease;
}

.collab-pill:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
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
  background: var(--success);
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.divider {
  width: 1px;
  height: 24px;
  background: var(--rose-200);
  margin: 0 0.25rem;
}

.pill-btn {
  position: relative;
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

.pill-btn.active {
  background: var(--rose-100);
  color: var(--rose-600);
}

.icon {
  width: 14px;
  height: 14px;
}

.unread-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 8px;
  height: 8px;
  background: var(--rose-500);
  border-radius: 50%;
  border: 1px solid white;
}
</style>
