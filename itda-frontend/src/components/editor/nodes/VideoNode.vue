<script setup lang="ts">
/**
 * VideoNode - 비디오 노드 컴포넌트
 * 생성된 비디오를 표시하고 타임라인 확정 기능 제공
 */
import { computed } from 'vue';
import type { Node } from '../../../types/api/nodes';
import Button from '../../common/Button.vue';
import { Video, RefreshCw, Star, CheckCircle, Clock } from 'lucide-vue-next';

// =============================================================================
// Props & Emits
// =============================================================================

interface Props {
  node: Node;
  selected?: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'select'): void;
  (e: 'regenerate'): void;
  (e: 'confirm'): void;
}>();

// =============================================================================
// Computed
// =============================================================================

const duration = computed(() => props.node.settings?.duration || 4);
const cameraMotion = computed(() => props.node.settings?.cameraMotion || 'STATIC');
</script>

<template>
  <div
    :class="[
      'node',
      'node-video',
      `state-${node.status.toLowerCase()}`,
      { selected, confirmed: node.isConfirmed },
    ]"
    @click="emit('select')"
  >
    <!-- Handles -->
    <div class="node-handle node-handle-top connected"></div>

    <!-- Header -->
    <div class="node-header">
      <div class="node-header-title">
        <Video class="node-icon" />
        <span class="node-title">{{ node.title || 'Video' }}</span>
      </div>
      <span v-if="node.isConfirmed" class="node-status confirmed-badge">
        <Star class="status-icon" />
        확정
      </span>
      <span v-else-if="node.status === 'SUCCEEDED'" class="node-status node-status-done">
        <CheckCircle class="status-icon" />
        완료
      </span>
    </div>

    <!-- Body -->
    <div class="node-body">
      <!-- Video Preview -->
      <div
        class="node-preview video-preview"
        :style="node.thumbnailUrl ? { backgroundImage: `url(${node.thumbnailUrl})` } : {}"
      >
        <div class="play-overlay">
          <div class="play-button">
            <Video class="play-icon" />
          </div>
        </div>
      </div>

      <!-- Video Info -->
      <div class="video-info">
        <span class="info-item">
          <Clock class="info-icon" />
          {{ duration }}s
        </span>
        <span class="info-item">
          {{ cameraMotion.replace('_', ' ') }}
        </span>
      </div>

      <!-- Actions -->
      <div class="node-actions">
        <Button variant="secondary" size="sm" @click.stop="emit('regenerate')">
          <RefreshCw class="icon-sm" />
          Regen
        </Button>
        <Button
          v-if="!node.isConfirmed"
          variant="primary"
          size="sm"
          @click.stop="emit('confirm')"
        >
          <Star class="icon-sm" />
          확정
        </Button>
        <Button
          v-else
          variant="ghost"
          size="sm"
          @click.stop="emit('confirm')"
        >
          확정 취소
        </Button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==========================================================================
   Node Base
   ========================================================================== */

.node {
  position: relative;
  width: 100%;
  background: white;
  border-radius: 12px;
  border: 2px solid var(--node-video);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}

.node:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.node.selected {
  box-shadow: var(--node-video-glow);
}

.node.confirmed {
  border-color: var(--success);
}

/* ==========================================================================
   Node Header
   ========================================================================== */

.node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: linear-gradient(135deg, var(--node-video), var(--node-video-light));
}

.node-header-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: white;
}

.node-icon {
  width: 16px;
  height: 16px;
}

.node-title {
  font-weight: 600;
  font-size: 0.875rem;
}

.node-status {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: white;
  opacity: 0.9;
}

.confirmed-badge {
  background: rgba(255, 255, 255, 0.2);
  padding: 0.125rem 0.5rem;
  border-radius: 9999px;
  opacity: 1;
}

.status-icon {
  width: 14px;
  height: 14px;
}

/* ==========================================================================
   Node Body
   ========================================================================== */

.node-body {
  padding: 1rem;
}

.node-preview {
  position: relative;
  width: 100%;
  height: 100px;
  background: var(--gray-100) center/cover no-repeat;
  border-radius: 8px;
  margin-bottom: 0.75rem;
}

.video-preview {
  display: flex;
  align-items: center;
  justify-content: center;
}

.play-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.node:hover .play-overlay {
  opacity: 1;
}

.play-button {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.play-icon {
  width: 20px;
  height: 20px;
  color: var(--node-video);
}

/* ==========================================================================
   Video Info
   ========================================================================== */

.video-info {
  display: flex;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: var(--gray-600);
}

.info-icon {
  width: 12px;
  height: 12px;
}

/* ==========================================================================
   Actions
   ========================================================================== */

.node-actions {
  display: flex;
  gap: 0.5rem;
}

/* Uses global .icon-sm from base.css */

/* ==========================================================================
   Node Handle
   ========================================================================== */

.node-handle {
  position: absolute;
  width: 12px;
  height: 12px;
  background: white;
  border: 2px solid var(--node-video);
  border-radius: 50%;
  z-index: 5;
}

.node-handle-top {
  top: -6px;
  left: 50%;
  transform: translateX(-50%);
}

.node-handle.connected {
  background: var(--node-video);
}
</style>
