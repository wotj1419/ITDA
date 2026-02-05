<script setup lang="ts">
import { ref } from 'vue'
import type { TimelineClip } from '../../types/ui'
import ClipItem from './ClipItem.vue'

interface Props {
  clips: TimelineClip[]
  selectedClipId?: string | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'reorder', clipIds: string[]): void
  (e: 'remove', clipId: string): void
  (e: 'select', clipId: string): void
}>()

const draggedId = ref<string | null>(null)
const dragOverId = ref<string | null>(null)

function handleDragStart(clipId: string, event: DragEvent) {
  draggedId.value = clipId
  if (event.target instanceof HTMLElement) {
    event.target.classList.add('dragging')
  }
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', clipId)
  }
}

function handleDragEnd(event: DragEvent) {
  if (event.target instanceof HTMLElement) {
    event.target.classList.remove('dragging')
  }
  draggedId.value = null
  dragOverId.value = null
}

function handleDragOver(clipId: string, event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  dragOverId.value = clipId
}

function handleDragLeave() {
  dragOverId.value = null
}

function handleDrop(targetClipId: string) {
  if (!draggedId.value || draggedId.value === targetClipId) {
    dragOverId.value = null
    return
  }

  // Calculate new order
  const clipIds = props.clips.map((c) => c.clipId)
  const draggedIndex = clipIds.indexOf(draggedId.value)
  const targetIndex = clipIds.indexOf(targetClipId)

  if (draggedIndex !== -1 && targetIndex !== -1) {
    clipIds.splice(draggedIndex, 1)
    clipIds.splice(targetIndex, 0, draggedId.value)
    emit('reorder', clipIds)
  }

  dragOverId.value = null
}

function handleRemove(clipId: string) {
  emit('remove', clipId)
}

function handleSelect(clipId: string) {
  emit('select', clipId)
}
</script>

<template>
  <div class="video-track">
    <TransitionGroup name="clip-list" tag="div" class="track-content">
      <ClipItem
        v-for="clip in clips"
        :key="clip.clipId"
        :clip="clip"
        :active="props.selectedClipId === clip.clipId"
        :class="{ 'drag-over': dragOverId === clip.clipId }"
        @dragstart="handleDragStart(clip.clipId, $event)"
        @dragend="handleDragEnd"
        @dragover="handleDragOver(clip.clipId, $event)"
        @dragleave="handleDragLeave"
        @drop="handleDrop(clip.clipId)"
        @remove="handleRemove(clip.clipId)"
        @select="handleSelect(clip.clipId)"
      />
    </TransitionGroup>

    <div v-if="clips.length === 0" class="track-empty">
      <span class="empty-icon">🎬</span>
      <span>확정된 클립이 없습니다</span>
    </div>
  </div>
</template>

<style scoped>
.video-track {
  position: relative; /* Establish positioning context for .track-empty */
  min-height: 80px;
  /* background & border handled by parent container */
  padding: 8px 0; /* Vertical padding only */
  /* overflow-x handled by parent */
}

.track-content {
  display: flex;
  gap: 0;
  width: max-content;
}

.drag-over {
  border-color: var(--rose-500) !important;
  box-shadow: 0 0 0 2px var(--rose-200);
}

.track-empty {
  position: absolute;
  left: 0;
  top: 8px;
  width: 100%; /* Fill the container width */
  /* If width is wider than screen, it might look off, but user specialized "30 second mark" */
  
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  height: 64px;
  border: 2px dashed var(--rose-200);
  border-radius: 8px;
  font-size: 0.75rem;
  color: var(--gray-400);
  background-color: var(--white); /* Ensure it covers anything behind if needed */
  z-index: 10;
}

.empty-icon {
  font-size: 1.5rem;
}

/* Transition animations */
.clip-list-move,
.clip-list-enter-active,
.clip-list-leave-active {
  transition: all 0.3s ease;
}

.clip-list-enter-from,
.clip-list-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

.clip-list-leave-active {
  position: absolute;
}
</style>
