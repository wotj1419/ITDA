<script setup lang="ts">
/**
 * MiniTimeline - 확정된 비디오 트랙 요약 표시
 */
import { ref } from 'vue';
import type { TimelineClip } from '../../types/ui';
import { Star, Play, X } from 'lucide-vue-next';
import LazyVideo from '../media/LazyVideo.vue';
import { useHoverPreviewPolicy } from '../../composables/useHoverPreviewPolicy';

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
const thumbStates = ref<Record<string, 'idle' | 'loaded' | 'failed'>>({});
const {
  clearPreview,
  isPreviewing,
  startPreview: schedulePreviewStart,
  stopPreview,
} = useHoverPreviewPolicy({ delayMs: 150 });

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

const shouldMountVideo = (clip: TimelineClip) =>
  Boolean(clip.videoUrl) && isPreviewing(clip.clipId);

function startPreview(clip: TimelineClip) {
  if (!clip.videoUrl) return;
  schedulePreviewStart(clip.clipId);
}

function stopPreviewById(clipId: string) {
  stopPreview(clipId);
}

function handleDragStart(clipId: string, event: DragEvent) {
  clearPreview();
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
</script>

<template>
  <div class="mini-timeline" :class="{ 'is-expanded': clips.length > 0 }">
    <!-- Label -->
    <div class="timeline-label">
      <Star class="label-icon" />
      <span>확정</span>
    </div>

    <!-- Clips -->
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
        @mouseleave="stopPreviewById(clip.clipId)"
        @dragstart="handleDragStart(clip.clipId, $event)"
        @dragend="handleDragEnd"
        @dragover="handleDragOver(clip.clipId, $event)"
        @dragleave="handleDragLeave"
        @drop="handleDrop(clip.clipId)"
      >
        <button
          type="button"
          class="clip-remove"
          draggable="false"
          @pointerdown.stop
          @click.stop="handleRemove(clip.clipId)"
        >
          <X class="remove-icon" />
        </button>
        <div class="clip-media">
          <img
            v-if="clip.thumbnailUrl"
            :src="clip.thumbnailUrl"
            :alt="clip.label || '??? ???'"
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
            ?앹꽦 ???
          </div>
        </div>
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

    <!-- Timeline Play -->
    <button class="timeline-play" @click="handlePlay">
      <Play class="link-icon" />
      재생
    </button>
  </div>
</template>

<style scoped>
/* ==========================================================================
   Container
   ========================================================================== */

.mini-timeline {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  height: 40px;
  padding: 0 1.5rem;
  background: white;
  border-top: 1px solid var(--gray-100);
  position: relative;
  z-index: 20;
  transition: height 0.2s ease, padding 0.2s ease;
}

.mini-timeline.is-expanded {
  height: 72px;
  padding: 0.25rem 1.5rem;
}

/* ==========================================================================
   Label
   ========================================================================== */

.timeline-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 10px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.2em;
  color: var(--rose-500);
  white-space: nowrap;
}

.label-icon {
  width: 14px;
  height: 14px;
  fill: currentColor;
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
  align-items: center;
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

.mini-timeline.is-expanded .timeline-clip {
  height: 56px;
  width: 74px;
}

.timeline-clip.drag-over {
  box-shadow: 0 0 0 2px var(--rose-300);
}

.timeline-clip:hover {
  transform: scale(1.05);
}

.clip-media {
  position: relative;
  width: 100%;
  height: 100%;
  z-index: 1;
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
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--gray-400);
  background: var(--gray-50);
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
  z-index: 2;
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
  z-index: 3;
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
  font-size: 10px;
  color: var(--gray-300);
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

.link-icon {
  width: 14px;
  height: 14px;
}
</style>
