<script setup lang="ts">
/**
 * VideoPanel - 영상 생성/편집 패널
 * 트랜지션 영상 + 확정 기능 지원
 */
import { ref, computed, watch, nextTick, onUnmounted } from 'vue';
import type { Node as VueFlowNode } from '@vue-flow/core';
import type { VideoNodeData, CameraMotion, ShotNodeData } from '../../../types/ui/sceneNodes';
import { PromptStatus, JobStatus, NodeType } from '../../../types/ui/sceneNodes';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useUIStore } from '../../../stores/ui';
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { useHelpPopover } from '../../../composables/useHelpPopover';
import { Video, Repeat, Move, Timer, FileText, Sparkles, Check, RefreshCw, Target, ZoomIn, ZoomOut, ArrowRight, ArrowUp, Circle, Loader2, Star } from 'lucide-vue-next';
import { gsap } from 'gsap';
import { resolveCameraMotionKey } from '../../../utils/nodeSettings';
import {
  DEFAULT_VIDEO_CAMERA_MOTION,
  DEFAULT_VIDEO_DURATION,
} from '../../../utils/nodeDefaults';

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
  promptLang: 'EN' as 'EN' | 'KO',
});

const promptSectionRef = ref<HTMLElement | null>(null);
const detailSectionRef = ref<HTMLElement | null>(null);
const detailTextareaRef = ref<HTMLTextAreaElement | null>(null);
const isFinalEditing = ref(false);
let promptPreviewTimeout: ReturnType<typeof setTimeout> | null = null;

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
const promptOverride = computed(() => form.value.promptEnFinalOverride.trim());
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
    prompt: '',
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
  getImproveInstruction: () => form.value.motionDescription,
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
  getPromptOverride: () => promptOverride.value,
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
    promptEnFinalOverride: promptOverride.value,
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
  if (form.value.motionDescription) {
    parts.push(`detail: ${form.value.motionDescription}`);
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
    promptLang: 'EN',
  };
  isFinalEditing.value = false;
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

watch(
  () => [
    form.value.prompt,
    form.value.promptKo,
    form.value.promptEnFinalOverride,
    form.value.usePromptOverride,
    form.value.motionDescription,
  ],
  () => {
    if (!data.value) return;
    const updates: Partial<VideoNodeData> = {};
    if (form.value.prompt !== (data.value.prompt ?? '')) {
      updates.prompt = form.value.prompt;
    }
    if (form.value.promptKo !== (data.value.promptKo ?? '')) {
      updates.promptKo = form.value.promptKo;
    }
    if (form.value.motionDescription !== (data.value.motionDescription ?? '')) {
      updates.motionDescription = form.value.motionDescription;
    }
    const nextOverride = form.value.usePromptOverride ? form.value.promptEnFinalOverride : '';
    if (nextOverride !== (data.value.promptEnFinalOverride ?? '')) {
      updates.promptEnFinalOverride = nextOverride;
    }
    if (Object.keys(updates).length > 0) {
      nodeStore.updateNodeLocal(props.node.id, updates);
    }
  }
);

watch(
  () => ({
    prompt: form.value.prompt,
    cameraMotion: form.value.cameraMotion,
    duration: form.value.duration,
    motionDescription: form.value.motionDescription,
    isTransition: form.value.isTransition,
    startShotId: data.value?.startShotId,
    endShotId: data.value?.endShotId,
    usePromptOverride: form.value.usePromptOverride,
  }),
  () => {
    queuePromptPreview();
  },
  { deep: true }
);

onUnmounted(() => {
  if (promptPreviewTimeout) {
    clearTimeout(promptPreviewTimeout);
    promptPreviewTimeout = null;
  }
});

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

