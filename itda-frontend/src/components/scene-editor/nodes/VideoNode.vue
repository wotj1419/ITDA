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

const statusText = computed(() => {
  if (props.data.isConfirmed) return '타임라인에 추가됨';
  const texts: Record<string, string> = {
    [JobStatus.PENDING]: '대기중',
    [JobStatus.RUNNING]: '생성중',
    [JobStatus.SUCCEEDED]: '완료',
    [JobStatus.FAILED]: '실패',
  };
  return texts[props.data.jobStatus as string] ?? '준비';
});

const isRunning = computed(() => props.data.jobStatus === JobStatus.RUNNING);

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
      <div class="node-glass__thumbnail node-glass__thumbnail--video" @click="handleThumbnailClick">
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
          v-else-if="data.thumbnailUrl"
          :src="data.thumbnailUrl"
          alt="영상 썸네일"
          class="node-glass__thumbnail-img"
        />
        <span v-else class="node-glass__thumbnail-placeholder">
          영상 썸네일
        </span>
        <!-- Play overlay on hover -->
        <div v-if="data.videoUrl || data.thumbnailUrl" class="node-glass__play-overlay">
          <Play class="node-glass__play-icon" />
        </div>
      </div>

      <!-- Confirm Button or Status -->
      <div class="node-glass__footer">
        <button 
          v-if="canConfirm" 
          class="node-glass__confirm-btn"
          @click="handleConfirm"
        >
          <Star class="node-glass__confirm-btn-icon" />
          타임라인에 확정
        </button>
        <div v-else class="node-glass__info">
          {{ statusText }}
        </div>
      </div>
    </div>

    <!-- Confirmed Bottom Bar -->
    <div v-if="data.isConfirmed" class="node-glass__confirm-bar" />
  </div>
</template>

<style scoped>
</style>
