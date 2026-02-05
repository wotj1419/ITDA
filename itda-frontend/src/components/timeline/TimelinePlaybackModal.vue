<script setup lang="ts">
import { computed, ref, watch, nextTick, onMounted, onUnmounted } from 'vue';
import { Play, Pause, SkipBack, SkipForward } from 'lucide-vue-next';
import type { TimelineClip } from '../../types/ui';
import { useUIStore } from '../../stores/ui';
import { TIMELINE_PLAYBACK_MODAL_ID } from '../../constants/ui';
import ModalBase from '../common/ModalBase.vue';

interface Props {
  clips: TimelineClip[];
}

const props = defineProps<Props>();
const uiStore = useUIStore();

const videoRef = ref<HTMLVideoElement | null>(null);
const currentIndex = ref(0);
const isPlaying = ref(false);
const isLastClipEnded = ref(false); // 마지막 클립 종료 상태

const isOpen = computed(() => uiStore.activeModal === TIMELINE_PLAYBACK_MODAL_ID);
const currentClip = computed(() => props.clips[currentIndex.value]);

function showPlaybackError(message: string): void {
  uiStore.showToast({
    type: 'error',
    title: '재생 불가',
    message,
  });
}

function findNextPlayableIndex(startIndex: number): number {
  for (let i = startIndex; i < props.clips.length; i += 1) {
    const clip = props.clips[i];
    if (clip?.videoUrl) {
      return i;
    }
    showPlaybackError(`${clip?.label || '클립'}: 영상 URL이 없습니다.`);
  }
  return -1;
}

async function playClipAt(index: number): Promise<void> {
  isLastClipEnded.value = false;
  const playableIndex = findNextPlayableIndex(index);
  if (playableIndex === -1) {
    isPlaying.value = false;
    return;
  }

  currentIndex.value = playableIndex;
  await nextTick();
  if (!videoRef.value) return;

  try {
    await videoRef.value.play();
    isPlaying.value = true;
  } catch (error) {
    console.error('Failed to play video:', error);
    showPlaybackError('영상 재생에 실패했습니다.');
    isPlaying.value = false;
  }
}

async function togglePlay(): Promise<void> {
  if (!props.clips.length) {
    showPlaybackError('재생 가능한 영상이 없습니다.');
    return;
  }

  if (isPlaying.value) {
    videoRef.value?.pause();
    return;
  }

  if (!currentClip.value?.videoUrl) {
    await playClipAt(currentIndex.value);
    return;
  }

  try {
    await videoRef.value?.play();
    isPlaying.value = true;
  } catch (error) {
    console.error('Failed to play video:', error);
    showPlaybackError('영상 재생에 실패했습니다.');
    isPlaying.value = false;
  }
}

async function playNext(): Promise<void> {
  await playClipAt(currentIndex.value + 1);
}

async function playPrev(): Promise<void> {
  const prevIndex = Math.max(currentIndex.value - 1, 0);
  await playClipAt(prevIndex);
}

function handleEnded(): void {
  if (currentIndex.value >= props.clips.length - 1) {
    isPlaying.value = false;
    isLastClipEnded.value = true; // 마지막 클립 종료 상태로 설정
    return;
  }
  void playNext();
}

function handleReplay(): void {
  isLastClipEnded.value = false;
  currentIndex.value = 0;
  void playClipAt(0);
}

function handleClose(): void {
  if (document.fullscreenElement === videoRef.value) {
    void document.exitFullscreen().catch((error) => {
      console.error('Failed to exit fullscreen:', error);
    });
  }
  isPlaying.value = false;
  isLastClipEnded.value = false;
  videoRef.value?.pause();
}

async function toggleFullscreen(): Promise<void> {
  if (!videoRef.value) return;

  if (document.fullscreenElement === videoRef.value) {
    try {
      await document.exitFullscreen();
    } catch (error) {
      console.error('Failed to exit fullscreen:', error);
    }
    return;
  }

  if (document.fullscreenElement) {
    try {
      await document.exitFullscreen();
    } catch (error) {
      console.error('Failed to switch fullscreen target:', error);
      return;
    }
  }

  try {
    await videoRef.value.requestFullscreen();
  } catch (error) {
    console.error('Failed to enter fullscreen:', error);
  }
}

