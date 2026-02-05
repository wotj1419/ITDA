<script setup lang="ts">
import type { TimelineClip } from '../../types/ui'
import { X } from 'lucide-vue-next'
import LazyVideo from '../media/LazyVideo.vue'

interface Props {
  clip: TimelineClip
  draggable?: boolean
  active?: boolean
  previewing?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  draggable: true,
  previewing: false,
})

const emit = defineEmits<{
  (e: 'remove'): void
  (e: 'select'): void
  (e: 'hover-start'): void
  (e: 'hover-end'): void
}>()

</script>

<template>
  <div
    class="clip-item"
    :class="{
      'is-active': active,
      previewing: props.previewing,
    }"
    :draggable="draggable"
    :style="{ width: `${clip.duration * 20}px` }"
    tabindex="0"
    @click="emit('select')"
    @keydown.enter.prevent="emit('select')"
    @keydown.space.prevent="emit('select')"
    @mouseenter="emit('hover-start')"
    @mouseleave="emit('hover-end')"
  >
    <div
      class="clip-media"
      :class="{ 'has-video': Boolean(clip.videoUrl) }"
    >
      <img
        v-if="clip.thumbnailUrl"
        :src="clip.thumbnailUrl"
        :alt="clip.label"
        class="clip-image"
        loading="lazy"
      />
      <LazyVideo
        v-if="clip.videoUrl && props.previewing"
        :src="clip.videoUrl"
        class="clip-video"
        :poster="clip.thumbnailUrl || '/icon.png'"
        :play-on-hover="false"
        :lazy="false"
        :autoplay="true"
        :loop="true"
      />
      <img
        v-else-if="!clip.thumbnailUrl"
        src="/icon.png"
        :alt="clip.label"
        class="clip-image"
        loading="lazy"
      />
    </div>
    <div class="clip-info">
      <div class="clip-label">{{ clip.label || '확정 클립' }}</div>
      <div class="clip-duration">{{ clip.duration }}초</div>
    </div>

    <button class="clip-remove" @click.stop="emit('remove')">
      <X class="remove-icon" />
    </button>
  </div>
</template>

<style scoped>
.clip-item {
  flex-shrink: 0;
  background: white;
  border: 2px solid var(--rose-300);
  border-radius: 8px;
  overflow: hidden;
  cursor: grab;
  position: relative;
  transition: box-shadow 0.2s, transform 0.2s;
}

.clip-item:active {
  cursor: grabbing;
}

.clip-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.clip-item.dragging {
  opacity: 0.6;
  transform: scale(1.02);
}

.clip-item.is-active {
  border-color: var(--rose-500);
  box-shadow: 0 0 0 2px var(--rose-200);
}

.clip-media {
  position: relative;
  width: 100%;
  height: 48px;
}

.clip-image,
.clip-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.clip-video {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.clip-item.previewing .clip-video,
.clip-item:focus-visible .clip-video {
  opacity: 1;
}

.clip-item.previewing .clip-image,
.clip-item:focus-visible .clip-image {
  opacity: 0;
}

.clip-info {
  padding: 4px 8px;
  background: var(--rose-100);
  font-size: 0.625rem;
  color: var(--gray-700);
}

.clip-label {
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.clip-duration {
  color: var(--gray-500);
}



.clip-remove {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 16px;
  height: 16px;
  background: rgba(0, 0, 0, 0.5);
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.clip-item:hover .clip-remove {
  opacity: 1;
}

.clip-remove:hover {
  background: rgba(220, 38, 38, 0.8);
}

.remove-icon {
  width: 10px;
  height: 10px;
  color: white;
}
</style>