function toggleFinalEditing(): void {
  if (!form.value.usePromptOverride) {
    form.value.usePromptOverride = true;
    form.value.promptEnFinalOverride = form.value.promptEnFinal || form.value.prompt;
    isFinalEditing.value = true;
    return;
  }

  if (isFinalEditing.value && !form.value.promptEnFinalOverride.trim()) {
    form.value.promptEnFinalOverride = form.value.promptEnFinal || form.value.prompt;
  }

  isFinalEditing.value = !isFinalEditing.value;
}

function queuePromptPreview(): void {
  if (!form.value.prompt.trim()) return;
  if (form.value.usePromptOverride) return;
  if (/[ㄱ-ㅎㅏ-ㅣ가-힣]/.test(form.value.prompt)) return;
  if (promptPreviewTimeout) clearTimeout(promptPreviewTimeout);
  promptPreviewTimeout = setTimeout(() => {
    promptPreviewTimeout = null;
    refreshPromptPreview();
  }, 600);
}

async function focusDetailEditor(): Promise<void> {
  await nextTick();
  if (detailSectionRef.value) {
    const container = detailSectionRef.value.closest('.base-panel__content') as HTMLElement | null;
    if (container) {
      const containerRect = container.getBoundingClientRect();
      const sectionRect = detailSectionRef.value.getBoundingClientRect();
      const currentScroll = container.scrollTop;
      const offset = sectionRect.top - containerRect.top;
      const centeredOffset = (container.clientHeight - sectionRect.height) / 2;
      const rawTarget = currentScroll + offset - centeredOffset;
      const maxScroll = Math.max(0, container.scrollHeight - container.clientHeight);
      const targetScroll = Math.min(Math.max(0, rawTarget), maxScroll);
      gsap.to(container, { scrollTop: targetScroll, duration: 0.45, ease: 'power2.out' });
    } else {
      detailSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
    gsap.killTweensOf(detailSectionRef.value);
    gsap.fromTo(
      detailSectionRef.value,
      { boxShadow: '0 0 0 0 rgba(255, 107, 138, 0)', backgroundColor: 'rgba(255, 250, 252, 0)' },
      {
        boxShadow: '0 0 0 12px rgba(255, 107, 138, 0.35)',
        backgroundColor: 'rgba(255, 250, 252, 0.9)',
        duration: 0.35,
        yoyo: true,
        repeat: 1,
        ease: 'power2.out',
        clearProps: 'boxShadow,backgroundColor',
      }
    );
  }
  detailTextareaRef.value?.focus();
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
  if (form.value.usePromptOverride && !promptOverride.value) {
    notifyBlocked('영문 직접 편집 필요', '영문 직접 편집 내용이 비어 있습니다.');
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

      <!-- Detail Change -->
      <div class="panel-section" ref="detailSectionRef">
        <label class="panel-label">
          <Star class="panel-label-icon" />
          디테일 변경 (선택)
          <span
            class="panel-tooltip"
            data-tooltip="연예인/실존 인물 이름은 직접 언급하지 말아주세요."
          >
            <span class="panel-tooltip__icon">?</span>
          </span>
        </label>
        <textarea
          ref="detailTextareaRef"
          v-model="form.motionDescription"
          class="panel-textarea"
          rows="2"
          placeholder="예: 동작 흐름/세부 분위기/소품 등 추가 지시사항"
        ></textarea>
      </div>

      <!-- Narrative Prompt -->
      <div class="panel-section" ref="promptSectionRef">
        <div class="panel-label-row">
          <label class="panel-label">
            <FileText class="panel-label-icon" />
            서술 프롬프트
          </label>
          <div class="panel-segmented" role="tablist" aria-label="Prompt language">
            <button
              type="button"
              class="panel-segmented__btn"
              :class="{ 'is-active': form.promptLang === 'EN' }"
              @click="form.promptLang = 'EN'"
            >
              EN
            </button>
            <button
              type="button"
              class="panel-segmented__btn"
              :class="{ 'is-active': form.promptLang === 'KO' }"
              @click="form.promptLang = 'KO'"
            >
              KO
            </button>
          </div>
        </div>
        <p
          class="panel-subtext panel-tooltip"
          data-tooltip="서술 프롬프트는 읽기 전용입니다. 한글 수정은 디테일 변경에서 가능합니다."
        >
          {{ form.promptLang === 'EN' ? '원본(읽기 전용)' : '번역(읽기 전용)' }}
          <span class="panel-tooltip__icon">?</span>
        </p>
        <div v-if="form.promptLang === 'EN'">
          <textarea
            v-model="form.prompt"
            class="panel-textarea panel-textarea--prompt"
            rows="4"
            placeholder="예: The camera glides toward the character as the city lights blur."
            readonly
          ></textarea>
        </div>
        <div v-else class="panel-translation-block">
          <textarea
            v-model="form.promptKo"
            class="panel-textarea panel-textarea--prompt"
            rows="4"
            readonly
          ></textarea>
        </div>
      </div>

      <!-- Generate Prompt -->
      <button
        class="panel-btn panel-btn--secondary panel-btn--full panel-btn--prompt-generate"
        :disabled="isGeneratingPrompt || isGeneratingVideo"
        @click="generatePrompt"
      >
        <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon panel-btn-icon--spin" />
        <Sparkles v-else class="panel-btn-icon" />
        {{ isGeneratingPrompt ? '생성 중...' : 'AI로 다듬기' }}
      </button>

      <!-- Generated Prompt -->
      <div v-if="isPromptGenerated" class="panel-section panel-section--prompt">
        <label class="panel-label">
          <FileText class="panel-label-icon" />
          최종 프롬프트 (영어)
        </label>
        <textarea
          v-if="form.usePromptOverride"
          v-model="form.promptEnFinalOverride"
          class="panel-textarea panel-textarea--prompt"
          rows="3"
          :readonly="!isFinalEditing"
          placeholder="최종 영어 프롬프트를 직접 입력하세요."
        ></textarea>
        <textarea
          v-else
          :value="form.promptEnFinal"
          class="panel-textarea panel-textarea--prompt"
          rows="3"
          readonly
          placeholder="자동으로 갱신됩니다."
        ></textarea>

        <div class="panel-prompt-actions">
          <button
            class="panel-btn panel-btn--text"
            :disabled="!form.promptEnFinal && !form.prompt"
            @click="toggleFinalEditing"
          >
            {{ isFinalEditing ? '편집 완료' : '영문 직접 편집' }}
          </button>
          <button class="panel-btn panel-btn--text" @click="focusDetailEditor">
            한글 편집
          </button>
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
  gap: 0.75rem;
  margin-bottom: 0.35rem;
}

.panel-label-row .panel-label {
  margin-bottom: 0;
}

.panel-segmented {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.15rem;
  background: var(--gray-100, #F3F4F6);
  border-radius: 999px;
}

.panel-segmented__btn {
  border: 0;
  background: transparent;
  padding: 0.2rem 0.65rem;
  font-size: 0.7rem;
  color: var(--gray-600, #4B5563);
  border-radius: 999px;
  cursor: pointer;
}

.panel-segmented__btn.is-active {
  background: var(--gray-900, #111827);
  color: var(--gray-50, #F9FAFB);
  box-shadow: 0 2px 6px rgba(17, 24, 39, 0.18);
}

.panel-subtext {
  margin: 0 0 0.5rem;
  font-size: 0.75rem;
  color: var(--gray-500, #6B7280);
}

.panel-translation-block .panel-prompt-actions {
  margin-top: 0.4rem;
}

.panel-btn--sync--muted {
  background: var(--gray-100, #F3F4F6);
  color: var(--gray-500, #6B7280);
  border-color: var(--gray-200, #E5E7EB);
}

.panel-btn--sync--muted:hover {
  background: var(--gray-100, #F3F4F6);
  color: var(--gray-500, #6B7280);
  border-color: var(--gray-200, #E5E7EB);
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
