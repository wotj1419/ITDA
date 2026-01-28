<script setup lang="ts">
/**
 * MasterImageNode - 마스터 이미지 노드
 * 씬의 기준이 되는 와이드샷 이미지
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 3.4
 */
import { computed } from 'vue';
import { Handle, Position } from '@vue-flow/core';
import { NodeResizer } from '@vue-flow/node-resizer';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { NodeType } from '../../../types/ui/sceneNodes';
import type { MasterImageNodeData, SceneHeaderNodeData } from '../../../types/ui/sceneNodes';
import { useNodeStatus } from '../../../composables/useNodeStatus';
import { useNodeThumbnail } from '../../../composables/useNodeThumbnail';
import { getNodeMinSize, NODE_RESIZER_STYLE } from '../../../utils/nodeUi';
import { Film, Star, Plus } from 'lucide-vue-next';

// =============================================================================
// Props & Emits
// =============================================================================

interface Props {
  id: string;
  data: MasterImageNodeData;
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

const nodeClasses = computed(() => [
  'node-glass',
  'node-glass--master',
  {
    'node-glass--selected': props.selected,
    'node-glass--active': props.data.isActive,
    'node-glass--inactive': !props.data.isActive,
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
const sceneTitle = computed(() => {
  const parentId = props.data.parentNodeId;
  if (!parentId) return '';
  const parent = store.nodes.find((node) => node.id === parentId);
  if (parent?.data?.type === NodeType.SCENE_HEADER) {
    return (parent.data as SceneHeaderNodeData).title;
  }
  return '';
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

function handleActivate(event: Event): void {
  event.stopPropagation();
  if (props.data.isActive) return;
  void store.setActiveMaster(props.id);
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
    <!-- Active Badge -->
    <div v-if="data.isActive" class="node-glass__badge node-glass__badge--active">
      <Star class="node-glass__badge-icon" />
      Active
    </div>
    <button
      v-else
      type="button"
      class="node-glass__badge node-glass__badge--activate"
      title="이 마스터를 활성화"
      @click="handleActivate"
    >
      <Star class="node-glass__badge-icon" />
      활성화
    </button>

    <!-- Target Handle (top) -->
    <Handle
      type="target"
      :position="Position.Top"
      class="node-glass__handle"
    />

    <!-- Header -->
    <div class="node-glass__header">
      <div class="node-glass__header-left">
        <Film class="node-glass__icon" />
        <div class="node-glass__title-group">
          <span class="node-glass__title">
            마스터 이미지 {{ data.version }}
          </span>
          <span v-if="sceneTitle" class="node-glass__subtitle">
            {{ sceneTitle }}
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
      <!-- Thumbnail -->
      <div
        class="node-glass__thumbnail node-glass__thumbnail--wide"
        :class="{ 'node-glass__thumbnail--loading': isThumbnailLoading && !isThumbnailVisible }"
      >
        <img 
          v-if="hasThumbnailSource" 
          v-show="isThumbnailVisible"
          :src="data.thumbnailUrl || ''" 
          alt="마스터 이미지" 
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
          class="node-glass__thumbnail-placeholder node-glass__thumbnail-placeholder--master"
        >
          <Film class="node-glass__thumbnail-placeholder-icon" />
          <span class="node-glass__thumbnail-placeholder-label">마스터 없음</span>
        </div>
      </div>
    </div>

    <!-- Source Handle (bottom) -->
    <Handle
      type="source"
      :position="Position.Bottom"
      class="node-glass__handle"
    />

    <!-- Add/Collapse Button (hover) -->
    <button
      v-if="!data.isActive"
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
      title="그리드 추가"
      @click="handleAddChild"
    >
      <Plus :size="32" />
    </button>
  </div>
</template>
