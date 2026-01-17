<script setup lang="ts">
interface Props {
  maxTime: number
  tickInterval?: number
}

const props = withDefaults(defineProps<Props>(), {
  tickInterval: 5,
})

// Generate tick marks
const ticks = computed(() => {
  const result = []
  for (let i = 0; i <= props.maxTime; i += props.tickInterval) {
    result.push(i)
  }
  return result
})

import { computed } from 'vue'

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}
</script>

<template>
  <div class="time-ruler">
    <div
      v-for="tick in ticks"
      :key="tick"
      class="ruler-tick"
      :style="{ left: `${(tick / maxTime) * 100}%` }"
    >
      <span class="tick-label">{{ formatTime(tick) }}</span>
    </div>
  </div>
</template>

<style scoped>
.time-ruler {
  position: relative;
  height: 24px;
  background: linear-gradient(to right, var(--rose-100), var(--rose-50));
  border-radius: 4px;
  margin-bottom: 8px;
}

.ruler-tick {
  position: absolute;
  top: 0;
  height: 100%;
  border-left: 1px solid var(--rose-200);
}

.ruler-tick::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 1px;
  height: 8px;
  background: var(--rose-300);
}

.tick-label {
  position: absolute;
  top: 8px;
  left: 4px;
  font-size: 0.625rem;
  color: var(--gray-500);
  white-space: nowrap;
}
</style>
