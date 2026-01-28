<script setup lang="ts">
/**
 * VideoNode - 영상 노드
 * 샷 이미지 기반 AI 영상 생성, 확정(Confirm) 기능 적용
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 3.7
 */
import { computed, ref } from 'vue';
import { Handle, Position } from '@vue-flow/core';
import { NodeResizer } from '@vue-flow/node-resizer';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { JobStatus, NODE_HEIGHTS, NODE_WIDTHS } from '../../../types/ui/sceneNodes';
import type { VideoNodeData } from '../../../types/ui/sceneNodes';
import { useThumbnailGuard } from '../../../composables/useThumbnailGuard';
import { 
  Video, 
  Star, 
  Loader2, 
  CheckCircle, 
  AlertCircle, 
  Clock, 
  Play
} from 'lucide-vue-next';

// =============================================================================
// Props & Emits
// =============================================================================

interface Props {
  id: string;
  data: VideoNodeData;
  selected?: boolean;
}

const props = defineProps<Props>();
const store = useSceneNodeStore();
const videoRef = ref<HTMLVideoElement | null>(null);

const nodeStyle = { '--node-resizer-color': 'var(--rose-500, #FF85A1)' } as Record<string, string>;

const minWidth = NODE_WIDTHS[props.data.type] ?? 200;
const minHeight = NODE_HEIGHTS[props.data.type] ?? 140;

const emit = defineEmits<{
  (e: 'confirm'): void;
}>();

// =============================================================================
// Computed
// =============================================================================

const statusKey = computed(() => {
  if (props.data.jobStatus === null) return 'idle';
  return props.data.jobStatus;
});

const isUnderInactiveMaster = computed(() => store.isUnderInactiveMaster(props.id));

const nodeClasses = computed(() => [
  'node-glass',
  'node-glass--video',
  {
    'node-glass--selected': props.selected,
    'node-glass--inactive': isUnderInactiveMaster.value,
    'node-glass--confirmed': props.data.isConfirmed,
    [`node-glass--${statusKey.value}`]: !props.data.isConfirmed,
  },
]);

const statusIcon = computed(() => {
  const icons = {
    [JobStatus.PENDING]: Clock,
    [JobStatus.RUNNING]: Loader2,
    [JobStatus.SUCCEEDED]: CheckCircle,
    [JobStatus.FAILED]: AlertCircle,
  };
  return icons[props.data.jobStatus as JobStatus] ?? Clock;
});

const isRunning = computed(() => props.data.jobStatus === JobStatus.RUNNING);
const isGenerationRequested = computed(() => props.data.generationState === 'requested');
const isGenerationFailed = computed(() => props.data.generationState === 'failed');
const hasGenerationFailure = computed(
  () => isGenerationFailed.value || props.data.jobStatus === JobStatus.FAILED
);
const {
  hasSource: hasThumbnailSource,
  isVisible: isThumbnailVisible,
  isLoading: isThumbnailGuardLoading,
  isBlocked: isThumbnailBlocked,
  handleLoad: handleThumbnailLoad,
  handleError: handleThumbnailError,
} = useThumbnailGuard(() => props.data.thumbnailUrl);
const isThumbnailLoading = computed(
  () => isGenerationRequested.value || isRunning.value || isThumbnailGuardLoading.value
);
const showFailureOverlay = computed(
  () =>
    hasGenerationFailure.value &&
    !isThumbnailVisible.value &&
    !props.data.videoUrl &&
    !isThumbnailLoading.value
);
const hasPreview = computed(() => Boolean(props.data.videoUrl || isThumbnailVisible.value));

/** Camera motion labels in Korean */
const cameraMotionLabel = computed(() => {
  const labels: Record<string, string> = {
    lowZoomIn: '로우 줌인',
    zoomOut: '줌아웃',
    panLeftToRight: '좌->우 팬',
    tiltUp: '틸트 업',
    staticCamera: '정지 카메라',
    zoomIn: '로우 줌인',
    panLeft: '좌->우 팬',
    panRight: '좌->우 팬',
    tiltDown: '틸트 업',
    static: '정지 카메라',
  };
  return labels[props.data.cameraMotion] ?? '정지 카메라';
});

