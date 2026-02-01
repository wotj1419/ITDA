<script setup lang="ts">
import { computed } from 'vue'
interface AvatarItem {
  src?: string
  alt?: string
  fallback?: string
  title?: string
  onClick?: () => void
}

interface Props {
  avatars: AvatarItem[]
  max?: number
  size?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  max: 3,
  size: 'sm',
})

const visibleAvatars = computed(() => props.avatars.slice(0, props.max))
const remaining = computed(() => props.avatars.length - props.max)
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
      :title="avatar.title"
      @click="avatar.onClick && avatar.onClick()"
    >
      <img
        v-if="avatar.src"
        :src="avatar.src"
        :alt="avatar.alt || 'Avatar'"
        class="avatar-image"
      />
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
  overflow: hidden;
  flex-shrink: 0;
  border: none;
  padding: 0;
}

.avatar-clickable {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.avatar-clickable:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12);
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

.avatar-more {
  background: var(--gray-100);
  color: var(--gray-500);
  font-size: 0.625rem;
}
</style>
