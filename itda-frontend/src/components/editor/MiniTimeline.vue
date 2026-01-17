<script setup lang="ts">
/**
 * MiniTimeline - 확정된 비디오 클립을 보여주는 미니 타임라인
 */
import { RouterLink } from 'vue-router';
import type { TimelineClip } from '../../types';
import { Star, ArrowRight } from 'lucide-vue-next';

// =============================================================================
// Props
// =============================================================================

interface Props {
  clips: TimelineClip[];
  totalDuration: number;
  maxDuration?: number;
  projectId: number;
  sceneId?: number;
}

const props = withDefaults(defineProps<Props>(), {
  maxDuration: 60,
});

// =============================================================================
// Computed
// =============================================================================

const progressPercent = Math.min(
  (props.totalDuration / props.maxDuration) * 100,
  100
);
</script>

<template>
  <div class="mini-timeline">
    <!-- Label -->
    <div class="timeline-label">
      <Star class="label-icon" />
      <span>Confirmed</span>
    </div>

    <!-- Clips -->
    <div class="timeline-clips">
      <div
        v-for="clip in clips"
        :key="clip.clipId"
        class="timeline-clip"
        :title="clip.label"
      >
        <img :src="clip.thumbnailUrl" :alt="clip.label" />
        <span class="clip-duration">{{ clip.duration }}s</span>
      </div>

      <!-- Empty State -->
      <div v-if="clips.length === 0" class="timeline-empty">
        확정된 클립이 없습니다
      </div>
    </div>

    <!-- Progress Bar -->
    <div class="timeline-progress">
      <div
        class="progress-bar"
        :style="{ width: `${progressPercent}%` }"
      />
    </div>

    <!-- Total Duration -->
    <span class="timeline-total">
      {{ totalDuration }}s / {{ maxDuration }}s
    </span>

    <!-- Timeline Link -->
    <RouterLink
      :to="{
        name: 'timeline',
        params: { id: projectId },
        query: sceneId ? { sceneId } : undefined,
      }"
      class="timeline-link"
    >
      <ArrowRight class="link-icon" />
      Scene Timeline
    </RouterLink>
  </div>
</template>

<style scoped>
/* ==========================================================================
   Container
   ========================================================================== */

.mini-timeline {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.5rem;
  background: white;
  border-top: 1px solid var(--rose-100);
}

/* ==========================================================================
   Label
   ========================================================================== */

.timeline-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--rose-500);
  white-space: nowrap;
}

.label-icon {
  width: 16px;
  height: 16px;
}

/* ==========================================================================
   Clips
   ========================================================================== */

.timeline-clips {
  display: flex;
  gap: 0.5rem;
  flex: 1;
  overflow-x: auto;
  padding: 4px 0;
}

.timeline-clip {
  position: relative;
  width: 64px;
  height: 48px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease;
}

.timeline-clip:hover {
  transform: scale(1.05);
}

.timeline-clip img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.clip-duration {
  position: absolute;
  bottom: 2px;
  right: 2px;
  font-size: 0.625rem;
  font-weight: 500;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 1px 4px;
  border-radius: 2px;
}

.timeline-empty {
  font-size: 0.75rem;
  color: var(--gray-400);
  font-style: italic;
}

/* ==========================================================================
   Progress Bar
   ========================================================================== */

.timeline-progress {
  width: 80px;
  height: 4px;
  background: var(--gray-200);
  border-radius: 2px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--rose-400), var(--rose-500));
  border-radius: 2px;
  transition: width 0.3s ease;
}

/* ==========================================================================
   Total Duration
   ========================================================================== */

.timeline-total {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--gray-600);
  white-space: nowrap;
  font-family: 'JetBrains Mono', monospace;
}

/* ==========================================================================
   Timeline Link
   ========================================================================== */

.timeline-link {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--rose-500);
  background: var(--rose-50);
  border-radius: 6px;
  text-decoration: none;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.timeline-link:hover {
  background: var(--rose-100);
  color: var(--rose-600);
}

.link-icon {
  width: 14px;
  height: 14px;
}
</style>
