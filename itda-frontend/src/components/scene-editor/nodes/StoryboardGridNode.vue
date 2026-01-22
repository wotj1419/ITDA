<script setup lang="ts">
/**
 * StoryboardGridNode - 스토리보드 그리드 노드
 * 마스터 기반 여러 샷 타입의 그리드 형태 이미지
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 3.5
 */
import { computed } from 'vue';
import { Handle, Position } from '@vue-flow/core';
import { NodeResizer } from '@vue-flow/node-resizer';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { JobStatus, NODE_HEIGHTS, NODE_WIDTHS } from '../../../types/node';
import type { StoryboardGridNodeData } from '../../../types/node';
import { 
  LayoutGrid, 
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
  data: StoryboardGridNodeData;
  selected?: boolean;
}

const props = defineProps<Props>();
const store = useSceneNodeStore();

const nodeStyle = { '--node-resizer-color': 'var(--rose-500, #FF85A1)' } as Record<string, string>;

const minWidth = NODE_WIDTHS[props.data.type] ?? 200;
const minHeight = NODE_HEIGHTS[props.data.type] ?? 140;

const emit = defineEmits<{
  (e: 'add-child'): void;
}>();

// =============================================================================
// Computed
// =============================================================================

const isUnderInactiveMaster = computed(() => store.isUnderInactiveMaster(props.id));

const nodeClasses = computed(() => [
  'node-glass',
  'node-glass--grid',
  {
    'node-glass--selected': props.selected,
    'node-glass--inactive': isUnderInactiveMaster.value,
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

// =============================================================================
// Handlers
// =============================================================================

function handleAddChild(event: Event): void {
  event.stopPropagation();
  emit('add-child');
}

function handleToggleCollapse(event: Event): void {
  event.stopPropagation();
  store.toggleCollapse(props.id);
}
</script>

<template>
  <div :class="nodeClasses" :style="nodeStyle">
    <NodeResizer
      :min-width="minWidth"
      :min-height="minHeight"
      :is-visible="props.selected"
      @resize-start="store.pushPositionSnapshot()"
    />
    <div v-if="props.selected" class="node-resizer-outline" />
    <!-- Target Handle (top) -->
    <Handle 
      type="target" 
      :position="Position.Top" 
      class="node-glass__handle" 
    />

    <!-- Header -->
    <div class="node-glass__header">
      <div class="node-glass__header-left">
        <LayoutGrid class="node-glass__icon" />
        <div class="node-glass__title-group">
          <span class="node-glass__title">그리드 {{ data.version }}</span>
          <span class="node-glass__subtitle">{{ data.layout }} 레이아웃</span>
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
      <div class="node-glass__thumbnail node-glass__thumbnail--wide">
        <img 
          v-if="data.thumbnailUrl" 
          :src="data.thumbnailUrl" 
          alt="그리드 이미지" 
          class="node-glass__thumbnail-img"
        />
        <span v-else class="node-glass__thumbnail-placeholder">
          그리드 이미지
        </span>
      </div>
      <div class="node-glass__footer">
        <div class="node-glass__info">
          {{ statusText }}
        </div>
      </div>
    </div>

    <!-- Source Handle (bottom) -->
    <Handle 
      type="source" 
      :position="Position.Bottom" 
      class="node-glass__handle" 
    />

    <!-- Add/Collapse Button -->
    <button
      v-if="isUnderInactiveMaster"
      class="node-glass__collapse-btn"
      @click="handleToggleCollapse"
    >
      <span
        class="node-glass__collapse-icon"
        :class="{ 'node-glass__collapse-icon--expanded': !data.isCollapsed }"
      >
        ^
      </span>
    </button>
    <button
      v-else
      class="node-glass__add-btn"
      title="샷 추가"
      @click="handleAddChild"
    >
      <Plus :size="14" />
    </button>
  </div>
</template>
