<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { formatRelativeTime } from '../../utils/date'

interface Props {
  date: string | Date
  refreshInterval?: number // ms
}

const props = withDefaults(defineProps<Props>(), {
  refreshInterval: 10000 // 10 seconds
})

const now = ref(new Date())
let timer: ReturnType<typeof setInterval> | null = null

const displayTime = computed(() => {
  return formatRelativeTime(props.date, now.value)
})

onMounted(() => {
  timer = setInterval(() => {
    now.value = new Date()
  }, props.refreshInterval)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <span>{{ displayTime }}</span>
</template>
