<script setup lang="ts">
/**
 * VideoPanel - 영상 생성/편집 패널
 * 트랜지션 영상 + 확정 기능 지원
 */
import { ref, computed, watch } from 'vue';
import type { Node } from '@vue-flow/core';
import type { VideoNodeData, CameraMotion } from '../../../types/node';
import { PromptStatus, JobStatus } from '../../../types/node';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { aiService } from '../../../services';
import { Video, Repeat, Move, Timer, Text, FileText, Sparkles, Check, RefreshCw, Star, Target, ZoomIn, ZoomOut, ArrowLeft, ArrowRight, ArrowUp, ArrowDown, Circle, Loader2 } from 'lucide-vue-next';

interface Props {
  node: Node<VideoNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();

// 로딩 상태
const isGeneratingPrompt = ref(false);
const isGeneratingVideo = ref(false);
const errorMessage = ref<string | null>(null);

const form = ref({
  isTransition: false,
  cameraMotion: 'static' as CameraMotion,
  duration: 5,
  motionDescription: '',
  prompt: '',
});

const cameraOptions: { value: CameraMotion; label: string; icon: any }[] = [
  { value: 'static', label: '정지', icon: Circle },
  { value: 'zoomIn', label: '줌인', icon: ZoomIn },
  { value: 'zoomOut', label: '줌아웃', icon: ZoomOut },
  { value: 'panLeft', label: '팬 좌', icon: ArrowLeft },
  { value: 'panRight', label: '팬 우', icon: ArrowRight },
  { value: 'tiltUp', label: '틸트 업', icon: ArrowUp },
  { value: 'tiltDown', label: '틸트 다운', icon: ArrowDown },
];

const durationOptions = [3, 5, 8, 10];

const data = computed(() => props.node.data as VideoNodeData | undefined);
const hasEndShot = computed(() => !!data.value?.endShotId);
const isPromptGenerated = computed(() => data.value?.promptStatus !== PromptStatus.DRAFT);
const isPromptApproved = computed(() => data.value?.promptStatus === PromptStatus.APPROVED);
const canGenerate = computed(() => isPromptApproved.value && (!form.value.isTransition || hasEndShot.value) && !isGeneratingVideo.value);
const isSucceeded = computed(() => data.value?.jobStatus === JobStatus.SUCCEEDED);
const isConfirmed = computed(() => data.value?.isConfirmed ?? false);

watch(() => props.node.id, () => {
  if (!data.value) return;
  form.value = {
    isTransition: !!data.value.endShotId,
    cameraMotion: data.value.cameraMotion || 'static',
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
    } else {
      throw new Error(result.error?.message || 'Video generation failed');
    }
  } catch (error) {
    console.error('Failed to generate video:', error);
    errorMessage.value = '영상 생성에 실패했습니다. 다시 시도해주세요.';
    nodeStore.updateNode(props.node.id, { jobStatus: JobStatus.FAILED });
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
        <label class="panel-label">
          <Move class="panel-label-icon" />
          카메라 움직임
        </label>
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

      <!-- Confirm Button (Succeeded only) -->
      <div v-if="isSucceeded" class="panel-section">
        <button
          :class="['panel-btn panel-btn--full', isConfirmed ? 'panel-btn--confirmed' : 'panel-btn--confirm']"
          @click="toggleConfirm"
        >
          <Star class="panel-btn-icon" />
          {{ isConfirmed ? '★ 확정됨 (취소하려면 클릭)' : '☆ 타임라인에 확정' }}
        </button>
      </div>
    </template>

    <template #footer>
      <button
        class="panel-btn panel-btn--primary panel-btn--full"
        :disabled="!canGenerate"
        @click="generateVideo"
      >
        <Video class="panel-btn-icon" />
        영상 생성
      </button>
    </template>
  </BasePanel>
</template>
