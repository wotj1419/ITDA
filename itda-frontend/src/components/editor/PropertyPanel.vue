<script setup lang="ts">
/**
 * PropertyPanel - 노드 속성 편집 패널
 * 선택된 노드의 세부 설정을 편집
 */
import { computed } from 'vue';
import type { Node, CameraMotion } from '../../types/api/nodes';
import Button from '../common/Button.vue';
import { Trash2, Wand2 } from 'lucide-vue-next';

// =============================================================================
// Props & Emits
// =============================================================================

interface Props {
  node: Node | null;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'update-settings', settings: { cameraMotion?: CameraMotion; duration?: number }): void;
  (e: 'delete-node'): void;
}>();

// =============================================================================
// Constants
// =============================================================================

const cameraOptions: { value: CameraMotion; label: string }[] = [
  { value: 'ZOOM_IN', label: 'Zoom In' },
  { value: 'ZOOM_OUT', label: 'Zoom Out' },
  { value: 'PAN_LEFT', label: 'Pan Left' },
  { value: 'PAN_RIGHT', label: 'Pan Right' },
  { value: 'TILT_UP', label: 'Tilt Up' },
  { value: 'TILT_DOWN', label: 'Tilt Down' },
  { value: 'STATIC', label: 'Static' },
];

const durationOptions = [3, 4, 5, 6];

// =============================================================================
// Computed
// =============================================================================

const selectedCameraMotion = computed(
  () => props.node?.settings?.cameraMotion || 'ZOOM_IN'
);

const selectedDuration = computed(
  () => props.node?.settings?.duration || 4
);

// =============================================================================
// Methods
// =============================================================================

function handleCameraChange(motion: CameraMotion): void {
  emit('update-settings', { cameraMotion: motion });
}

function handleDurationChange(duration: number): void {
  emit('update-settings', { duration });
}
</script>

<template>
  <!-- Property Panel with Node Selected -->
  <div v-if="node" class="property-panel">
    <!-- Header -->
    <div class="panel-header">
      <h3 class="panel-title">Properties</h3>
      <p class="panel-subtitle">
        <span class="node-type">{{ node.type.toLowerCase() }}</span>
        Node #{{ node.nodeId }}
      </p>
    </div>

    <!-- Body -->
    <div class="panel-body">
      <!-- Camera Motion -->
      <div class="property-section">
        <label class="property-label">Camera Motion</label>
        <div class="button-grid">
          <button
            v-for="option in cameraOptions"
            :key="option.value"
            :class="['option-btn', { active: selectedCameraMotion === option.value }]"
            @click="handleCameraChange(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </div>

      <!-- Duration -->
      <div class="property-section">
        <label class="property-label">Duration</label>
        <div class="chip-group">
          <button
            v-for="dur in durationOptions"
            :key="dur"
            :class="['chip', { selected: selectedDuration === dur }]"
            @click="handleDurationChange(dur)"
          >
            {{ dur }}s
          </button>
        </div>
      </div>

      <!-- Prompt -->
      <div class="property-section">
        <label class="property-label">Input Prompt</label>
        <textarea
          class="prompt-textarea"
          :value="node.prompt"
          readonly
        />
        <button class="improve-prompt-btn">
          <Wand2 class="icon-sm" />
          Improve Prompt
        </button>
      </div>

      <!-- Metadata -->
      <div class="property-section">
        <label class="property-label">Metadata</label>
        <div class="metadata-list">
          <div class="metadata-row">
            <span class="meta-label">Duration</span>
            <span class="meta-value">{{ selectedDuration }}s</span>
          </div>
          <div class="metadata-row">
            <span class="meta-label">Model</span>
            <span class="meta-value">{{ node.settings?.provider || 'Veo 3.1' }}</span>
          </div>
          <div class="metadata-row">
            <span class="meta-label">Status</span>
            <span :class="['meta-value', `status-${node.status.toLowerCase()}`]">
              {{ node.status }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="panel-footer">
      <Button variant="secondary" class="delete-btn" @click="emit('delete-node')">
        <Trash2 class="icon-sm" />
        Delete Node
      </Button>
    </div>
  </div>

  <!-- Empty State -->
  <div v-else class="property-panel empty">
    <div class="empty-content">
      <p class="empty-text">노드를 선택하세요</p>
      <p class="empty-hint">클릭하여 속성을 편집할 수 있습니다</p>
    </div>
  </div>
</template>

<style scoped>
/* ==========================================================================
   Panel Base
   ========================================================================== */

.property-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* ==========================================================================
   Panel Header
   ========================================================================== */

.panel-header {
  padding: 1rem;
  border-bottom: 1px solid var(--rose-100);
}

.panel-title {
  font-size: 1rem;
  font-weight: 700;
  margin: 0 0 0.25rem;
}

.panel-subtitle {
  font-size: 0.75rem;
  color: var(--gray-500);
  margin: 0;
}

.node-type {
  text-transform: capitalize;
}

/* ==========================================================================
   Panel Body
   ========================================================================== */

.panel-body {
  flex: 1;
  padding: 1rem;
  overflow-y: auto;
}

.property-section {
  margin-bottom: 1.5rem;
}

.property-label {
  display: block;
  font-size: 0.625rem;
  font-weight: 600;
  color: var(--gray-500);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 0.75rem;
}

/* ==========================================================================
   Camera Motion Grid
   ========================================================================== */

.button-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
}

