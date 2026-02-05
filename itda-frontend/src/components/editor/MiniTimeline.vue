<script setup lang="ts">
/**
 * MiniTimeline - 확정된 비디오 트랙 요약 표시
 */
import { ref } from 'vue';
import type { TimelineClip } from '../../types/ui';
import { Star, Play, X } from 'lucide-vue-next';
import LazyVideo from '../media/LazyVideo.vue';

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
const activePreviewClipId = ref<string | null>(null);
const thumbStates = ref<Record<string, 'idle' | 'loaded' | 'failed'>>({});

const getThumbState = (clipId: string) => thumbStates.value[clipId] ?? 'idle';

const isThumbReady = (clip: TimelineClip) =>
  Boolean(clip.thumbnailUrl) && getThumbState(clip.clipId) === 'loaded';

const handleThumbLoad = (clipId: string, event: Event) => {
  const img = event.target as HTMLImageElement | null;
  if (!img) return;
  const width = img.naturalWidth || 0;
  const height = img.naturalHeight || 0;
  if (width < 2 || height < 2) {
    thumbStates.value[clipId] = 'failed';
    return;
  }
  thumbStates.value[clipId] = 'loaded';
};

const handleThumbError = (clipId: string) => {
  thumbStates.value[clipId] = 'failed';
};

const getPoster = (clip: TimelineClip) =>
  clip.thumbnailUrl || '/icon.png';

const isPreviewing = (clipId: string) => activePreviewClipId.value === clipId;
const shouldMountVideo = (clip: TimelineClip) =>
  Boolean(clip.videoUrl) && isPreviewing(clip.clipId);

function startPreview(clip: TimelineClip) {
  if (!clip.videoUrl) return;
  activePreviewClipId.value = clip.clipId;
}

function stopPreview(clipId: string) {
  if (activePreviewClipId.value === clipId) {
    activePreviewClipId.value = null;
  }
}

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
</script>

<template>
  <div class="mini-timeline" :class="{ 'is-expanded': clips.length > 0 }">
    <!-- Left Controls: Label + Play -->
    <div class="timeline-controls">
      <div class="timeline-label">
        <Star class="label-icon" />
        <span>확정</span>
      </div>

      <button class="timeline-play" @click="handlePlay">
        <Play class="play-icon" />
      </button>
    </div>

    <!-- Divider -->
    <div class="timeline-divider" />

    <!-- Clips Area -->
    <div class="timeline-clips">
      <div
        v-for="clip in clips"
        :key="clip.clipId"
        class="timeline-clip"
        :class="{
          'drag-over': dragOverId === clip.clipId,
          'has-video': Boolean(clip.videoUrl),
          'thumb-ready': isThumbReady(clip),
          previewing: isPreviewing(clip.clipId),
        }"
        :title="clip.label || '확정 클립'"
        draggable="true"
        @mouseenter="startPreview(clip)"
        @mouseleave="stopPreview(clip.clipId)"
        @dragstart="handleDragStart(clip.clipId, $event)"
        @dragend="handleDragEnd"
        @dragover="handleDragOver(clip.clipId, $event)"
        @dragleave="handleDragLeave"
        @drop="handleDrop(clip.clipId)"
      >
        <button class="clip-remove" @click.stop="handleRemove(clip.clipId)">
          <X class="remove-icon" />
        </button>
        <div class="clip-media">
          <img
            v-if="clip.thumbnailUrl"
            :src="clip.thumbnailUrl"
            :alt="clip.label || '확정 클립'"
            class="clip-img"
            @load="handleThumbLoad(clip.clipId, $event)"
            @error="handleThumbError(clip.clipId)"
          />
          <LazyVideo
            v-if="shouldMountVideo(clip)"
            :src="clip.videoUrl"
            class="clip-video"
            :poster="getPoster(clip)"
            :play-on-hover="false"
            :lazy="false"
            :autoplay="true"
            :loop="true"
          />
          <div
            v-else-if="!clip.thumbnailUrl"
            class="clip-placeholder"
          >
            🎬
          </div>
        </div>
        <span class="clip-duration">{{ clip.duration }}s</span>
      </div>

      <!-- Empty State -->
      <div v-if="clips.length === 0" class="timeline-empty">
        <span class="empty-icon">🎞️</span>
        <span class="empty-text">영상을 확정하면 여기에 쌓여요!</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==========================================================================
   Container
   ========================================================================== */

