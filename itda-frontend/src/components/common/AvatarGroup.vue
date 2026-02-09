<script setup lang="ts">
import { computed } from 'vue'
import { resolveApiUrl } from '../../services/api/urls'
interface AvatarItem {
  src?: string
  alt?: string
  fallback?: string
  title?: string
  userId?: number // 사용자 고유 ID
  onClick?: () => void
}

interface Props {
  avatars: AvatarItem[]
  max?: number
  size?: 'sm' | 'md' | 'lg'
  useEmoji?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  max: 3,
  size: 'sm',
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
  if (userId !== undefined && userId > 0) {
    const index = (userId - 1) % AVATAR_EMOJIS.length
    return AVATAR_EMOJIS[index] ?? '🐱'
  }
  if (!name) return AVATAR_EMOJIS[0] ?? '🐱'
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = ((hash << 5) - hash) + name.charCodeAt(i)
    hash = hash & hash
  }
  const index = Math.abs(hash) % AVATAR_EMOJIS.length
  return AVATAR_EMOJIS[index] ?? '🐱'
}

const visibleAvatars = computed(() => props.avatars.slice(0, props.max))
const remaining = computed(() => props.avatars.length - props.max)

const resolveAvatarSrc = (src?: string) => resolveApiUrl(src)
const getAvatarEmoji = (avatar: AvatarItem) => getEmoji(avatar.userId, avatar.alt || avatar.fallback || '')
const shouldShowEmoji = (avatar: AvatarItem) => props.useEmoji && !resolveAvatarSrc(avatar.src)
</script>

<template>
  <div class="avatar-group">
    <component
      v-for="(avatar, index) in visibleAvatars"
      :key="index"
      :is="avatar.onClick ? 'button' : 'div'"
      :type="avatar.onClick ? 'button' : undefined"
      :class="['avatar', `avatar-${size}`, { 'avatar-clickable': !!avatar.onClick }]"
      :style="{ zIndex: visibleAvatars.length - index }"
      :data-tooltip="avatar.title"
      :aria-label="avatar.title"
      @click="avatar.onClick && avatar.onClick()"
    >
      <img
        v-if="resolveAvatarSrc(avatar.src)"
        :src="resolveAvatarSrc(avatar.src) || ''"
        :alt="avatar.alt || 'Avatar'"
        class="avatar-image"
      />
      <span v-else-if="shouldShowEmoji(avatar)" class="avatar-emoji">
        {{ getAvatarEmoji(avatar) }}
      </span>
      <span v-else class="avatar-fallback">
        {{ avatar.fallback || (avatar.alt?.[0] || '?').toUpperCase() }}
      </span>
    </component>
    <div
      v-if="remaining > 0"
      :class="['avatar', `avatar-${size}`, 'avatar-more']"
    >
      +{{ remaining }}
    </div>
  </div>
</template>

<style scoped>
.avatar-group {
  display: flex;
  align-items: center;
  overflow: visible;
}

.avatar-group .avatar {
  margin-left: -8px;
  border: 2px solid white;
  position: relative;
}

.avatar-group .avatar:first-child {
  margin-left: 0;
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--rose-100);
  color: var(--rose-600);
  font-weight: 600;
  overflow: visible;
  flex-shrink: 0;
  border: none;
  padding: 0;
  position: relative;
}

.avatar-clickable {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.avatar-clickable:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12);
}

.avatar[data-tooltip]::after {
  content: attr(data-tooltip);
  position: absolute;
  left: 50%;
  bottom: calc(100% + 10px);
  transform: translate(-50%, 6px);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(255, 245, 249, 0.95));
  color: var(--gray-800);
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.35rem 0.6rem;
  border-radius: 999px;
  border: 1px solid rgba(255, 133, 161, 0.25);
  box-shadow: 0 8px 18px rgba(255, 133, 161, 0.22);
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.18s ease, transform 0.18s ease;
  z-index: 20;
}

.avatar[data-tooltip]::before {
  content: "";
  position: absolute;
  left: 50%;
  bottom: calc(100% + 4px);
  transform: translateX(-50%);
  border-width: 6px;
  border-style: solid;
  border-color: rgba(255, 255, 255, 0.95) transparent transparent transparent;
  opacity: 0;
  transition: opacity 0.18s ease;
  z-index: 19;
}

.avatar[data-tooltip]:hover::after,
.avatar[data-tooltip]:focus-visible::after {
  opacity: 1;
  transform: translate(-50%, 0);
}

.avatar[data-tooltip]:hover::before,
.avatar[data-tooltip]:focus-visible::before {
  opacity: 1;
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
  border-radius: 50%;
}

.avatar-more {
  background: var(--gray-100);
  color: var(--gray-500);
  font-size: 0.625rem;
}

.avatar-emoji {
  font-size: 0.9rem;
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
</style>
