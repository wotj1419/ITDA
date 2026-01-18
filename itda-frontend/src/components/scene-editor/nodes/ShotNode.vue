<script setup lang="ts">
/**
 * ShotNode - 샷 노드
 * 그리드에서 추출하여 AI로 고품질 재생성한 개별 샷 이미지
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 3.6
 */
import { computed } from 'vue';
import { Handle, Position } from '@vue-flow/core';
import type { ShotNodeData } from '../../../types/node';
import { JobStatus } from '../../../types/node';
import { 
  Camera, 
  Loader2, 
  CheckCircle, 
  AlertCircle, 
  Clock,
  Plus 
} from 'lucide-vue-next';

// =============================================================================
// Props & Emits
// =============================================================================

interface Props {
  id: string;
  data: ShotNodeData;
  selected?: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'add-child'): void;
}>();

// =============================================================================
// Computed
// =============================================================================

const nodeClasses = computed(() => [
  'node-glass',
  'node-glass--shot',
  {
    'node-glass--selected': props.selected,
    [`node-glass--${statusKey.value}`]: true,
  },
]);

const statusKey = computed(() => {
  if (props.data.jobStatus === null) return 'idle';
  return props.data.jobStatus;
});

const statusIcon = computed(() => {
  const icons = {
    [JobStatus.PENDING]: Clock,
    [JobStatus.RUNNING]: Loader2,
    [JobStatus.SUCCEEDED]: CheckCircle,
    [JobStatus.FAILED]: AlertCircle,
  };
  return icons[props.data.jobStatus as JobStatus] ?? Clock;
});

const statusText = computed(() => {
  const texts: Record<string, string> = {
    [JobStatus.PENDING]: '대기중',
    [JobStatus.RUNNING]: '생성중',
    [JobStatus.SUCCEEDED]: '완료',
    [JobStatus.FAILED]: '실패',
  };
  return texts[props.data.jobStatus as string] ?? '준비';
});

const isRunning = computed(() => props.data.jobStatus === JobStatus.RUNNING);

/** Convert grid cell index to letter label (0 -> A, 1 -> B, etc.) */
const shotLabel = computed(() => {
  const index = props.data.gridCellIndex ?? 0;
  return String.fromCharCode(65 + index);
});

// =============================================================================
// Handlers
// =============================================================================

function handleAddChild(event: Event): void {
  event.stopPropagation();
  emit('add-child');
}
</script>

<template>
  <div :class="nodeClasses">
    <!-- Target Handle -->
    <Handle 
      type="target" 
      :position="Position.Top" 
      class="node-glass__handle" 
    />

    <!-- Header -->
    <div class="node-glass__header">
      <div class="node-glass__header-left">
        <Camera class="node-glass__icon" />
        <div class="node-glass__title-group">
          <span class="node-glass__title">
            샷 {{ shotLabel }} v{{ data.version }}
          </span>
          <span class="node-glass__subtitle">
            {{ data.shotType || '타입 미지정' }}
          </span>
        </div>
      </div>
      <div class="node-glass__status">
        <component 
          :is="statusIcon" 
          class="node-glass__status-icon" 
          :class="{ 'animate-spin': isRunning }" 
        />
      </div>
    </div>

    <!-- Body -->
    <div class="node-glass__body">
      <div class="node-glass__thumbnail node-glass__thumbnail--square">
        <img 
          v-if="data.thumbnailUrl" 
          :src="data.thumbnailUrl" 
          alt="샷 이미지" 
          class="node-glass__thumbnail-img"
        />
        <span v-else class="node-glass__thumbnail-placeholder">
          샷 이미지
        </span>
      </div>
      <div class="node-glass__footer">
        <div class="node-glass__info">
          {{ statusText }}
        </div>
      </div>
    </div>

    <!-- Source Handle -->
    <Handle 
      type="source" 
      :position="Position.Bottom" 
      class="node-glass__handle" 
    />

    <!-- Add Button -->
    <button 
      class="node-glass__add-btn" 
      title="영상 추가"
      @click="handleAddChild"
    >
      <Plus :size="14" />
    </button>
  </div>
</template>
