<script setup lang="ts">
import { ref } from 'vue'
import { Play, Pause } from 'lucide-vue-next'

interface Props {
  thumbnailUrl?: string
  currentTime: number
  totalTime: number
}

const props = defineProps<Props>()

const isPlaying = ref(false)

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = Math.round(seconds % 60)
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

function togglePlay() {
  isPlaying.value = !isPlaying.value
}
</script>

<template>
  <div class="video-preview">
    <div
      class="preview-container"
      :style="thumbnailUrl ? { backgroundImage: `url(${thumbnailUrl})` } : {}"
    >
      <!-- Play Overlay -->
      <div class="play-overlay" @click="togglePlay">
        <div class="play-button">
          <Pause v-if="isPlaying" class="play-icon" />
          <Play v-else class="play-icon" />
        </div>
      </div>

      <!-- Timecode -->
      <div class="timecode">
        <span>{{ formatTime(currentTime) }}</span>
        <span> / </span>
        <span>{{ formatTime(totalTime) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.video-preview {
  width: 100%;
}

.preview-container {
  aspect-ratio: 16 / 9;
  max-width: 700px;
  margin: 0 auto;
  background: #111 center/cover no-repeat;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
}

.play-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition: background 0.2s;
}

.play-overlay:hover {
  background: rgba(0, 0, 0, 0.4);
}

.play-button {
  width: 64px;
  height: 64px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s;
}

.play-overlay:hover .play-button {
  transform: scale(1.1);
}

.play-icon {
  width: 24px;
  height: 24px;
  color: var(--rose-500);
}

.timecode {
  position: absolute;
  bottom: 1rem;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-size: 0.875rem;
  font-family: monospace;
  color: white;
}
</style>
