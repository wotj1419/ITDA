import { computed, nextTick, ref, watch, type Ref } from 'vue'
import type { TimelineClip } from '../types/ui/timeline'

type UseTimelinePlaybackArgs = {
  clips: Ref<TimelineClip[]>
}

export function useTimelinePlayback({ clips }: UseTimelinePlaybackArgs) {
  const videoRef = ref<HTMLVideoElement | null>(null)
  const currentIndex = ref(0)
  const isPlaying = ref(false)
  const currentClipTime = ref(0)

  const currentClip = computed(() => clips.value[currentIndex.value] ?? null)
  const totalDuration = computed(() => clips.value.reduce((sum, clip) => sum + clip.duration, 0))
  const currentElapsedTime = computed(() => {
    const before = clips.value
      .slice(0, currentIndex.value)
      .reduce((sum, clip) => sum + clip.duration, 0)
    return before + currentClipTime.value
  })

  const canPlayCurrent = computed(() => Boolean(currentClip.value?.videoUrl))
  const canPrev = computed(() => currentIndex.value > 0)
  const canNext = computed(() => currentIndex.value < clips.value.length - 1)

  const findPlayableIndex = (startIndex: number, step: 1 | -1): number => {
    let index = startIndex
    while (index >= 0 && index < clips.value.length) {
      if (clips.value[index]?.videoUrl) {
        return index
      }
      index += step
    }
    return -1
  }

  const ensurePlayableIndex = (): void => {
    if (clips.value.length === 0) {
      currentIndex.value = 0
      currentClipTime.value = 0
      isPlaying.value = false
      return
    }
    if (currentIndex.value >= clips.value.length) {
      currentIndex.value = clips.value.length - 1
    }
    if (!clips.value[currentIndex.value]?.videoUrl) {
      const fallback = findPlayableIndex(0, 1)
      currentIndex.value = fallback >= 0 ? fallback : 0
    }
  }

  const playCurrent = async (): Promise<void> => {
    if (!canPlayCurrent.value || !videoRef.value) {
      isPlaying.value = false
      return
    }
    try {
      await videoRef.value.play()
      isPlaying.value = true
    } catch {
      isPlaying.value = false
    }
  }

  const pauseCurrent = (): void => {
    videoRef.value?.pause()
    isPlaying.value = false
  }

  const seekTo = async (index: number, autoplay: boolean): Promise<void> => {
    if (index < 0 || index >= clips.value.length) return
    currentIndex.value = index
    currentClipTime.value = 0
    await nextTick()
    if (!videoRef.value) return
    videoRef.value.currentTime = 0
    if (autoplay) {
      await playCurrent()
    } else {
      pauseCurrent()
    }
  }

  const playNext = async (): Promise<void> => {
    const nextIndex = findPlayableIndex(currentIndex.value + 1, 1)
    if (nextIndex < 0) {
      pauseCurrent()
      return
    }
    await seekTo(nextIndex, true)
  }

  const playPrev = async (): Promise<void> => {
    const prevIndex = findPlayableIndex(currentIndex.value - 1, -1)
    if (prevIndex < 0) return
    await seekTo(prevIndex, true)
  }

  const togglePlay = async (): Promise<void> => {
    if (!canPlayCurrent.value) return
    if (isPlaying.value) {
      pauseCurrent()
      return
    }
    await playCurrent()
  }

  const onEnded = async (): Promise<void> => {
    await playNext()
  }

  const onTimeUpdate = (): void => {
    if (!videoRef.value) return
    currentClipTime.value = videoRef.value.currentTime
  }

  watch(
    clips,
    async () => {
      const wasPlaying = isPlaying.value
      ensurePlayableIndex()
      await nextTick()
      if (wasPlaying) {
        await playCurrent()
      }
    },
    { immediate: true, deep: true }
  )

  return {
    videoRef,
    currentIndex,
    currentClip,
    currentElapsedTime,
    totalDuration,
    isPlaying,
    canPrev,
    canNext,
    canPlayCurrent,
    togglePlay,
    playPrev,
    playNext,
    onEnded,
    onTimeUpdate,
  }
}