/** Transition video has endShotId */
const isTransition = computed(() => !!props.data.endShotId);

/** Can confirm only when succeeded and not yet confirmed */
const canConfirm = computed(() => 
  props.data.jobStatus === JobStatus.SUCCEEDED && !props.data.isConfirmed
);

// =============================================================================
// Handlers
// =============================================================================

function handleConfirm(event: Event): void {
  event.stopPropagation();
  emit('confirm');
}

function handleThumbnailClick(event: MouseEvent): void {
  if (!props.data.videoUrl || !videoRef.value) return;
  event.stopPropagation();
  if (videoRef.value.paused) {
    void videoRef.value.play().catch(() => {});
  } else {
    videoRef.value.pause();
  }
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
    <!-- Confirmed Badge -->
    <div v-if="data.isConfirmed" class="node-glass__badge node-glass__badge--confirmed">
      <Star class="node-glass__badge-icon" />
    </div>

    <!-- Target Handle -->
    <Handle 
      type="target" 
      :position="Position.Top" 
      class="node-glass__handle" 
    />
    
    <!-- Transition: Left Handle for end shot -->
    <Handle 
      v-if="isTransition" 
      type="target" 
      :position="Position.Left" 
      id="end-shot"
      class="node-glass__handle node-glass__handle--left" 
    />

    <!-- Header -->
    <div class="node-glass__header">
      <div class="node-glass__header-left">
        <Video class="node-glass__icon" />
        <div class="node-glass__title-group">
          <span class="node-glass__title">
            영상 {{ data.version }}
            <span v-if="isTransition" class="node-glass__tag">트랜지션</span>
          </span>
          <span class="node-glass__subtitle">
            {{ cameraMotionLabel }} · {{ data.duration }}초
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
      <div
        class="node-glass__thumbnail node-glass__thumbnail--video"
        :class="{ 'node-glass__thumbnail--loading': isThumbnailLoading && !isThumbnailVisible && !data.videoUrl }"
        @click="handleThumbnailClick"
      >
        <video
          v-if="data.videoUrl"
          :src="data.videoUrl"
          ref="videoRef"
          class="node-glass__thumbnail-video"
          preload="metadata"
          muted
          playsinline
        />
        <img
          v-else-if="hasThumbnailSource"
          v-show="isThumbnailVisible"
          :src="data.thumbnailUrl || ''"
          alt="영상 썸네일"
          class="node-glass__thumbnail-img"
          @load="handleThumbnailLoad"
          @error="handleThumbnailError"
        />
        <div
          v-if="isThumbnailLoading && !isThumbnailVisible && !data.videoUrl"
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
          v-if="!data.videoUrl && (!hasThumbnailSource || isThumbnailBlocked) && !isThumbnailLoading"
          class="node-glass__thumbnail-placeholder node-glass__thumbnail-placeholder--video"
        >
          <Video class="node-glass__thumbnail-placeholder-icon" />
          <span class="node-glass__thumbnail-placeholder-label">영상 없음</span>
        </div>
        <!-- Play overlay on hover -->
        <div v-if="hasPreview" class="node-glass__play-overlay">
          <Play class="node-glass__play-icon" />
        </div>
      </div>

      <!-- Confirm Button or Status -->
      <div v-if="canConfirm" class="node-glass__footer">
        <button 
          class="node-glass__confirm-btn"
          @click="handleConfirm"
        >
          <Star class="node-glass__confirm-btn-icon" />
          타임라인에 확정
        </button>
      </div>
    </div>

    <!-- Confirmed Bottom Bar -->
    <div v-if="data.isConfirmed" class="node-glass__confirm-bar" />
  </div>
</template>

<style scoped>
</style>
