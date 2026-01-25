<script setup lang="ts">
/**
 * ShotNode - 샷 노드 컴포넌트
 * 개별 샷 이미지를 표시하고 비디오 생성 트리거
 */
import type { Node } from '../../../types/api/nodes';
import Button from '../../common/Button.vue';
import { Camera, RefreshCw, Play, CheckCircle } from 'lucide-vue-next';

// =============================================================================
// Props & Emits
// =============================================================================

interface Props {
  node: Node;
  selected?: boolean;
}

defineProps<Props>();

const emit = defineEmits<{
  (e: 'select'): void;
  (e: 'regenerate'): void;
  (e: 'add-video'): void;
}>();
</script>

<template>
  <div
    :class="[
      'node',
      'node-shot',
      `state-${node.status.toLowerCase()}`,
      { selected },
    ]"
    @click="emit('select')"
  >
    <!-- Handles -->
    <div class="node-handle node-handle-top connected"></div>
    <div class="node-handle node-handle-bottom connected"></div>

    <!-- Header -->
    <div class="node-header">
      <div class="node-header-title">
        <Camera class="node-icon" />
        <span class="node-title">{{ node.title || 'Shot' }}</span>
      </div>
      <span v-if="node.status === 'SUCCEEDED'" class="node-status node-status-done">
        <CheckCircle class="status-icon" />
        완료
      </span>
    </div>

    <!-- Body -->
    <div class="node-body">
      <!-- Preview Image -->
      <div
        class="node-preview"
        :style="node.contentUrl ? { backgroundImage: `url(${node.contentUrl})` } : {}"
      />

      <!-- Description -->
      <p v-if="node.prompt" class="node-description">
        "{{ node.prompt.substring(0, 60) }}..."
      </p>

      <!-- Actions -->
      <div class="node-actions">
        <Button variant="secondary" size="sm" @click.stop="emit('regenerate')">
          <RefreshCw class="icon-sm" />
          Regen
        </Button>
        <Button variant="primary" size="sm" @click.stop="emit('add-video')">
          <Play class="icon-sm" />
          Video
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
  border: 2px solid var(--node-shot);
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
  box-shadow: var(--node-shot-glow);
}

/* ==========================================================================
   Node Header
   ========================================================================== */

.node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: linear-gradient(135deg, var(--node-shot), var(--node-shot-light));
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
  width: 100%;
  height: 120px;
  background: var(--gray-100) center/cover no-repeat;
  border-radius: 8px;
  margin-bottom: 0.75rem;
}

.node-description {
  font-size: 0.75rem;
  color: var(--gray-600);
  margin: 0 0 0.75rem;
  line-height: 1.4;
}

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
  border: 2px solid var(--node-shot);
  border-radius: 50%;
  z-index: 5;
}

.node-handle-top {
  top: -6px;
  left: 50%;
  transform: translateX(-50%);
}

.node-handle-bottom {
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
}

.node-handle.connected {
  background: var(--node-shot);
}
</style>