.option-btn {
  padding: 0.5rem;
  font-size: 0.75rem;
  font-weight: 500;
  border: 1px solid var(--rose-200);
  border-radius: 6px;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.option-btn:hover {
  border-color: var(--rose-300);
  background: var(--rose-50);
}

.option-btn.active {
  background: var(--rose-500);
  border-color: var(--rose-500);
  color: white;
}

/* ==========================================================================
   Duration Chips
   ========================================================================== */

.chip-group {
  display: flex;
  gap: 0.5rem;
}

.chip {
  padding: 0.375rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 500;
  border: 1px solid var(--rose-200);
  border-radius: 9999px;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.chip:hover {
  border-color: var(--rose-300);
  background: var(--rose-50);
}

.chip.selected {
  background: var(--rose-500);
  border-color: var(--rose-500);
  color: white;
}

/* ==========================================================================
   Prompt Textarea
   ========================================================================== */

.prompt-textarea {
  width: 100%;
  min-height: 80px;
  padding: 0.75rem;
  font-size: 0.75rem;
  font-family: inherit;
  border: 1px solid var(--rose-200);
  border-radius: 8px;
  resize: vertical;
  background: var(--gray-50);
}

.improve-prompt-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.5rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--rose-500);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.2s ease;
}

.improve-prompt-btn:hover {
  color: var(--rose-600);
}

/* ==========================================================================
   Metadata
   ========================================================================== */

.metadata-list {
  background: var(--gray-50);
  border-radius: 8px;
  padding: 0.75rem;
}

.metadata-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
  padding: 0.25rem 0;
}

.metadata-row:not(:last-child) {
  border-bottom: 1px solid var(--gray-100);
  padding-bottom: 0.5rem;
  margin-bottom: 0.5rem;
}

.meta-label {
  color: var(--gray-500);
}

.meta-value {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
}

.status-succeeded {
  color: var(--success);
}

.status-running {
  color: var(--warning);
}

.status-failed {
  color: var(--error);
}

/* ==========================================================================
   Panel Footer
   ========================================================================== */

.panel-footer {
  padding: 1rem;
  border-top: 1px solid var(--rose-100);
}

.delete-btn {
  width: 100%;
  justify-content: center;
  color: var(--error-600);
}

/* Uses global .icon-sm from base.css */

/* ==========================================================================
   Empty State
   ========================================================================== */

.empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-content {
  text-align: center;
}

.empty-text {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin: 0 0 0.25rem;
}

.empty-hint {
  font-size: 0.75rem;
  color: var(--gray-400);
  margin: 0;
}
</style>
