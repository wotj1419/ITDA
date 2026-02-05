<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useIntersectionObserver } from '@vueuse/core'

interface Props {
  src?: string | null
  poster?: string | null
  lazy?: boolean
  playOnHover?: boolean
  autoplay?: boolean
  loop?: boolean
  preload?: 'none' | 'metadata' | 'auto'
}

const props = withDefaults(defineProps<Props>(), {
  lazy: true,
  playOnHover: false,
  autoplay: false,
  loop: false,
  preload: 'metadata',
})

const videoRef = ref<HTMLVideoElement | null>(null)
const isVisible = ref(!props.lazy)

useIntersectionObserver(
  videoRef,
  (entries) => {
    if (!props.lazy) return
    const entry = entries[0]
    if (!entry) return
    isVisible.value = entry.isIntersecting
  },
  { rootMargin: '150px' }
)

const resolvedSrc = computed(() => {
  if (!props.src) return undefined
  if (!props.lazy) return props.src
  return isVisible.value ? props.src : undefined
})

watch(resolvedSrc, (next) => {
  const el = videoRef.value
  if (!el) return
  if (!next) {
    el.removeAttribute('src')
    el.load()
  }
})

const handleMouseEnter = (event: MouseEvent) => {
  if (!props.playOnHover) return
  const video = event.currentTarget as HTMLVideoElement | null
  if (!video) return
  video.play().catch(() => {})
}

const handleMouseLeave = (event: MouseEvent) => {
  if (!props.playOnHover) return
  const video = event.currentTarget as HTMLVideoElement | null
  if (!video) return
  video.pause()
  video.currentTime = 0
}
</script>

<template>
  <video
    ref="videoRef"
    :src="resolvedSrc"
    :poster="poster || undefined"
    muted
    playsinline
    :preload="preload"
    :autoplay="autoplay"
    :loop="loop"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  />
</template>
