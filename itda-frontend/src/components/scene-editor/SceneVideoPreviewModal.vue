<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { SCENE_VIDEO_PREVIEW_MODAL_ID } from '../../constants/ui'
import { useUIStore } from '../../stores/ui'
import ModalBase from '../common/ModalBase.vue'

interface SceneVideoPreviewPayload {
  nodeId?: string
  title?: string
  videoUrl?: string | null
  posterUrl?: string | null
}

const uiStore = useUIStore()
const videoRef = ref<HTMLVideoElement | null>(null)

const isOpen = computed(() => uiStore.activeModal === SCENE_VIDEO_PREVIEW_MODAL_ID)

const payload = computed<SceneVideoPreviewPayload>(() => {
  const raw = uiStore.modalData
  if (!raw || typeof raw !== 'object') return {}
  return raw as SceneVideoPreviewPayload
})

const videoUrl = computed(() => payload.value.videoUrl || null)
const posterUrl = computed(() => payload.value.posterUrl || null)
const modalTitle = computed(() => payload.value.title || '영상 미리보기')

function stopPlayback(): void {
  if (!videoRef.value) return
  videoRef.value.pause()
  videoRef.value.currentTime = 0
}

async function playOnOpen(): Promise<void> {
  if (!isOpen.value || !videoUrl.value) return
  await nextTick()
  if (!videoRef.value) return
  try {
    await videoRef.value.play()
  } catch {
    // ignore autoplay rejection
  }
}

function handleClose(): void {
  stopPlayback()
}

watch(isOpen, (open) => {
  if (open) {
    void playOnOpen()
    return
  }
  stopPlayback()
})

watch(videoUrl, () => {
  if (!isOpen.value) return
  void playOnOpen()
})
</script>

<template>
  <ModalBase
    :modal-id="SCENE_VIDEO_PREVIEW_MODAL_ID"
    :title="modalTitle"
    size="lg"
    @close="handleClose"
  >
    <div class="player">
      <video
        v-if="videoUrl"
        ref="videoRef"
        class="player-video"
        :src="videoUrl"
        :poster="posterUrl || undefined"
        controls
        preload="metadata"
        playsinline
      />
      <div v-else class="player-empty">
        재생 가능한 영상이 없습니다.
      </div>
    </div>
  </ModalBase>
</template>

<style scoped>
.player {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.player-video {
  width: 100%;
  max-height: 420px;
  border-radius: 12px;
  background: var(--gray-950);
}

.player-empty {
  min-height: 180px;
  padding: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--gray-400);
  font-size: 0.875rem;
}
</style>
