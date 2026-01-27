<script setup lang="ts">
import { computed } from 'vue'
import { getSmoothStepPath } from '@vue-flow/core'
import type { EdgeProps } from '@vue-flow/core'

/**
 * FlowingEdge
 * - Solid base path + a moving "spark" highlight path on top (reference.html vibe).
 * - Styling lives in global `src/assets/styles/node-canvas.css` (theme variables).
 */

const props = defineProps<EdgeProps>()

const edgePath = computed(() => {
  const [path] = getSmoothStepPath({
    sourceX: props.sourceX,
    sourceY: props.sourceY,
    sourcePosition: props.sourcePosition,
    targetX: props.targetX,
    targetY: props.targetY,
    targetPosition: props.targetPosition,
    borderRadius: 16,
  })

  return path
})

const flowSeed = computed(() => {
  const str = String(props.id ?? '')
  let hash = 0
  for (let i = 0; i < str.length; i += 1) {
    hash = (hash * 31 + str.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
})

const flowDelay = computed(() => `-${flowSeed.value % 2400}ms`)
const breatheDuration = computed(() => `${3200 + (flowSeed.value % 1200)}ms`)
</script>

<template>
  <g
    class="edge-flow"
    :style="{
      '--edge-flow-delay': flowDelay,
      '--edge-breathe-duration': breatheDuration,
    }"
  >
    <path class="vue-flow__edge-path edge-flow__tube" :d="edgePath" fill="none" />
    <path class="edge-flow__core" :d="edgePath" fill="none" />
    <path class="edge-flow__glow" :d="edgePath" fill="none" />
  </g>
</template>
