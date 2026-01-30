<script setup lang="ts">
/**
 * VideoPanel - 영상 생성/편집 패널
 * 트랜지션 영상 + 확정 기능 지원
 */
import { ref, computed, watch } from 'vue';
import type { Node as VueFlowNode } from '@vue-flow/core';
import type { VideoNodeData, CameraMotion, ShotNodeData } from '../../../types/ui/sceneNodes';
import { PromptStatus, JobStatus, NodeType } from '../../../types/ui/sceneNodes';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useUIStore } from '../../../stores/ui';
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { useHelpPopover } from '../../../composables/useHelpPopover';
import { Video, Repeat, Move, Timer, Text, FileText, Sparkles, Check, RefreshCw, Target, ZoomIn, ZoomOut, ArrowRight, ArrowUp, Circle, Loader2 } from 'lucide-vue-next';
import { resolveCameraMotionKey } from '../../../utils/nodeSettings';
import {
  DEFAULT_VIDEO_CAMERA_MOTION,
  DEFAULT_VIDEO_DURATION,
} from '../../../utils/nodeDefaults';
import { aiService } from '../../../services';

interface Props {
  node: VueFlowNode<VideoNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();
const uiStore = useUIStore();
const cameraMotionHelp = useHelpPopover({
  storageKey: 'scene-editor:camera-motion-help',
  defaultOpen: true,
  openOnce: true,
});

const form = ref({
  isTransition: false,
  cameraMotion: DEFAULT_VIDEO_CAMERA_MOTION as CameraMotion,
  duration: DEFAULT_VIDEO_DURATION,
  motionDescription: '',
  prompt: '',
  promptKo: '',
  promptEnFinal: '',
  promptEnFinalOverride: '',
  usePromptOverride: false,
  showAdvanced: false,
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
const startShotData = computed(() => {
  const startId = data.value?.startShotId;
  if (!startId) return undefined;
  const node = nodeStore.nodes.find((n) => n.id === startId);
  return node?.data as ShotNodeData | undefined;
});
const endShotData = computed(() => {
  const endId = data.value?.endShotId;
  if (!endId) return undefined;
  const node = nodeStore.nodes.find((n) => n.id === endId);
  return node?.data as ShotNodeData | undefined;
});
const hasPromptContent = computed(() => {
  if (!data.value) return false;
  return Boolean(
    (data.value.prompt ?? '').trim() ||
    (data.value.promptKo ?? '').trim() ||
    (data.value.promptEnFinal ?? '').trim() ||
    (data.value.promptEnFinalOverride ?? '').trim()
  );
});
const isPromptGenerated = computed(
  () => hasPromptContent.value || data.value?.promptStatus !== PromptStatus.DRAFT
);
const isPromptApproved = computed(() => data.value?.promptStatus === PromptStatus.APPROVED);
const isSucceeded = computed(() => data.value?.jobStatus === JobStatus.SUCCEEDED);
const isConfirmed = computed(() => data.value?.isConfirmed ?? false);
const isStartShotReady = computed(() => {
  const start = startShotData.value as { jobStatus?: string; imageUrl?: string | null; thumbnailUrl?: string | null } | undefined;
  const hasImage = Boolean(start?.thumbnailUrl || start?.imageUrl);
  return start?.jobStatus === JobStatus.SUCCEEDED && hasImage;
});
const isEndShotReady = computed(() => {
  if (!form.value.isTransition) return true;
  const end = endShotData.value as { jobStatus?: string; imageUrl?: string | null; thumbnailUrl?: string | null } | undefined;
  const hasImage = Boolean(end?.thumbnailUrl || end?.imageUrl);
  return end?.jobStatus === JobStatus.SUCCEEDED && hasImage;
});

function findAncestorNodeId(startNodeId: string | null | undefined, targetType: NodeType): string | null {
  let currentId = startNodeId ?? null;
  const visited = new Set<string>();
  while (currentId) {
    if (visited.has(currentId)) return null;
    visited.add(currentId);
    const node = nodeStore.nodes.find((n) => n.id === currentId);
    if (!node?.data) return null;
    if (node.data.type === targetType) return node.id;
    currentId = node.data.parentNodeId;
  }
  return null;
}

type EndShotOption = { id: string; label: string; isReady: boolean; order: number };
const endShotOptions = computed<EndShotOption[]>(() => {
  const sceneHeaderId = findAncestorNodeId(data.value?.startShotId, NodeType.SCENE_HEADER);
  if (!sceneHeaderId) return [];

  return nodeStore.nodes
    .filter((n) => n.data?.type === NodeType.SHOT)
    .filter((n) => findAncestorNodeId(n.id, NodeType.SCENE_HEADER) === sceneHeaderId)
    .map((n) => {
      const shotData = n.data as ShotNodeData;
      const hasImage = Boolean(shotData.thumbnailUrl || shotData.imageUrl);
      const isReady = shotData.jobStatus === JobStatus.SUCCEEDED && hasImage;
      const gridCellIndex = typeof shotData.gridCellIndex === 'number' ? shotData.gridCellIndex + 1 : null;
      const order = typeof shotData.gridCellIndex === 'number' ? shotData.gridCellIndex : Number.MAX_SAFE_INTEGER;
      const shotTypeLabel = shotData.shotType ? ` · ${shotData.shotType}` : '';
      const statusLabel = isReady ? '' : ' (미완료)';
      const label = `${gridCellIndex ? `#${gridCellIndex}` : 'SHOT'}${shotTypeLabel}${statusLabel}`;
      return { id: n.id, label, isReady, order };
    })
    .sort((a, b) => {
      if (a.order !== b.order) return a.order - b.order;
      return a.label.localeCompare(b.label);
    });
});


const {
  isGeneratingPrompt,
  isGeneratingJob: isGeneratingVideo,
  clearError,
  generatePrompt,
  approvePrompt,
  refreshPromptPreview,
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
  getPromptUpdate: (result) => ({
    cameraMotion: form.value.cameraMotion,
    duration: normalizeDuration(form.value.duration),
    motionDescription: form.value.motionDescription,
    prompt: result.promptEnBase,
    promptKo: result.promptKo,
  }),
  getApprovedUpdate: () => ({
    prompt: form.value.prompt,
    promptKo: form.value.promptKo,
    promptEnFinalOverride: form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  }),
  getJobSettings: () => ({
    cameraMotionKey: resolveCameraMotionKey(form.value.cameraMotion),
    motionDescriptionKo: form.value.motionDescription,
    duration: normalizeDuration(form.value.duration),
    startShotNodeId: data.value?.startShotId ? Number(data.value.startShotId) : undefined,
    endShotNodeId: form.value.isTransition && data.value?.endShotId
      ? Number(data.value.endShotId)
      : null,
  }),
  getPromptOverride: () =>
    form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  getPromptPreviewPayload: () => ({
    prompt: form.value.prompt,
    settings: {
      cameraMotionKey: resolveCameraMotionKey(form.value.cameraMotion),
      motionDescriptionKo: form.value.motionDescription,
      duration: normalizeDuration(form.value.duration),
      startShotNodeId: data.value?.startShotId ? Number(data.value.startShotId) : undefined,
      endShotNodeId: form.value.isTransition && data.value?.endShotId
        ? Number(data.value.endShotId)
        : null,
    },
    promptEnFinalOverride: form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  }),
  onPromptPreview: (result) => ({
    promptEnFinal: result.promptEnFinal,
  }),
  getJobSuccessUpdate: ({ resultUrl, thumbnailUrl }) => ({
    videoUrl: resultUrl || null,
    thumbnailUrl: thumbnailUrl || null,
  }),
  messages: {
    jobError: '영상 생성에 실패했습니다. 다시 시도해주세요.',
  },
});


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

watch(() => props.node.id, () => {
  if (!data.value) return;
  form.value = {
    isTransition: !!data.value.endShotId,
    cameraMotion: normalizeCameraMotion(data.value.cameraMotion),
    duration: normalizeDuration(data.value.duration),
    motionDescription: data.value.motionDescription || '',
    prompt: data.value.prompt || '',
    promptKo: data.value.promptKo || '',
    promptEnFinal: data.value.promptEnFinal || '',
    promptEnFinalOverride: data.value.promptEnFinalOverride || '',
    usePromptOverride: Boolean(data.value.promptEnFinalOverride),
    showAdvanced: false,
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
  () => data.value?.promptKo,
  (nextPromptKo) => {
    const normalized = nextPromptKo ?? '';
    if (normalized !== form.value.promptKo) {
      form.value.promptKo = normalized;
    }
  }
);

watch(
  () => data.value?.promptEnFinal,
  (nextPromptEnFinal) => {
    const normalized = nextPromptEnFinal ?? '';
    if (normalized !== form.value.promptEnFinal) {
      form.value.promptEnFinal = normalized;
    }
  }
);

watch(
  () => data.value?.promptEnFinalOverride,
  (nextPromptOverride) => {
    const normalized = nextPromptOverride ?? '';
    if (normalized !== form.value.promptEnFinalOverride) {
      form.value.promptEnFinalOverride = normalized;
    }
    form.value.usePromptOverride = Boolean(normalized);
  }
);

watch(
  () => [data.value?.cameraMotion, data.value?.duration, data.value?.motionDescription],
  () => {
    if (!data.value) return;
    form.value.cameraMotion = normalizeCameraMotion(data.value.cameraMotion);
    form.value.duration = normalizeDuration(data.value.duration);
    form.value.motionDescription = data.value.motionDescription || '';
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

function handleEndShotChange(event: Event): void {
  const selected = (event.target as HTMLSelectElement | null)?.value ?? '';
  if (!selected) {
    nodeStore.clearEndShot(props.node.id);
    return;
  }
  nodeStore.startSelectEndShot(props.node.id);
  nodeStore.setEndShot(selected);
}

const isTranslating = ref(false);
const isRewriting = ref(false);

async function translatePrompt(): Promise<void> {
  if (!form.value.prompt) return;
  if (isTranslating.value) return;
  isTranslating.value = true;
  try {
    const result = await aiService.translatePrompt(form.value.prompt);
    if (result.promptKo && result.promptKo.trim()) {
      form.value.promptKo = result.promptKo;
    }
  } catch (error) {
    console.error('Failed to translate prompt:', error);
  } finally {
    isTranslating.value = false;
  }
}

async function rewritePrompt(): Promise<void> {
  if (!form.value.promptKo) return;
  if (isRewriting.value) return;
  isRewriting.value = true;
  try {
    const result = await aiService.rewritePrompt(form.value.promptKo);
    if (result.promptEnBase && result.promptEnBase.trim()) {
      form.value.prompt = result.promptEnBase;
      await refreshPromptPreview();
    }
  } catch (error) {
    console.error('Failed to rewrite prompt:', error);
  } finally {
    isRewriting.value = false;
  }
}

function enableFinalOverride(): void {
  if (!form.value.usePromptOverride) {
    form.value.usePromptOverride = true;
  }
  if (!form.value.promptEnFinalOverride.trim()) {
    form.value.promptEnFinalOverride = form.value.promptEnFinal || form.value.prompt;
  }
}

function clearFinalOverride(): void {
  form.value.usePromptOverride = false;
  form.value.promptEnFinalOverride = '';
}

function toggleConfirm(): void {
  nodeStore.toggleVideoConfirm(props.node.id);
}

function notifyBlocked(title: string, message: string): void {
  uiStore.showToast({
    type: 'warning',
    title,
    message,
  });
}

function handleGenerateVideo(): void {
  if (isGeneratingVideo.value || isGeneratingPrompt.value) return;
  if (!isStartShotReady.value) {
    notifyBlocked('영상 생성 불가', '시작 SHOT 이미지가 준비되어야 영상을 생성할 수 있습니다.');
    return;
  }
  if (form.value.isTransition && !data.value?.endShotId) {
    notifyBlocked('끝 샷 필요', '트랜지션 영상을 위해 끝 샷을 선택해 주세요.');
    return;
  }
  if (form.value.isTransition && !isEndShotReady.value) {
    notifyBlocked('끝 샷 준비 필요', '선택한 끝 SHOT 이미지가 준비되어야 합니다.');
    return;
  }
  if (!isPromptGenerated.value) {
    notifyBlocked('프롬프트 필요', '먼저 프롬프트를 생성해 주세요.');
    return;
  }
  if (!isPromptApproved.value) {
    notifyBlocked('프롬프트 승인 필요', '승인 후 영상을 생성할 수 있습니다.');
    return;
  }
  generateVideo();
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
      </div>

      <!-- End Shot Selection (Transition) -->
      <div v-if="form.isTransition" class="panel-section">
        <label class="panel-label">
          <Target class="panel-label-icon" />
          끝 샷
        </label>
        <select :value="data.endShotId ?? ''" class="panel-select" @change="handleEndShotChange">
          <option value="">선택 안 함</option>
          <option v-for="opt in endShotOptions" :key="opt.id" :value="opt.id">
            {{ opt.label }}
          </option>
        </select>
        <button class="panel-btn panel-btn--secondary panel-btn--full" @click="startSelectEndShot">
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
          <div :ref="cameraMotionHelp.popoverRef" class="panel-info">
            <button
              type="button"
              class="panel-info-button"
              aria-label="카메라 움직임 안내"
              :aria-expanded="cameraMotionHelp.isOpen.value"
              @click="cameraMotionHelp.toggle"
            >
              <span class="panel-info-icon">i</span>
            </button>
            <div v-if="cameraMotionHelp.isOpen.value" class="panel-info-popover">
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
      <button
        class="panel-btn panel-btn--secondary panel-btn--full panel-btn--prompt-generate"
        :disabled="isGeneratingPrompt || isGeneratingVideo"
        @click="generatePrompt"
      >
        <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon panel-btn-icon--spin" />
        <Sparkles v-else class="panel-btn-icon" />
        {{ isGeneratingPrompt ? '생성 중...' : '프롬프트 생성' }}
      </button>

      <!-- Generated Prompt -->
      <div v-if="isPromptGenerated" class="panel-section panel-section--prompt">
        <label class="panel-label">
          <FileText class="panel-label-icon" />
          프롬프트
          <span class="panel-label-badge">생성됨</span>
        </label>
        <label class="panel-label" style="margin-top: 0.75rem;">
          <FileText class="panel-label-icon" />
          생성용 프롬프트 (영어)
        </label>
        <textarea
          :value="form.promptEnFinal"
          class="panel-textarea panel-textarea--prompt"
          rows="3"
          readonly
          placeholder="생성용 프롬프트 미리보기로 확인하세요."
        ></textarea>

        <div class="panel-prompt-actions">
          <button class="panel-btn panel-btn--text" :disabled="!form.prompt" @click="refreshPromptPreview">
            <RefreshCw class="panel-btn-icon" />
            생성용 프롬프트 미리보기
          </button>
          <button class="panel-btn panel-btn--text" :disabled="!form.promptEnFinal" @click="enableFinalOverride">
            영문 직접 편집
          </button>
          <button class="panel-btn panel-btn--text" @click="form.showAdvanced = !form.showAdvanced">
            <span class="panel-btn-icon">⋯</span>
            고급 설정
          </button>
        </div>

        <div v-if="form.usePromptOverride" class="panel-section" style="margin-top: 0.75rem;">
          <label class="panel-label">
            <FileText class="panel-label-icon" />
            생성용 프롬프트 직접 수정
          </label>
          <textarea
            v-model="form.promptEnFinalOverride"
            class="panel-textarea panel-textarea--prompt"
            rows="3"
            placeholder="최종 영어 프롬프트를 직접 입력하세요."
          ></textarea>
          <div class="panel-prompt-actions panel-prompt-actions--right">
            <button class="panel-btn panel-btn--text" @click="clearFinalOverride">
              오버라이드 해제
            </button>
          </div>
        </div>

        <div v-if="form.showAdvanced" class="panel-section" style="margin-top: 0.75rem;">
          <label class="panel-label">
            <FileText class="panel-label-icon" />
            서술 프롬프트 (한국어)
          </label>
          <textarea v-model="form.promptKo" class="panel-textarea panel-textarea--prompt" rows="3"></textarea>
          <div class="panel-prompt-actions">
            <button class="panel-btn panel-btn--text" :disabled="isTranslating || !form.prompt" @click="translatePrompt">
              <Loader2 v-if="isTranslating" class="panel-btn-icon panel-btn-icon--spin" />
              <RefreshCw v-else class="panel-btn-icon" />
              EN → KO
            </button>
            <button class="panel-btn panel-btn--text" :disabled="isRewriting || !form.promptKo" @click="rewritePrompt">
              <Loader2 v-if="isRewriting" class="panel-btn-icon panel-btn-icon--spin" />
              <RefreshCw v-else class="panel-btn-icon" />
              KO → EN
            </button>
          </div>

          <label class="panel-label" style="margin-top: 0.75rem;">
            <FileText class="panel-label-icon" />
            서술 프롬프트 (영어)
          </label>
          <textarea v-model="form.prompt" class="panel-textarea panel-textarea--prompt" rows="3"></textarea>
        </div>

        <div class="panel-prompt-actions panel-prompt-actions--right">
          <button class="panel-btn panel-btn--text" :disabled="isGeneratingPrompt || isGeneratingVideo" @click="generatePrompt">
            <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon panel-btn-icon--spin" />
            <RefreshCw v-else class="panel-btn-icon" />
            재생성
          </button>
          <button
            v-if="!isPromptApproved"
            class="panel-btn panel-btn--success"
            :disabled="isGeneratingPrompt || isGeneratingVideo"
            @click="approvePrompt"
          >
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
          :disabled="isGeneratingVideo || isGeneratingPrompt"
          @click="handleGenerateVideo"
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
