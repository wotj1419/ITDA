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
import type { StoryboardGridNodeData } from '../../../types/ui/sceneNodes';
import { useNodeStatus } from '../../../composables/useNodeStatus';
import { useNodeThumbnail } from '../../../composables/useNodeThumbnail';
import { getNodeMinSize, NODE_RESIZER_STYLE } from '../../../utils/nodeUi';
import { LayoutGrid, Plus } from 'lucide-vue-next';

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

const nodeStyle = NODE_RESIZER_STYLE;
const { minWidth, minHeight } = getNodeMinSize(props.data.type);

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

const { statusKey, statusIcon, isRunning, isGenerationRequested, hasGenerationFailure } =
  useNodeStatus(
    () => props.data.jobStatus,
    () => props.data.generationState
  );
const {
  hasSource: hasThumbnailSource,
  isVisible: isThumbnailVisible,
  isBlocked: isThumbnailBlocked,
  isThumbnailLoading,
  showFailureOverlay,
  handleLoad: handleThumbnailLoad,
  handleError: handleThumbnailError,
} = useNodeThumbnail({
  getThumbnailUrl: () => props.data.thumbnailUrl,
  isRunning: () => isRunning.value,
  isGenerationRequested: () => isGenerationRequested.value,
  hasGenerationFailure: () => hasGenerationFailure.value,
});

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

function handleRetry(event: Event): void {
  event.stopPropagation();
  store.updateNodeLocal(props.id, { generationState: null });
  store.selectNode(props.id);
}
</script>

<template>
  <div :class="nodeClasses" :style="nodeStyle">
    <NodeResizer
      :min-width="minWidth"
      :min-height="minHeight"
      :is-visible="props.selected"
      @resize-start="store.pushPositionSnapshot()"
      @resize-end="store.persistNodePositions()"
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
      <div v-if="statusIcon" class="node-glass__status">
        <component 
          :is="statusIcon" 
          class="node-glass__status-icon" 
          :class="{ 'animate-spin': isRunning }" 
        />
      </div>
    </div>

    <!-- Body -->
    <div class="node-glass__body">
      <div
        class="node-glass__thumbnail node-glass__thumbnail--wide"
        :class="{ 'node-glass__thumbnail--loading': isThumbnailLoading && !isThumbnailVisible }"
      >
        <img 
          v-if="hasThumbnailSource" 
          v-show="isThumbnailVisible"
          :src="data.thumbnailUrl || ''" 
          alt="그리드 이미지" 
          class="node-glass__thumbnail-img"
          @load="handleThumbnailLoad"
          @error="handleThumbnailError"
        />
        <div
          v-if="isThumbnailLoading && !isThumbnailVisible"
          class="node-glass__thumbnail-loader"
        >
          <span class="node-glass__thumbnail-spinner" />
          <span>생성중…</span>
        </div>
        <div
          v-if="showFailureOverlay"
          class="node-glass__thumbnail-error"
        >
          <span>생성 실패</span>
          <button class="node-glass__thumbnail-retry" @click="handleRetry">
            다시 시도
          </button>
        </div>
        <div
          v-if="(!hasThumbnailSource || isThumbnailBlocked) && !isThumbnailLoading"
          class="node-glass__thumbnail-placeholder node-glass__thumbnail-placeholder--grid"
        >
          <LayoutGrid class="node-glass__thumbnail-placeholder-icon" />
          <span class="node-glass__thumbnail-placeholder-label">그리드 없음</span>
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
      <Plus :size="32" />
    </button>
  </div>
</template>