.mini-timeline {
  display: flex;
  align-items: center;
  gap: 0;
  height: 48px;
  padding: 0 1rem;
  background: linear-gradient(180deg, #fefefe 0%, #faf9f9 100%);
  border-top: 1px solid var(--gray-100);
  position: relative;
  z-index: 20;
  transition: height 0.25s ease, padding 0.25s ease;
}

.mini-timeline.is-expanded {
  height: 80px;
  padding: 0.5rem 1rem;
}

/* ==========================================================================
   Left Controls
   ========================================================================== */

.timeline-controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
  padding-right: 1rem;
}

.timeline-label {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--rose-500);
  white-space: nowrap;
  background: linear-gradient(135deg, var(--rose-50) 0%, #fff5f5 100%);
  padding: 0.375rem 0.625rem;
  border-radius: 20px;
  border: 1px solid var(--rose-100);
}

.label-icon {
  width: 12px;
  height: 12px;
  fill: currentColor;
}

/* Play Button */
.timeline-play {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--rose-400) 0%, var(--rose-500) 100%);
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(244, 63, 94, 0.3);
}

.timeline-play:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(244, 63, 94, 0.4);
}

.timeline-play:active {
  transform: scale(0.95);
}

.play-icon {
  width: 14px;
  height: 14px;
  margin-left: 2px;
}

/* ==========================================================================
   Divider
   ========================================================================== */

.timeline-divider {
  width: 1px;
  height: 32px;
  background: linear-gradient(180deg, transparent 0%, var(--gray-200) 50%, transparent 100%);
  margin: 0 0.75rem;
  flex-shrink: 0;
}

/* ==========================================================================
   Clips
   ========================================================================== */

.timeline-clips {
  display: flex;
  gap: 0.625rem;
  flex: 1;
  overflow-x: auto;
  padding: 6px 4px;
  align-items: center;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.timeline-clips::-webkit-scrollbar {
  display: none;
}

.timeline-clip {
  position: relative;
  width: 72px;
  height: 54px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.2s ease;
  border: 2px solid transparent;
  background: var(--gray-100);
}

.mini-timeline.is-expanded .timeline-clip {
  width: 88px;
  height: 66px;
}

.timeline-clip:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  border-color: var(--rose-200);
}

.timeline-clip.drag-over {
  border-color: var(--rose-400);
  box-shadow: 0 0 0 3px rgba(244, 63, 94, 0.2);
}

.clip-media {
  position: relative;
  width: 100%;
  height: 100%;
}

.clip-img,
.clip-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.clip-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  background: linear-gradient(135deg, var(--gray-100) 0%, var(--gray-50) 100%);
}

.clip-img {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.clip-video {
  opacity: 1;
  transition: opacity 0.2s ease;
}

.timeline-clip.thumb-ready .clip-img {
  opacity: 1;
}

.timeline-clip.thumb-ready .clip-video {
  opacity: 0;
}

.timeline-clip.previewing .clip-video {
  opacity: 1;
}

.timeline-clip.previewing .clip-img {
  opacity: 0;
}

/* Clip Duration Badge */
.clip-duration {
  position: absolute;
  bottom: 3px;
  right: 3px;
  font-size: 9px;
  font-weight: 600;
  background: rgba(0, 0, 0, 0.75);
  color: white;
  padding: 1px 5px;
  border-radius: 4px;
  backdrop-filter: blur(4px);
}

/* Remove Button */
.clip-remove {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(4px);
}

.timeline-clip:hover .clip-remove {
  opacity: 1;
}

.clip-remove:hover {
  background: var(--rose-500);
  transform: scale(1.1);
}

.remove-icon {
  width: 10px;
  height: 10px;
  color: white;
}

/* ==========================================================================
   Empty State
   ========================================================================== */

.timeline-empty {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, var(--gray-50) 0%, #f8f8f8 100%);
  border-radius: 12px;
  border: 1px dashed var(--gray-200);
}

.empty-icon {
  font-size: 1.25rem;
}

.empty-text {
  font-size: 11px;
  font-weight: 500;
  color: var(--gray-400);
  white-space: nowrap;
}
</style>
