<script setup lang="ts">
/**
 * VideoPanel - 영상 생성/편집 패널
 * 트랜지션 영상 + 확정 기능 지원
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import type { Node } from '@vue-flow/core';
import type { VideoNodeData, CameraMotion } from '../../../types/node';
import { PromptStatus, JobStatus } from '../../../types/node';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useGenerationToast } from '../../../composables/useGenerationToast';
import { aiService } from '../../../services';
import { Video, Repeat, Move, Timer, Text, FileText, Sparkles, Check, RefreshCw, Target, ZoomIn, ZoomOut, ArrowRight, ArrowUp, Circle } from 'lucide-vue-next';

interface Props {
  node: Node<VideoNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();
const cameraMotionHelpRef = ref<HTMLElement | null>(null);
const isCameraMotionHelpOpen = ref(false);
const { startGenerationToast, finishGenerationToast } = useGenerationToast();

// 로딩 상태
const isGeneratingPrompt = ref(false);
const isGeneratingVideo = ref(false);
const errorMessage = ref<string | null>(null);

const form = ref({
  isTransition: false,
  cameraMotion: 'staticCamera' as CameraMotion,
  duration: 5,
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

const durationOptions = [3, 5, 8, 10];

const data = computed(() => props.node.data as VideoNodeData | undefined);
const hasEndShot = computed(() => !!data.value?.endShotId);
const isPromptGenerated = computed(() => data.value?.promptStatus !== PromptStatus.DRAFT);
const isPromptApproved = computed(() => data.value?.promptStatus === PromptStatus.APPROVED);
const canGenerate = computed(() => isPromptApproved.value && (!form.value.isTransition || hasEndShot.value) && !isGeneratingVideo.value);
const isSucceeded = computed(() => data.value?.jobStatus === JobStatus.SUCCEEDED);
const isConfirmed = computed(() => data.value?.isConfirmed ?? false);

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
    duration: data.value.duration || 5,
    motionDescription: data.value.motionDescription || '',
    prompt: data.value.prompt || '',
  };
  errorMessage.value = null;
}, { immediate: true });

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

async function generatePrompt(): Promise<void> {
  isGeneratingPrompt.value = true;
  errorMessage.value = null;

  try {
    const prompt = await aiService.generatePrompt({
      nodeType: 'VIDEO',
      cameraMotion: form.value.cameraMotion,
      duration: form.value.duration,
      motionDescription: form.value.motionDescription,
    });

    form.value.prompt = prompt;
    nodeStore.updateNode(props.node.id, { 
      cameraMotion: form.value.cameraMotion,
      duration: form.value.duration,
      motionDescription: form.value.motionDescription,
      prompt: form.value.prompt,
      promptStatus: PromptStatus.GENERATED,
    });
  } catch (error) {
    console.error('Failed to generate prompt:', error);
    errorMessage.value = '프롬프트 생성에 실패했습니다. 다시 시도해주세요.';
  } finally {
    isGeneratingPrompt.value = false;
  }
}

function approvePrompt(): void {
  nodeStore.updateNode(props.node.id, { prompt: form.value.prompt, promptStatus: PromptStatus.APPROVED });
}

async function generateVideo(): Promise<void> {
  if (!form.value.prompt) return;

  isGeneratingVideo.value = true;
  errorMessage.value = null;
  const toastId = startGenerationToast('video');

  try {
    nodeStore.updateNode(props.node.id, { jobStatus: JobStatus.RUNNING });

    const jobId = await aiService.generateNode(props.node.id, form.value.prompt);
    console.log('Video generation job started:', jobId);

    const result = await aiService.pollJobUntilComplete(jobId, (status) => {
      console.log('Job status:', status.status);
    });

    if (result.status === 'SUCCEEDED') {
      nodeStore.updateNode(props.node.id, {
        jobStatus: JobStatus.SUCCEEDED,
        videoUrl: result.resultUrl,
        thumbnailUrl: result.thumbnailUrl,
      });
      finishGenerationToast(toastId, 'video', 'success');
    } else {
      throw new Error(result.error?.message || 'Video generation failed');
    }
  } catch (error) {
    console.error('Failed to generate video:', error);
    errorMessage.value = '영상 생성에 실패했습니다. 다시 시도해주세요.';
    nodeStore.updateNode(props.node.id, { jobStatus: JobStatus.FAILED });
    const reason = error instanceof Error ? error.message : '알 수 없는 오류';
    finishGenerationToast(toastId, 'video', 'error', { reason });
  } finally {
    isGeneratingVideo.value = false;
  }
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