function handleKeydown(event: KeyboardEvent): void {
  if (!isOpen.value) return;

  if (event.key === 'Escape') {
    event.preventDefault();
    if (document.fullscreenElement === videoRef.value) {
      void document.exitFullscreen().catch((error) => {
        console.error('Failed to exit fullscreen:', error);
      });
      return;
    }
    uiStore.closeModal();
    return;
  }

  const target = event.target as HTMLElement | null;
  if (
    target &&
    (target instanceof HTMLInputElement ||
      target instanceof HTMLTextAreaElement ||
      target instanceof HTMLSelectElement ||
      target instanceof HTMLButtonElement ||
      target instanceof HTMLAnchorElement ||
      target.isContentEditable)
  ) {
    return;
  }

  if (event.key === 'ArrowLeft') {
    event.preventDefault();
    void playPrev();
    return;
  }

  if (event.key === 'ArrowRight') {
    event.preventDefault();
    void playNext();
    return;
  }

  if (event.code === 'Space' || event.key === ' ') {
    event.preventDefault();
    void togglePlay();
    return;
  }

  if (event.key === 'f' || event.key === 'F') {
    event.preventDefault();
    void toggleFullscreen();
  }
}

watch(isOpen, (open) => {
  if (open) {
    currentIndex.value = 0;
    isLastClipEnded.value = false;
    void playClipAt(0);
    return;
  }
  handleClose();
});

onMounted(() => {
  document.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown);
});
</script>

<template>
  <ModalBase
    :modal-id="TIMELINE_PLAYBACK_MODAL_ID"
    title="타임라인 재생"
    size="lg"
    :close-on-escape="false"
    @close="handleClose"
  >
    <div class="player">
      <template v-if="props.clips.length">
        <div class="player-header">
          <div class="player-title">
            {{ currentClip?.label || '클립 재생' }}
          </div>
          
          <!-- 마지막 클립 종료 시 다시보기/닫기 버튼 -->
          <div v-if="isLastClipEnded" class="player-controls">
            <button class="player-btn player-btn--primary" @click="handleReplay">
              <Play class="player-icon" />
              다시보기
            </button>
            <button class="player-btn" @click="handleClose">
              닫기
            </button>
          </div>
          
          <!-- 일반 재생 컨트롤 -->
          <div v-else class="player-controls">
            <button class="player-btn" @click="playPrev">
              <SkipBack class="player-icon" />
              이전
            </button>
            <button class="player-btn player-btn--primary" @click="togglePlay">
              <component :is="isPlaying ? Pause : Play" class="player-icon" />
              {{ isPlaying ? '일시정지' : '재생' }}
            </button>
            <button class="player-btn" @click="playNext">
              <SkipForward class="player-icon" />
              다음
            </button>
          </div>
        </div>

        <video
          ref="videoRef"
          class="player-video"
          :src="currentClip?.videoUrl"
          :poster="currentClip?.thumbnailUrl"
          controls
          @ended="handleEnded"
          @play="isPlaying = true"
          @pause="isPlaying = false"
        ></video>
      </template>

      <div v-else class="player-empty">
        재생할 영상이 없습니다.
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

.player-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.player-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--gray-700);
}

.player-controls {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.player-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.4rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: 0.5rem;
  border: 1px solid var(--gray-200);
  background: white;
  color: var(--gray-700);
  cursor: pointer;
  transition: all 0.2s ease;
}

.player-btn:hover {
  background: var(--gray-50);
}

.player-btn--primary {
  border-color: var(--rose-200);
  color: var(--rose-600);
  background: var(--rose-50);
}

.player-btn--primary:hover {
  background: var(--rose-100);
}

.player-icon {
  width: 14px;
  height: 14px;
}

.player-video {
  width: 100%;
  max-height: 420px;
  border-radius: 12px;
  background: var(--gray-950);
}

.player-empty {
  padding: 2rem;
  text-align: center;
  font-size: 0.875rem;
  color: var(--gray-400);
}

</style>
