<script setup lang="ts">
import { computed } from 'vue'
import { getSmoothStepPath } from '@vue-flow/core'
import type { EdgeProps } from '@vue-flow/core'

/**
 * FlowingEdge
 * - Renders a solid base path + a moving "spark" highlight path on top.
 * - Visual styling lives in `src/assets/styles/node-canvas.css` so the theme stays consistent.
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

const gradientId = computed(() => `edge-flow-gradient-${props.id}`)
</script>

<template>
  <!-- Vue Flow wraps this in `.vue-flow__edge`; edge-level classes (confirmed/transition) are applied there -->
  <g class="edge-flow">
    <defs>
      <!-- Use CSS for stop colors so we can theme via CSS variables per edge state -->
      <linearGradient
        :id="gradientId"
        gradientUnits="userSpaceOnUse"
        :x1="sourceX"
        :y1="sourceY"
        :x2="targetX"
        :y2="targetY"
      >
        <stop offset="0%" class="edge-flow__stop edge-flow__stop--0" />
        <stop offset="40%" class="edge-flow__stop edge-flow__stop--1" />
        <stop offset="50%" class="edge-flow__stop edge-flow__stop--2" />
        <stop offset="60%" class="edge-flow__stop edge-flow__stop--1" />
        <stop offset="100%" class="edge-flow__stop edge-flow__stop--0" />
      </linearGradient>
    </defs>

    <!-- Base line -->
    <path
      class="vue-flow__edge-path edge-flow__base"
      :d="edgePath"
      fill="none"
    />

    <!-- Moving highlight -->
    <path
      class="edge-flow__spark"
      :d="edgePath"
      fill="none"
      :stroke="`url(#${gradientId})`"
    />
  </g>
</template>
