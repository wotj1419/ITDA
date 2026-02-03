<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  src?: string
  alt?: string
  size?: 'sm' | 'md' | 'lg'
  fallback?: string
  showMore?: number
  useEmoji?: boolean
  userId?: number // 사용자 고유 ID로 이모지 결정
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  alt: 'Avatar',
  useEmoji: true,
})

// 귀여운 동물 이모지 목록
const AVATAR_EMOJIS = [
  '🐱', '🐶', '🐰', '🦊', '🐻', '🐼', '🐨', '🦁',
  '🐯', '🐮', '🐷', '🐸', '🐵', '🐔', '🐧', '🦄',
  '🐹', '🐝', '🦋', '🐢', '🐙', '🦀', '🐳', '🦩',
]

// userId 기반으로 이모지 선택 (userId가 없으면 이름 해시 사용)
function getEmoji(userId?: number, name?: string): string {
  // userId가 있으면 userId로 선택 (1~24번 유저는 각각 다른 이모지)
  if (userId !== undefined && userId > 0) {
    const index = (userId - 1) % AVATAR_EMOJIS.length
    return AVATAR_EMOJIS[index] ?? '🐱'
  }
  // userId 없으면 이름 해시로 폴백
  if (!name) return AVATAR_EMOJIS[0] ?? '🐱'
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = ((hash << 5) - hash) + name.charCodeAt(i)
    hash = hash & hash
  }
  const index = Math.abs(hash) % AVATAR_EMOJIS.length
  return AVATAR_EMOJIS[index] ?? '🐱'
}

const classes = computed(() => [
  'avatar',
  `avatar-${props.size}`,
])

const emoji = computed(() => getEmoji(props.userId, props.alt || ''))

const initials = computed(() => {
  if (props.fallback) return props.fallback
  if (props.alt) {
    return props.alt
      .split(' ')
      .map((word) => word[0])
      .slice(0, 2)
      .join('')
      .toUpperCase()
  }
  return '?'
})

const showEmoji = computed(() => props.useEmoji && !props.src && !props.showMore)
</script>

<template>
  <div :class="classes">
    <img
      v-if="src"
      :src="src"
      :alt="alt"
      class="avatar-image"
    />
    <span v-else-if="showMore" class="avatar-more">+{{ showMore }}</span>
    <span v-else-if="showEmoji" class="avatar-emoji">{{ emoji }}</span>
    <span v-else class="avatar-fallback">{{ initials }}</span>
  </div>
</template>

<style scoped>
.avatar {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--rose-100);
  color: var(--rose-600);
  font-weight: 600;
  overflow: hidden;
  flex-shrink: 0;
}

/* Sizes */
.avatar-sm {
  width: 28px;
  height: 28px;
  font-size: 0.625rem;
}

.avatar-md {
  width: 36px;
  height: 36px;
  font-size: 0.75rem;
}

.avatar-lg {
  width: 48px;
  height: 48px;
  font-size: 1rem;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  text-transform: uppercase;
}

.avatar-emoji {
  font-size: 1em;
  line-height: 1;
}

.avatar-sm .avatar-emoji {
  font-size: 0.9rem;
}

.avatar-md .avatar-emoji {
  font-size: 1.1rem;
}

.avatar-lg .avatar-emoji {
  font-size: 1.5rem;
}

.avatar-more {
  font-size: 0.625rem;
  color: var(--gray-500);
  background: var(--gray-100);
}
</style>
