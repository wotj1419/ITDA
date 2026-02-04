<script setup lang="ts">
import { computed, ref } from 'vue'
import { Play, Pause, Maximize2 } from 'lucide-vue-next'
import { useTimelinePlayback } from '../../composables/useTimelinePlayback'
import type { TimelineClip } from '../../types/ui/timeline'

interface Props {
  clips: TimelineClip[]
}

const props = defineProps<Props>()

const containerRef = ref<HTMLElement | null>(null)

const clipsRef = computed(() => props.clips)
const {
  videoRef,
  currentClip,
  currentElapsedTime,
  totalDuration,
  isPlaying,
  canPlayCurrent,
  togglePlay,
  onEnded,
  onTimeUpdate,
} = useTimelinePlayback({ clips: clipsRef })
void videoRef

const hasClips = computed(() => props.clips.length > 0)
const hasVideo = computed(() => Boolean(currentClip.value?.videoUrl))

function formatTime(seconds: number): string {
  const safeSeconds = Number.isFinite(seconds) ? Math.max(0, Math.floor(seconds)) : 0
  const m = Math.floor(safeSeconds / 60)
  const s = safeSeconds % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

async function toggleFullscreen(): Promise<void> {
  const target = containerRef.value
  if (!target) return
  if (document.fullscreenElement) {
    await document.exitFullscreen().catch(() => {})
    return
  }
  await target.requestFullscreen().catch(() => {})
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.code === 'Space') {
    event.preventDefault()
    void togglePlay()
  }
}
</script>

<template>
  <div class="video-preview" tabindex="0" @keydown="handleKeydown">
    <div ref="containerRef" class="preview-container">
      <template v-if="hasClips">
        <video
          v-if="hasVideo"
          ref="videoRef"
          :src="currentClip?.videoUrl"
          class="preview-video"
          preload="metadata"
          playsinline
          :poster="currentClip?.thumbnailUrl || undefined"
          @ended="onEnded"
          @timeupdate="onTimeUpdate"
          @play="isPlaying = true"
          @pause="isPlaying = false"
        />
        <img
          v-else-if="currentClip?.thumbnailUrl"
          :src="currentClip.thumbnailUrl"
          alt="preview"
          class="preview-image"
        />
        <div v-else class="preview-empty">미리보기 가능한 클립이 없습니다.</div>
      </template>
      <div v-else class="preview-empty">타임라인에 클립이 없습니다.</div>

      <div class="controls">
        <div class="controls-left">
          <button
            class="ctrl-btn primary"
            :disabled="!canPlayCurrent"
            @click="togglePlay"
            aria-label="재생/일시정지"
            title="재생/일시정지"
          >
            <Pause v-if="isPlaying" class="icon" />
            <Play v-else class="icon" />
          </button>
          <div class="timecode">
            <span>{{ formatTime(currentElapsedTime) }}</span>
            <span> / </span>
            <span>{{ formatTime(totalDuration) }}</span>
          </div>
        </div>
        <button class="ctrl-btn" @click="toggleFullscreen" aria-label="전체화면" title="전체화면">
          <Maximize2 class="icon" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.video-preview {
  width: 100%;
  outline: none;
}

.preview-container {
  aspect-ratio: 16 / 9;
  max-width: 700px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  position: relative;
  overflow: hidden;

}

.preview-video,
.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scale(1.26);
  transform-origin: center;
}

.preview-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-300);
  font-size: 0.875rem;
}

.controls {
  position: absolute;
  left: 1rem;
  right: 1rem;
  bottom: 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.controls-left {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.ctrl-btn {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.55);
  color: white;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.ctrl-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.ctrl-btn.primary {
  background: rgba(255, 255, 255, 0.95);
  color: var(--rose-600);
}

.icon {
  width: 16px;
  height: 16px;
}

.timecode {
  background: rgba(0, 0, 0, 0.7);
  padding: 0.25rem 0.625rem;
  border-radius: 6px;
  font-size: 0.75rem;
  color: white;
}
</style>
