<script setup lang="ts">
import type { TimelineClip } from '../../types/ui'
import { X } from 'lucide-vue-next'

interface Props {
  clip: TimelineClip
  draggable?: boolean
}

withDefaults(defineProps<Props>(), {
  draggable: true,
})

const emit = defineEmits<{
  (e: 'remove'): void
}>()
</script>

<template>
  <div
    class="clip-item"
    :draggable="draggable"
    :style="{ width: `${clip.duration * 20}px` }"
  >
    <img :src="clip.thumbnailUrl" :alt="clip.label" class="clip-thumbnail" />
    <div class="clip-info">
      <div class="clip-label">{{ clip.label || '확정 클립' }}</div>
      <div class="clip-duration">{{ clip.duration }}초</div>
    </div>
    <span class="clip-badge">OK</span>
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

.clip-thumbnail {
  width: 100%;
  height: 48px;
  object-fit: cover;
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

.clip-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  background: var(--success);
  color: white;
  font-size: 0.5rem;
  font-weight: 700;
  padding: 1px 4px;
  border-radius: 2px;
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
