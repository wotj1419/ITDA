<script setup lang="ts">
import type { CollabParticipant } from '../../types';

interface Props {
  participant: CollabParticipant;
  isMe?: boolean;
}

withDefaults(defineProps<Props>(), {
  isMe: false,
});
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
