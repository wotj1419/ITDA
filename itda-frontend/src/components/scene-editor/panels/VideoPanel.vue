<script setup lang="ts">
/**
 * VideoPanel - 영상 생성/편집 패널
 * 트랜지션 영상 + 확정 기능 지원
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import type { Node as VueFlowNode } from '@vue-flow/core';
import type { VideoNodeData, CameraMotion, ShotNodeData } from '../../../types/ui/sceneNodes';
import { PromptStatus, JobStatus } from '../../../types/ui/sceneNodes';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { Video, Repeat, Move, Timer, Text, FileText, Sparkles, Check, RefreshCw, Target, ZoomIn, ZoomOut, ArrowRight, ArrowUp, Circle } from 'lucide-vue-next';

interface Props {
  node: VueFlowNode<VideoNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();
const cameraMotionHelpRef = ref<HTMLElement | null>(null);
const isCameraMotionHelpOpen = ref(false);

const form = ref({
  isTransition: false,
  cameraMotion: 'staticCamera' as CameraMotion,
  duration: 4,
  motionDescription: '',
  prompt: '',
});

const cameraOptions: { value: CameraMotion; label: string; icon: any }[] = [
  { value: 'lowZoomIn', label: '로우 줌인', icon: ZoomIn },
  { value: 'zoomOut', label: '줌아웃', icon: ZoomOut },
  { value: 'panLeftToRight', label: '좌->우 팬', icon: ArrowRight },
  { value: 'tiltUp', label: '틸트 업', icon: ArrowUp },
  { value: 'staticCamera', label: '정지 카메라', icon: Circle },
];
const cameraMotionHelpItems = [
  {
    label: '로우 줌인',
    description: '천천히 피사체를 가까이 당겨 긴장감을 높입니다.',
  },
  {
    label: '줌아웃',
    description: '화면을 멀리 물려 배경과 상황을 넓게 보여줍니다.',
  },
  {
    label: '좌->우 팬',
    description: '좌측에서 우측으로 시선을 이동시키며 공간을 훑습니다.',
  },
  {
    label: '틸트 업',
    description: '아래에서 위로 시야를 올려 크기나 높이를 강조합니다.',
  },
  {
    label: '정지 카메라',
    description: '카메라를 고정해 인물/장면의 안정감을 강조합니다.',
  },
];

const durationOptions = [4, 6, 8];
const defaultDuration = durationOptions[0] ?? 4;

function normalizeDuration(value?: number | null): number {
  if (value == null) return defaultDuration;
  return durationOptions.includes(value) ? value : defaultDuration;
}

const data = computed(() => props.node.data as VideoNodeData | undefined);
const hasEndShot = computed(() => !!data.value?.endShotId);
const startShotData = computed(() => {
  const startId = data.value?.startShotId;
  if (!startId) return undefined;
  const node = nodeStore.nodes.find((n) => n.id === startId);
  return node?.data as ShotNodeData | undefined;
});
const isPromptGenerated = computed(() => data.value?.promptStatus !== PromptStatus.DRAFT);
const isPromptApproved = computed(() => data.value?.promptStatus === PromptStatus.APPROVED);
const isSucceeded = computed(() => data.value?.jobStatus === JobStatus.SUCCEEDED);
const isConfirmed = computed(() => data.value?.isConfirmed ?? false);

const {
  isGeneratingJob: isGeneratingVideo,
  clearError,
  generatePrompt,
  approvePrompt,
  runGeneration: generateVideo,
} = useNodeGeneration({
  nodeId: props.node.id,
  nodeType: 'VIDEO',
  toastType: 'video',
  getPrompt: () => form.value.prompt,
  getPromptPayload: () => ({
    nodeType: 'VIDEO',
    sceneOneLine: buildVideoSceneOneLine(),
    cameraMotion: form.value.cameraMotion,
    duration: normalizeDuration(form.value.duration),
    motionDescription: form.value.motionDescription,
  }),
  getPromptUpdate: (prompt) => ({
    cameraMotion: form.value.cameraMotion,
    duration: normalizeDuration(form.value.duration),
    motionDescription: form.value.motionDescription,
    prompt,
  }),
  getApprovedUpdate: () => ({ prompt: form.value.prompt }),
  getJobSettings: () => ({
    cameraMotion: form.value.cameraMotion,
    motionDescription: form.value.motionDescription,
    duration: normalizeDuration(form.value.duration),
    startShotNodeId: data.value?.startShotId,
    endShotNodeId: data.value?.endShotId,
  }),
  getJobSuccessUpdate: ({ resultUrl, thumbnailUrl }) => ({
    videoUrl: resultUrl || null,
    thumbnailUrl: thumbnailUrl || null,
  }),
  messages: {
    jobError: '영상 생성에 실패했습니다. 다시 시도해주세요.',
  },
});

const canGenerate = computed(() =>
  isPromptApproved.value &&
  (!form.value.isTransition || hasEndShot.value) &&
  !isGeneratingVideo.value
);

function normalizeCameraMotion(value?: CameraMotion | string | null): CameraMotion {
  if (!value) return 'staticCamera';
  if (cameraOptions.some((option) => option.value === value)) {
    return value as CameraMotion;
  }
  const fallbackMap: Record<string, CameraMotion> = {
    zoomIn: 'lowZoomIn',
    zoomOut: 'zoomOut',
    panLeft: 'panLeftToRight',
    panRight: 'panLeftToRight',
    tiltUp: 'tiltUp',
    tiltDown: 'tiltUp',
    static: 'staticCamera',
  };
  return fallbackMap[value] ?? 'staticCamera';
}

function buildVideoSceneOneLine(): string {
  const parts: string[] = [];
  if (startShotData.value?.shotType) {
    parts.push(`shotType: ${startShotData.value.shotType}`);
  } else if (startShotData.value?.shotTypes?.length) {
    parts.push(`shotTypes: ${startShotData.value.shotTypes.join(', ')}`);
  }
  if (startShotData.value?.expression) {
    parts.push(`expression: ${startShotData.value.expression}`);
  }
  if (startShotData.value?.additionalDetail) {
    parts.push(`detail: ${startShotData.value.additionalDetail}`);
  }
  if (startShotData.value?.prompt) {
    parts.push(`shotPrompt: ${startShotData.value.prompt}`);
  }
  return parts.join(', ');
}

function toggleCameraMotionHelp(event: MouseEvent): void {
  event.stopPropagation();
  isCameraMotionHelpOpen.value = !isCameraMotionHelpOpen.value;
}

function closeCameraMotionHelp(): void {
  isCameraMotionHelpOpen.value = false;
}

function handleDocumentClick(event: MouseEvent): void {
  if (!isCameraMotionHelpOpen.value) return;
  const target = event.target as Node | null;
  if (!cameraMotionHelpRef.value || !target) return;
  if (!cameraMotionHelpRef.value.contains(target)) {
    closeCameraMotionHelp();
  }
}

function handleDocumentKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape' && isCameraMotionHelpOpen.value) {
    closeCameraMotionHelp();
  }
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick);
  document.addEventListener('keydown', handleDocumentKeydown);
});

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick);
  document.removeEventListener('keydown', handleDocumentKeydown);
});

watch(() => props.node.id, () => {
  if (!data.value) return;
  form.value = {
    isTransition: !!data.value.endShotId,
    cameraMotion: normalizeCameraMotion(data.value.cameraMotion),
    duration: normalizeDuration(data.value.duration),
    motionDescription: data.value.motionDescription || '',
    prompt: data.value.prompt || '',
  };
  clearError();
}, { immediate: true });

watch(
  () => data.value?.prompt,
  (nextPrompt) => {
    const normalized = nextPrompt ?? '';
    if (normalized !== form.value.prompt) {
      form.value.prompt = normalized;
    }
  }
);

watch(
  () => form.value.isTransition,
  (enabled) => {
    if (!enabled) {
      nodeStore.clearEndShot(props.node.id);
    }
  }
);

function startSelectEndShot(): void {
  nodeStore.startSelectEndShot(props.node.id);
}

function toggleConfirm(): void {
  nodeStore.toggleVideoConfirm(props.node.id);
}
</script>

<template>
  <BasePanel title="영상 생성" :icon="Video">
    <template v-if="data">
      <!-- Transition Toggle -->
      <div class="panel-section">
        <div class="panel-toggle-row">
          <label class="panel-label">
            <Repeat class="panel-label-icon" />
            트랜지션 영상
          </label>
          <input type="checkbox" v-model="form.isTransition" class="panel-toggle" />
        </div>
        <p class="panel-hint">시작 샷과 끝 샷 사이를 연결하는 영상</p>
      </div>

      <!-- End Shot Selection (Transition) -->
      <div v-if="form.isTransition" class="panel-section">
        <label class="panel-label">
          <Target class="panel-label-icon" />
          끝 샷
        </label>
        <div v-if="data.endShotId" class="panel-selected-shot">
          <span>샷 선택됨</span>
          <button class="panel-btn panel-btn--text" @click="startSelectEndShot">변경</button>
        </div>
        <button v-else class="panel-btn panel-btn--secondary panel-btn--full" @click="startSelectEndShot">
          <Target class="panel-btn-icon" />
          캔버스에서 끝 샷 선택
        </button>
      </div>

      <!-- Camera Motion -->
      <div class="panel-section">
        <div class="panel-label-row">
          <label class="panel-label">
            <Move class="panel-label-icon" />
            카메라 움직임
          </label>
          <div ref="cameraMotionHelpRef" class="panel-info">
            <button
              type="button"
              class="panel-info-button"
              aria-label="카메라 움직임 안내"
              :aria-expanded="isCameraMotionHelpOpen"
              @click="toggleCameraMotionHelp"
            >
              <span class="panel-info-icon">i</span>
            </button>
            <div v-if="isCameraMotionHelpOpen" class="panel-info-popover">
              <div class="panel-info-title">카메라 움직임 안내</div>
              <ul class="panel-info-list">
                <li v-for="item in cameraMotionHelpItems" :key="item.label" class="panel-info-item">
                  <span class="panel-info-label">{{ item.label }}</span>
                  <span class="panel-info-desc">{{ item.description }}</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
        <div class="panel-camera-grid">
          <button
            v-for="opt in cameraOptions"
            :key="opt.value"
            :class="['panel-camera-option', { active: form.cameraMotion === opt.value }]"
            @click="form.cameraMotion = opt.value"
          >
            <component :is="opt.icon" class="panel-camera-icon" />
            <span>{{ opt.label }}</span>
          </button>
        </div>
      </div>

      <!-- Duration -->
      <div class="panel-section">
        <label class="panel-label">
          <Timer class="panel-label-icon" />
          길이
        </label>
        <select v-model="form.duration" class="panel-select">
          <option v-for="d in durationOptions" :key="d" :value="d">{{ d }}초</option>
        </select>
      </div>

      <!-- Motion Description -->
      <div class="panel-section">
        <label class="panel-label">
          <Text class="panel-label-icon" />
          모션 설명 (선택)
        </label>
        <textarea
          v-model="form.motionDescription"
          class="panel-textarea"
          rows="2"
          placeholder="예: 우주인이 창밖을 바라보다 고개를 돌린다"
        ></textarea>
      </div>

      <!-- Generate Prompt -->
      <button class="panel-btn panel-btn--secondary panel-btn--full" @click="generatePrompt">
        <Sparkles class="panel-btn-icon" />
        프롬프트 생성
      </button>

      <!-- Generated Prompt -->
      <div v-if="isPromptGenerated" class="panel-section">
        <label class="panel-label">
          <FileText class="panel-label-icon" />
          AI 프롬프트
        </label>
        <textarea v-model="form.prompt" class="panel-textarea panel-textarea--prompt" rows="3"></textarea>
        <div class="panel-prompt-actions">
          <button class="panel-btn panel-btn--text" @click="generatePrompt">
            <RefreshCw class="panel-btn-icon" /> 재생성
          </button>
          <button v-if="!isPromptApproved" class="panel-btn panel-btn--success" @click="approvePrompt">
            <Check class="panel-btn-icon" /> 승인
          </button>
          <span v-else class="panel-status panel-status--success">
            <Check class="panel-status-icon" />
            승인됨
          </span>
        </div>
      </div>

    </template>

    <template #footer>
      <div class="panel-actions panel-actions--footer">
        <button
          class="panel-btn panel-btn--primary panel-btn--full"
          :disabled="!canGenerate"
          @click="generateVideo"
        >
          <Video class="panel-btn-icon" />
          영상 생성
        </button>
        <div class="panel-toggle-row">
          <span class="panel-checkbox-label">타임라인 확정</span>
          <input
            type="checkbox"
            class="panel-toggle"
            :checked="isConfirmed"
            :disabled="!isSucceeded"
            @change="toggleConfirm"
          />
        </div>
      </div>
    </template>
  </BasePanel>
</template>

<style scoped>
.panel-actions--footer {
  margin-top: 0;
}

.panel-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.panel-label-row .panel-label {
  margin-bottom: 0;
}

.panel-info {
  position: relative;
}

.panel-info-button {
  width: 16px;
  height: 16px;
  border-radius: 9999px;
  border: 1px solid var(--gray-300, #D1D5DB);
  background: white;
  color: var(--gray-500, #6B7280);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease;
}

.panel-info-button:hover {
  border-color: var(--gray-400, #9CA3AF);
  color: var(--gray-700, #374151);
}

.panel-info-icon {
  font-size: 0.6875rem;
  font-style: italic;
  line-height: 1;
  font-weight: 600;
}

.panel-info-popover {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  width: 240px;
  background: white;
  border: 1px solid var(--rose-200, #FFE8F2);
  border-radius: 0.75rem;
  box-shadow: var(--shadow-lg);
  padding: 0.75rem;
  z-index: 20;
}

.panel-info-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--gray-800, #1F2937);
  margin-bottom: 0.5rem;
}

.panel-info-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.panel-info-item {
  display: flex;
  gap: 0.5rem;
  align-items: flex-start;
}

.panel-info-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--gray-700, #374151);
  flex-shrink: 0;
}

.panel-info-desc {
  font-size: 0.6875rem;
  color: var(--gray-500, #6B7280);
  line-height: 1.4;
}
</style>
