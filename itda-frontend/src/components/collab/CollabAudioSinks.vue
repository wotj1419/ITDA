<script setup lang="ts">
import { computed } from 'vue'
import { useCollabStore } from '../../stores/collab'

const collabStore = useCollabStore()

const streams = computed(() => Object.entries(collabStore.remoteStreams))

const setAudioRef =
  (peerId: string, stream: MediaStream) =>
  (el: HTMLAudioElement | null) => {
    if (!el) return
    if (el.srcObject !== stream) {
      el.srcObject = stream
    }
    el.autoplay = true
    el.playsInline = true
    el.volume = 1
  }
</script>

<template>
  <div style="display: none">
    <audio
      v-for="[peerId, stream] in streams"
      :key="peerId"
      :ref="setAudioRef(peerId, stream)"
    />
  </div>
</template>

