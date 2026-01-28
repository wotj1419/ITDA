<script setup lang="ts">
import { computed } from 'vue'
import { getStraightPath } from '@vue-flow/core'
import type { EdgeProps } from '@vue-flow/core'

/**
 * FlowingEdge
 * - `sample/reference.html` connector-line 느낌을 SVG path로 재현:
 *   - Base track: light pink line
 *   - Moving highlight: translated gradient band (transparent -> pink -> transparent)
 * - Styling lives in global `src/assets/styles/node-canvas.css`.
 */

const props = defineProps<EdgeProps>()

const edgePath = computed(() => {
  const [path] = getStraightPath({
    sourceX: props.sourceX,
    sourceY: props.sourceY,
    targetX: props.targetX,
    targetY: props.targetY,
  })

  return path
})

const gradientId = computed(() => {
  const raw = String(props.id ?? 'edge')
  const safe = raw.replace(/[^a-zA-Z0-9_-]/g, '_')
  return `edge-flow-gradient-${safe}`
})

const dx = computed(() => props.targetX - props.sourceX)
const dy = computed(() => props.targetY - props.sourceY)

const fromTranslate = computed(() => `${-0.6 * dx.value} ${-0.6 * dy.value}`)
const toTranslate = computed(() => `${dx.value} ${dy.value}`)

</script>

<template>
  <g class="edge-flow">
    <defs>
      <linearGradient
        :id="gradientId"
        gradientUnits="userSpaceOnUse"
        :x1="props.sourceX"
        :y1="props.sourceY"
        :x2="props.targetX"
        :y2="props.targetY"
      >
        <stop offset="0%" stop-color="rgb(255, 77, 141)" stop-opacity="0" />
        <stop offset="30%" stop-color="rgb(255, 77, 141)" stop-opacity="1" />
        <stop offset="60%" stop-color="rgb(255, 77, 141)" stop-opacity="0" />
        <stop offset="100%" stop-color="rgb(255, 77, 141)" stop-opacity="0" />

        <animateTransform
          attributeName="gradientTransform"
          type="translate"
          :from="fromTranslate"
          :to="toTranslate"
          dur="3s"
          repeatCount="indefinite"
          calcMode="spline"
          keyTimes="0;1"
          keySplines="0.42 0 0.58 1"
        />
      </linearGradient>
    </defs>

    <!-- Vue Flow expects a `.vue-flow__edge-path` for interactions/selection. -->
    <path class="vue-flow__edge-path edge-flow__track" :d="edgePath" fill="none" />

    <!-- Moving highlight (reference-like) -->
    <path class="edge-flow__highlight" :d="edgePath" :stroke="`url(#${gradientId})`" fill="none">
      <animate
        attributeName="opacity"
        values="0;1;0"
        keyTimes="0;0.3;1"
        dur="3s"
        repeatCount="indefinite"
        calcMode="spline"
        keySplines="0.42 0 0.58 1;0.42 0 0.58 1"
      />
    </path>
  </g>
</template>
