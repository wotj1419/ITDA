<script setup lang="ts">
/**
 * MiniTimeline - 확정된 비디오 클립을 보여주는 미니 타임라인
 */
import { ref } from 'vue';
import { RouterLink } from 'vue-router';
import type { TimelineClip } from '../../types/ui';
import { Star, ArrowRight, Play, X } from 'lucide-vue-next';

// =============================================================================
// Props
// =============================================================================

interface Props {
  clips: TimelineClip[];
  totalDuration: number;
  maxDuration?: number;
  projectId: number;
  sceneId?: number;
}

const props = withDefaults(defineProps<Props>(), {
  maxDuration: 60,
});

const emit = defineEmits<{
  (e: 'reorder', clipIds: string[]): void;
  (e: 'remove', clipId: string): void;
  (e: 'play'): void;
}>();

const draggedId = ref<string | null>(null);
const dragOverId = ref<string | null>(null);

function handleDragStart(clipId: string, event: DragEvent) {
  draggedId.value = clipId;
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.setData('text/plain', clipId);
  }
}

function handleDragEnd() {
  draggedId.value = null;
  dragOverId.value = null;
}

function handleDragOver(clipId: string, event: DragEvent) {
  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move';
  }
  dragOverId.value = clipId;
}

function handleDragLeave() {
  dragOverId.value = null;
}

function handleDrop(targetClipId: string) {
  if (!draggedId.value || draggedId.value === targetClipId) {
    dragOverId.value = null;
    return;
  }

  const clipIds = props.clips.map((c) => c.clipId);
  const draggedIndex = clipIds.indexOf(draggedId.value);
  const targetIndex = clipIds.indexOf(targetClipId);

  if (draggedIndex !== -1 && targetIndex !== -1) {
    clipIds.splice(draggedIndex, 1);
    clipIds.splice(targetIndex, 0, draggedId.value);
    emit('reorder', clipIds);
  }

  dragOverId.value = null;
}

function handleRemove(clipId: string) {
  emit('remove', clipId);
}

function handlePlay() {
  emit('play');
}

// =============================================================================
// Computed
// =============================================================================

const progressPercent = Math.min(
  (props.totalDuration / props.maxDuration) * 100,
  100
);

function isVideo(url?: string): boolean {
  if (!url) return false;
  const lower = url.toLowerCase();
  return lower.endsWith('.mp4') || lower.endsWith('.webm') || lower.endsWith('.mov');
}

function handleVideoEnter(event: MouseEvent) {
  const video = event.target as HTMLVideoElement;
  if (video && video.paused) {
    video.play().catch(() => {
      // Auto-play might be blocked or interrupted
    });
  }
}

function handleVideoLeave(event: MouseEvent) {
  const video = event.target as HTMLVideoElement;
  if (video) {
    video.pause();
    video.currentTime = 0; // Reset to start
  }
}
</script>

<template>
  <div class="mini-timeline">
    <!-- Label -->
    <div class="timeline-label">
      <Star class="label-icon" />
      <span>Confirmed</span>
    </div>

    <!-- Clips -->
    <div class="timeline-clips">
      <div
        v-for="clip in clips"
        :key="clip.clipId"
        class="timeline-clip"
        :class="{ 'drag-over': dragOverId === clip.clipId }"
        :title="clip.label || '확정 클립'"
        draggable="true"
        @dragstart="handleDragStart(clip.clipId, $event)"
        @dragend="handleDragEnd"
        @dragover="handleDragOver(clip.clipId, $event)"
        @dragleave="handleDragLeave"
        @drop="handleDrop(clip.clipId)"
      >
        <button class="clip-remove" @click.stop="handleRemove(clip.clipId)">
          <X class="remove-icon" />
        </button>
        <video
          v-if="clip.videoUrl || isVideo(clip.thumbnailUrl)"
          :src="clip.videoUrl || clip.thumbnailUrl"
          class="clip-content clip-video"
          preload="metadata"
          muted
          playsinline
          @mouseenter="handleVideoEnter"
          @mouseleave="handleVideoLeave"
        />
        <img
          v-else
          :src="clip.thumbnailUrl"
          :alt="clip.label || '확정 클립'"
          class="clip-content clip-img"
        />
        <span class="clip-duration">{{ clip.duration }}s</span>
        <span class="clip-duration">{{ clip.duration }}s</span>
      </div>

      <!-- Empty State -->
      <div v-if="clips.length === 0" class="timeline-empty">
        확정된 클립이 없습니다
      </div>
    </div>

    <!-- Progress Bar -->
    <div class="timeline-progress">
      <div
        class="progress-bar"
        :style="{ width: `${progressPercent}%` }"
      />
    </div>

    <!-- Timeline Link -->
    <button class="timeline-play" @click="handlePlay">
      <Play class="link-icon" />
      재생
    </button>

    <RouterLink
      :to="{
        name: 'timeline',
        params: { id: projectId },
        query: sceneId ? { sceneId } : undefined,
      }"
      class="timeline-link"
    >
      <ArrowRight class="link-icon" />
      Scene Timeline
    </RouterLink>
  </div>
</template>

<style scoped>
/* ==========================================================================
   Container
   ========================================================================== */

.mini-timeline {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.5rem;
  background: white;
  border-top: 1px solid var(--rose-100);
}

/* ==========================================================================
   Label
   ========================================================================== */

.timeline-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--rose-500);
  white-space: nowrap;
}

.label-icon {
  width: 16px;
  height: 16px;
}

/* ==========================================================================
   Clips
   ========================================================================== */

.timeline-clips {
  display: flex;
  gap: 0.5rem;
  flex: 1;
  overflow-x: auto;
  padding: 4px 0;
}

.timeline-clip {
  position: relative;
  width: 64px;
  height: 48px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease;
}

.timeline-clip.drag-over {
  box-shadow: 0 0 0 2px var(--rose-300);
}

.timeline-clip:hover {
  transform: scale(1.05);
}

.timeline-clip:hover {
  transform: scale(1.05);
}

.clip-content {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.clip-duration {
  position: absolute;
  bottom: 2px;
  right: 2px;
  font-size: 0.625rem;
  font-weight: 500;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 1px 4px;
  border-radius: 2px;
}

.clip-remove {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  cursor: pointer;
  transition: opacity 0.2s ease, background 0.2s ease;
}

.timeline-clip:hover .clip-remove {
  opacity: 1;
}

.clip-remove:hover {
  background: rgba(220, 38, 38, 0.85);
}

.remove-icon {
  width: 10px;
  height: 10px;
  color: white;
}

.timeline-empty {
  font-size: 0.75rem;
  color: var(--gray-400);
  font-style: italic;
}

/* ==========================================================================
   Progress Bar
   ========================================================================== */

.timeline-progress {
  width: 80px;
  height: 4px;
  background: var(--gray-200);
  border-radius: 2px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--rose-400), var(--rose-500));
  border-radius: 2px;
  transition: width 0.3s ease;
}

/* ==========================================================================
   Timeline Link
   ========================================================================== */

.timeline-link {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--rose-500);
  background: var(--rose-50);
  border-radius: 6px;
  text-decoration: none;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.timeline-play {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--gray-700);
  background: var(--gray-50);
  border: 1px solid var(--gray-200);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.timeline-play:hover {
  background: var(--gray-100);
  color: var(--gray-900);
}

.timeline-link:hover {
  background: var(--rose-100);
  color: var(--rose-600);
}

.link-icon {
  width: 14px;
  height: 14px;
}
</style>
