<script setup lang="ts">
import { computed } from 'vue';
import type { CollabParticipant } from '../../types/ui';
import { MicOff } from 'lucide-vue-next'
import { resolveApiUrl } from '../../services/api/urls';

interface Props {
  participant: CollabParticipant;
  isMe?: boolean;
  isSpeaking?: boolean;
  isMuted?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  isMe: false,
  isSpeaking: false,
  isMuted: false,
});

const resolvedAvatarUrl = computed(() => resolveApiUrl(props.participant.avatarUrl));
</script>

<template>
  <div class="participant">
    <div :class="['avatar', { me: props.isMe, speaking: props.isSpeaking }]">
      <img
        v-if="resolvedAvatarUrl"
        :src="resolvedAvatarUrl || ''"
        :alt="props.participant.name"
      />
      <span v-else>{{ props.participant.name.substring(0, 2) }}</span>
    </div>
    <div class="mute-slot">
      <div v-if="props.isMuted" class="mute-button active" title="음소거됨">
        <MicOff class="mute-icon" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.participant {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.02rem;
  position: relative;
}

.mute-slot {
  height: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
  position: relative;
  overflow: visible;
}

.mute-slot .mute-button {
  position: absolute;
  top: 2px;
}

.avatar {
  width: 48px;
  height: 48px;
  background: var(--rose-100);
  border: 2px solid transparent;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--rose-600);
  overflow: hidden;
  position: relative;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.avatar.me {
  background: var(--rose-500);
  border-color: transparent;
  color: white;
}

.avatar.speaking {
  border-color: #22c55e;
  box-shadow: 0 0 0 5px rgba(34, 197, 94, 0.18);
  animation: speakingPulse 1.6s ease-in-out infinite;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mute-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 999px;
  border: 1px solid var(--rose-200);
  background: white;
  box-shadow: 0 2px 4px rgba(255, 133, 161, 0.12);
  transition: all 0.2s ease;
  padding: 0;
}

.mute-button.active {
  background: var(--rose-100);
  border-color: var(--rose-300);
}

.mute-icon {
  width: 12px;
  height: 12px;
  color: var(--rose-500);
}

@keyframes speakingPulse {
  0% {
    box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.12);
  }
  50% {
    box-shadow: 0 0 0 7px rgba(34, 197, 94, 0.2);
  }
  100% {
    box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.12);
  }
}
</style>
