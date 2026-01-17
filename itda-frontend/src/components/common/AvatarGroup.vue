<script setup lang="ts">
interface AvatarItem {
  src?: string
  alt?: string
  fallback?: string
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

const visibleAvatars = props.avatars.slice(0, props.max)
const remaining = props.avatars.length - props.max
</script>

<template>
  <div class="avatar-group">
    <div
      v-for="(avatar, index) in visibleAvatars"
      :key="index"
      :class="['avatar', `avatar-${size}`]"
      :style="{ zIndex: visibleAvatars.length - index }"
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
    </div>
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
