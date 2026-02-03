<script setup lang="ts">
/**
 * MasterImagePanel - 마스터 이미지 생성/편집 패널
 *
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 6.2
 */
import { ref, computed, watch, nextTick, onUnmounted, provide } from 'vue';
import type { Node } from '@vue-flow/core';
import type { MasterImageNodeData, SceneHeaderNodeData } from '../../../types/ui/sceneNodes';
import { NodeType, PromptStatus } from '../../../types/ui/sceneNodes';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useObjectStore } from '../../../stores/object';
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { Film, Palette, Sun, Smile, Sparkles, FileText, Image, Check, Star, Users, Loader2 } from 'lucide-vue-next';
import { gsap } from 'gsap';
import { resolveMoodKey, resolveStyleKey, resolveTimeOfDayKey } from '../../../utils/nodeSettings';
import {
  DEFAULT_MASTER_MOOD,
  DEFAULT_MASTER_STYLE,
  DEFAULT_MASTER_TIME_OF_DAY,
} from '../../../utils/nodeDefaults';

interface Props {
  node: Node<MasterImageNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();
const objectStore = useObjectStore();
const autoFilledNodes = new Set<string>();
let promptPreviewTimeout: ReturnType<typeof setTimeout> | null = null;
const isPromptEditing = ref(false);
const isFinalEditing = ref(false);
const isGeneratingFinalPrompt = ref(false);
const finalPromptSignature = ref('');
const finalPromptSourcePromptSnapshot = ref('');
const approvedFinalPromptSnapshot = ref('');
// 폼 상태 - objectIds는 배열로 관리 (다중 선택)
const form = ref({
  style: DEFAULT_MASTER_STYLE,
  timeOfDay: DEFAULT_MASTER_TIME_OF_DAY,
  mood: DEFAULT_MASTER_MOOD,
  objectIds: [] as number[],  // 등장 오브젝트 IDs (캐릭터 포함)
  additionalDetail: '',
  prompt: '',
  promptKo: '',
  promptEnFinal: '',
  promptEnFinalOverride: '',
  usePromptOverride: false,
});

const {
  isGeneratingPrompt,
  isGeneratingJob: isGeneratingImage,
  clearError,
  generatePrompt,
  approvePrompt,
  refreshPromptPreview,
  runGeneration: generateImage,
} = useNodeGeneration({
  nodeId: props.node.id,
  nodeType: 'MASTER',
  toastType: 'image',
  getPrompt: () => form.value.prompt,
  getPromptPayload: () => ({
    nodeType: 'MASTER',
    sceneOneLine: buildSceneOneLine(),
    additionalDetail: form.value.additionalDetail,
    style: form.value.style,
    timeOfDay: form.value.timeOfDay,
    mood: form.value.mood,
    objects: selectedObjectNames.value,
  }),
  getPromptUpdate: (result) => ({
    style: form.value.style,
    timeOfDay: form.value.timeOfDay,
    mood: form.value.mood,
    objectIds: form.value.objectIds,
    prompt: result.promptKo || result.promptEnBase,
    promptKo: result.promptKo || result.promptEnBase,
  }),
  getImproveInstruction: () => form.value.additionalDetail,
  getApprovedUpdate: () => ({
    prompt: form.value.prompt,
    promptKo: form.value.prompt,
    promptEnFinalOverride: form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  }),
  getJobSettings: () => ({
    styleKey: resolveStyleKey(form.value.style),
    timeOfDayKey: resolveTimeOfDayKey(form.value.timeOfDay),
    moodKey: resolveMoodKey(form.value.mood) ?? 'NEUTRAL',
    objectIds: form.value.objectIds,
    detailKo: form.value.additionalDetail,
  }),
  getReferenceObjectIds: () => (form.value.objectIds.length ? [...form.value.objectIds] : undefined),
  getPromptOverride: () =>
    form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  getPromptPreviewPayload: () => ({
    prompt: form.value.prompt,
    settings: {
      styleKey: resolveStyleKey(form.value.style),
      timeOfDayKey: resolveTimeOfDayKey(form.value.timeOfDay),
      moodKey: resolveMoodKey(form.value.mood) ?? 'NEUTRAL',
      objectIds: form.value.objectIds,
      detailKo: form.value.additionalDetail,
    },
    promptEnFinalOverride: form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  }),
  onPromptPreview: (result) => ({
    promptEnFinal: result.promptEnFinal,
  }),
  getJobSuccessUpdate: ({ resultUrl, thumbnailUrl }) => ({
    imageUrl: resultUrl || null,
    thumbnailUrl: thumbnailUrl || resultUrl || null,
  }),
  messages: {
    jobError: '이미지 생성에 실패했습니다. 다시 시도해주세요.',
  },
});

const styleOptions = ['시네마틱', '애니메이션', '픽사', '수채화', '유화'];
const timeOptions = ['아침', '낮', '저녁', '밤'];
const moodOptions = ['중립', '편안', '고독', '긴장', '행복', '우울'];

const objectOptions = computed(() =>
  objectStore.objects.map((item) => ({
    id: item.objectId,
    name: item.name,
    type: item.type,
  }))
);

const objectNameMap = computed(() => {
  const map = new Map<number, string>();
  objectOptions.value.forEach((item) => map.set(item.id, item.name));
  return map;
});

const selectedObjectNames = computed(() =>
  form.value.objectIds
    .map((id) => objectNameMap.value.get(id))
    .filter((name): name is string => Boolean(name))
);

const data = computed(() => props.node.data as MasterImageNodeData | undefined);
const sceneHeaderData = computed<SceneHeaderNodeData | undefined>(() => {
  const node = nodeStore.nodes.find((candidate) => candidate.data?.type === NodeType.SCENE_HEADER);
  return node?.data && node.data.type === NodeType.SCENE_HEADER ? node.data : undefined;
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
const isUiLocked = computed(
  () => isGeneratingPrompt.value || isGeneratingFinalPrompt.value || isGeneratingImage.value
);
provide('nodePanelBusy', isUiLocked);
const hasFinalPromptSnapshot = computed(() => finalPromptSignature.value.length > 0);
const isFinalPromptDirty = computed(() => {
  if (!hasFinalPromptSnapshot.value) return true;
  return finalPromptSignature.value !== buildFinalPromptSignature();
});
const isNarrativePromptDirtyForFinal = computed(() => {
  if (!finalPromptSourcePromptSnapshot.value) return true;
  return form.value.prompt.trim() !== finalPromptSourcePromptSnapshot.value;
});
const effectiveFinalPrompt = computed(() => {
  const override = form.value.usePromptOverride ? form.value.promptEnFinalOverride.trim() : '';
  return override || form.value.promptEnFinal.trim();
});
const aiPromptActionLabel = computed(() =>
  data.value?.promptStatus === PromptStatus.DRAFT ? 'AI로 생성' : 'AI로 재생성'
);
const finalPromptActionLabel = computed(() =>
  form.value.promptEnFinal.trim().length > 0 ? '최종 프롬프트 재생성' : '최종 프롬프트 생성'
);
const canGenerate = computed(() => {
  // 동일 최종 프롬프트로도 재생성을 허용하므로 dirty 여부는 활성 조건에서 제외한다.
  void isFinalPromptDirty.value;
  return isPromptApproved.value && !isUiLocked.value;
});

function buildSceneOneLine(): string {
  const parts: string[] = [];
  const header = sceneHeaderData.value as { title?: string; description?: string } | undefined;
  if (header?.title) parts.push(`scene: ${header.title}`);
  if (header?.description) parts.push(`description: ${header.description}`);
  if (form.value.style) parts.push(`style: ${form.value.style}`);
  if (form.value.timeOfDay) parts.push(`time: ${form.value.timeOfDay}`);
  if (form.value.mood) parts.push(`mood: ${form.value.mood}`);
  if (selectedObjectNames.value.length) {
    parts.push(`objects: ${selectedObjectNames.value.join(', ')}`);
  }
  if (form.value.additionalDetail) {
    parts.push(`detail: ${form.value.additionalDetail}`);
  }
  return parts.join(', ');
}

function buildFinalPromptSignature(): string {
  return JSON.stringify({
    style: form.value.style,
    timeOfDay: form.value.timeOfDay,
    mood: form.value.mood,
    objectIds: [...form.value.objectIds],
    additionalDetail: form.value.additionalDetail.trim(),
    prompt: form.value.prompt.trim(),
    usePromptOverride: form.value.usePromptOverride,
    promptEnFinalOverride: form.value.promptEnFinalOverride.trim(),
  });
}
const generateButtonRef = ref<HTMLButtonElement | null>(null);
let generateButtonTween: gsap.core.Tween | null = null;

function normalizeIds(ids: number[] | undefined | null): number[] {
  return Array.isArray(ids) ? ids : [];
}

function areNumberArraysEqual(a: number[], b: number[]): boolean {
  if (a === b) return true;
  if (a.length !== b.length) return false;
  for (let index = 0; index < a.length; index += 1) {
    if (a[index] !== b[index]) return false;
  }
  return true;
}

let persistLookTimeout: ReturnType<typeof setTimeout> | null = null;
function queuePersistLook(): void {
  if (!data.value) return;
  if (persistLookTimeout) clearTimeout(persistLookTimeout);
  persistLookTimeout = setTimeout(() => {
    persistLookTimeout = null;
    nodeStore.updateNode(props.node.id, {
      style: form.value.style,
      timeOfDay: form.value.timeOfDay,
      mood: form.value.mood,
      objectIds: [...form.value.objectIds],
    });
  }, 300);
}

function syncFinalPromptSourceSnapshot(): void {
  if (finalPromptSourcePromptSnapshot.value.trim().length > 0) return;
  if (!form.value.promptEnFinal.trim()) return;
  if (!form.value.prompt.trim()) return;
  finalPromptSourcePromptSnapshot.value = form.value.prompt.trim();
}

function syncApprovedFinalPromptSnapshot(): void {
  if (data.value?.promptStatus !== PromptStatus.APPROVED) {
    approvedFinalPromptSnapshot.value = '';
    return;
  }
  if (approvedFinalPromptSnapshot.value.trim().length > 0) return;
  if (!effectiveFinalPrompt.value) return;
  approvedFinalPromptSnapshot.value = effectiveFinalPrompt.value;
}

// 노드 변경 시 폼 동기화
watch(() => props.node.id, () => {
  if (!data.value) return;
  form.value = {
    style: data.value.style || DEFAULT_MASTER_STYLE,
    timeOfDay: data.value.timeOfDay || DEFAULT_MASTER_TIME_OF_DAY,
    mood: data.value.mood || DEFAULT_MASTER_MOOD,
    objectIds: data.value.objectIds || [],
    additionalDetail: data.value.additionalDetail || '',
    prompt: data.value.promptKo || data.value.prompt || '',
    promptKo: data.value.promptKo || data.value.prompt || '',
    promptEnFinal: data.value.promptEnFinal || '',
    promptEnFinalOverride: data.value.promptEnFinalOverride || '',
    usePromptOverride: Boolean(data.value.promptEnFinalOverride),
  };
  isPromptEditing.value = false;
  isFinalEditing.value = false;
  maybeAutofillPrompt();
  finalPromptSignature.value = form.value.promptEnFinal.trim() ? buildFinalPromptSignature() : '';
  finalPromptSourcePromptSnapshot.value = form.value.promptEnFinal.trim() ? form.value.prompt.trim() : '';
  approvedFinalPromptSnapshot.value = '';
  syncFinalPromptSourceSnapshot();
  syncApprovedFinalPromptSnapshot();
  clearError();
}, { immediate: true });

watch(
  () => ({
    style: form.value.style,
    timeOfDay: form.value.timeOfDay,
    mood: form.value.mood,
    objectIds: form.value.objectIds.slice(),
  }),
  (next) => {
    if (!data.value) return;
    const savedObjectIds = normalizeIds(data.value.objectIds);
    const nextObjectIds = normalizeIds(next.objectIds);

    const unchanged =
      next.style === (data.value.style || DEFAULT_MASTER_STYLE) &&
      next.timeOfDay === (data.value.timeOfDay || DEFAULT_MASTER_TIME_OF_DAY) &&
      next.mood === (data.value.mood || DEFAULT_MASTER_MOOD) &&
      areNumberArraysEqual(nextObjectIds, savedObjectIds);

    if (unchanged) return;

    nodeStore.updateNodeLocal(props.node.id, {
      style: next.style,
      timeOfDay: next.timeOfDay,
      mood: next.mood,
      objectIds: [...nextObjectIds],
    });
    queuePersistLook();
  },
  { deep: true }
);

watch(
  () => data.value?.prompt,
  (nextPrompt) => {
    if ((data.value?.promptKo ?? '').trim().length > 0) return;
    const normalized = nextPrompt ?? '';
    if (normalized !== form.value.prompt) {
      form.value.prompt = normalized;
    }
    syncFinalPromptSourceSnapshot();
  }
);

watch(
  () => data.value?.promptKo,
  (nextPromptKo) => {
    const normalized = nextPromptKo ?? '';
    if (normalized && normalized !== form.value.prompt) {
      form.value.prompt = normalized;
    }
    if (normalized !== form.value.promptKo) {
      form.value.promptKo = normalized;
    }
    syncFinalPromptSourceSnapshot();
  }
);

watch(
  () => data.value?.promptEnFinal,
  (nextPromptEnFinal) => {
    const normalized = nextPromptEnFinal ?? '';
    if (normalized !== form.value.promptEnFinal) {
      form.value.promptEnFinal = normalized;
    }
    syncFinalPromptSourceSnapshot();
    syncApprovedFinalPromptSnapshot();
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
    syncApprovedFinalPromptSnapshot();
  }
);

watch(
  () => data.value?.promptStatus,
  () => {
    syncApprovedFinalPromptSnapshot();
  }
);

watch(
  effectiveFinalPrompt,
  (nextFinalPrompt, prevFinalPrompt) => {
    if (prevFinalPrompt === undefined) return;
    if (nextFinalPrompt === prevFinalPrompt) return;
    if (data.value?.promptStatus !== PromptStatus.APPROVED) return;
    if (!approvedFinalPromptSnapshot.value.trim()) {
      syncApprovedFinalPromptSnapshot();
      return;
    }
    if (nextFinalPrompt === approvedFinalPromptSnapshot.value) return;
    approvedFinalPromptSnapshot.value = '';
    nodeStore.updateNodeLocal(props.node.id, { promptStatus: PromptStatus.GENERATED });
  }
);

watch(
  () => data.value?.additionalDetail,
  (nextDetail) => {
    const normalized = nextDetail ?? '';
    if (normalized !== form.value.additionalDetail) {
      form.value.additionalDetail = normalized;
    }
  }
);

watch(
  () => [
    data.value?.style,
    data.value?.timeOfDay,
    data.value?.mood,
    data.value?.objectIds,
  ],
  () => {
    if (!data.value) return;
    form.value.style = data.value.style || DEFAULT_MASTER_STYLE;
    form.value.timeOfDay = data.value.timeOfDay || DEFAULT_MASTER_TIME_OF_DAY;
    form.value.mood = data.value.mood || DEFAULT_MASTER_MOOD;
    form.value.objectIds = [...(data.value.objectIds || [])];
  }
);

watch(
  () => sceneHeaderData.value?.description,
  () => {
    maybeAutofillPrompt();
  }
);

watch(
  () => form.value.prompt,
  (nextPrompt) => {
    if (nextPrompt !== form.value.promptKo) {
      form.value.promptKo = nextPrompt;
    }
  }
);

watch(
  () => [
    form.value.prompt,
    form.value.promptEnFinalOverride,
    form.value.usePromptOverride,
    form.value.additionalDetail,
  ],
  () => {
    if (!data.value) return;
    const updates: Partial<MasterImageNodeData> = {};
    if (form.value.prompt !== (data.value.prompt ?? '')) {
      updates.prompt = form.value.prompt;
    }
    if (form.value.prompt !== (data.value.promptKo ?? '')) {
      updates.promptKo = form.value.prompt;
    }
    if (form.value.additionalDetail !== (data.value.additionalDetail ?? '')) {
      updates.additionalDetail = form.value.additionalDetail;
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
    style: form.value.style,
    timeOfDay: form.value.timeOfDay,
    mood: form.value.mood,
    objectIds: form.value.objectIds.slice(),
    additionalDetail: form.value.additionalDetail,
    usePromptOverride: form.value.usePromptOverride,
  }),
  () => {
    queuePromptPreview();
  },
  { deep: true }
);

watch(
  isGeneratingImage,
  async (running) => {
    if (running) {
      await nextTick();
      if (!generateButtonRef.value) return;
      generateButtonTween?.kill();
      generateButtonTween = gsap.to(generateButtonRef.value, {
        scale: 1.02,
        boxShadow: '0 10px 26px rgba(255, 107, 138, 0.35)',
        duration: 0.6,
        ease: 'power1.inOut',
        yoyo: true,
        repeat: -1,
      });
      return;
    }

    generateButtonTween?.kill();
    generateButtonTween = null;
    if (generateButtonRef.value) {
      gsap.set(generateButtonRef.value, { clearProps: 'transform,boxShadow' });
    }
  },
  { immediate: true }
);

onUnmounted(() => {
  generateButtonTween?.kill();
  generateButtonTween = null;
  if (persistLookTimeout) {
    clearTimeout(persistLookTimeout);
    persistLookTimeout = null;
  }
  if (promptPreviewTimeout) {
    clearTimeout(promptPreviewTimeout);
    promptPreviewTimeout = null;
  }
});
// 오브젝트 선택 토글
function toggleObject(objectId: number): void {
  const idx = form.value.objectIds.indexOf(objectId);
  if (idx >= 0) {
    form.value.objectIds.splice(idx, 1);
  } else {
    form.value.objectIds.push(objectId);
  }
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

function maybeAutofillPrompt(): void {
  const nodeId = props.node.id;
  if (autoFilledNodes.has(nodeId)) return;
  if (form.value.prompt.trim()) return;
  const header = sceneHeaderData.value as { description?: string } | undefined;
  const description = header?.description?.trim();
  if (!description) return;
  const hasHangul = /[ㄱ-ㅎㅏ-ㅣ가-힣]/.test(description);
  if (hasHangul) {
    form.value.promptKo = description;
  }
  form.value.prompt = description;
  autoFilledNodes.add(nodeId);
}

function togglePromptEditing(): void {
  isPromptEditing.value = !isPromptEditing.value;
}

async function generateFinalPrompt(force = false): Promise<void> {
  if (isGeneratingFinalPrompt.value || isGeneratingPrompt.value || isGeneratingImage.value) return;
  if (!form.value.prompt.trim()) return;
  if (!force && !isNarrativePromptDirtyForFinal.value) return;
  isGeneratingFinalPrompt.value = true;
  try {
    // "최종 프롬프트 생성"은 항상 한글 서술 기준으로 재생성되도록 override를 해제한다.
    form.value.usePromptOverride = false;
    form.value.promptEnFinalOverride = '';
    isFinalEditing.value = false;
    await refreshPromptPreview(true);
    await nextTick();
    await nodeStore.updateNode(props.node.id, {
      prompt: form.value.prompt,
      promptKo: form.value.promptKo,
      promptEnFinal: form.value.promptEnFinal,
      promptEnFinalOverride: '',
    });
    finalPromptSignature.value = buildFinalPromptSignature();
    finalPromptSourcePromptSnapshot.value = form.value.prompt.trim();
  } catch (error) {
    console.error('Failed to generate final prompt:', error);
  } finally {
    isGeneratingFinalPrompt.value = false;
  }
}

function handlePromptWheel(event: WheelEvent): void {
  const target = event.currentTarget as HTMLTextAreaElement | null;
  if (!target || target.scrollHeight <= target.clientHeight) return;
  event.preventDefault();
  target.scrollTop += event.deltaY * 0.35;
}

function queuePromptPreview(): void {
  if (!form.value.prompt.trim()) return;
  if (form.value.usePromptOverride) return;
  if (promptPreviewTimeout) clearTimeout(promptPreviewTimeout);
  promptPreviewTimeout = setTimeout(() => {
    promptPreviewTimeout = null;
    refreshPromptPreview();
  }, 600);
}



function setActive(): void {
  if (data.value?.isActive) return;
  nodeStore.setActiveMaster(props.node.id);
}
</script>

<template>
  <BasePanel title="마스터 이미지 생성" :icon="Film" class="master-panel">
    <template v-if="data" #header-actions>
      <button
        class="panel-btn master-panel__header-action"
        :class="data.isActive ? 'panel-btn--confirmed' : 'panel-btn--secondary'"
        :disabled="isUiLocked"
        @click="setActive"
        :title="data.isActive ? '현재 Active' : 'Active로 설정'"
      >
        <Star class="panel-btn-icon" />
        {{ data.isActive ? '마스터 활성' : '활성화' }}
      </button>
    </template>
    <template v-if="data">
      <fieldset class="panel-lock-fieldset" :disabled="isUiLocked">
      <div v-if="!data.isActive" class="master-panel__inactive-hint">
        <span class="master-panel__inactive-title">안내</span>
        <p class="master-panel__inactive-text">
          이 마스터가 활성화되어야 하위 생성이 정상 동작합니다.
        </p>
      </div>

      <!-- Style Selection -->
      <div class="panel-section">
        <label class="panel-label">
          <Palette class="panel-label-icon" />
          스타일
        </label>
        <div class="panel-radio-group panel-style-grid">
          <label
            v-for="opt in styleOptions"
            :key="opt"
            :class="['panel-radio', 'panel-style-card', { 'panel-style-card--primary': opt === '시네마틱' }]"
          >
            <input type="radio" v-model="form.style" :value="opt" />
            <span class="panel-radio-label">{{ opt }}</span>
          </label>
        </div>
      </div>

      <!-- Time of Day -->
      <div class="panel-section">
        <label class="panel-label">
          <Sun class="panel-label-icon" />
          시간대
        </label>
        <div class="panel-radio-group panel-pill-group panel-pill-group--accent">
          <label v-for="opt in timeOptions" :key="opt" class="panel-radio panel-pill">
            <input type="radio" v-model="form.timeOfDay" :value="opt" />
            <span class="panel-radio-label">{{ opt }}</span>
          </label>
        </div>
      </div>

      <!-- 등장 오브젝트 (다중 선택 체크박스) -->
      <div class="panel-section">
        <label class="panel-label">
          <Users class="panel-label-icon" />
          등장 오브젝트
        </label>
        <div class="panel-radio-group panel-pill-group panel-pill-group--accent">
          <label v-for="obj in objectOptions" :key="obj.id" class="panel-radio panel-pill">
            <input
              type="checkbox"
              :checked="form.objectIds.includes(obj.id)"
              @change="toggleObject(obj.id)"
            />
            <span class="panel-radio-label">{{ obj.name }}</span>
          </label>
        </div>
      </div>

      <!-- Mood -->
      <div class="panel-section">
        <label class="panel-label">
          <Smile class="panel-label-icon" />
          분위기
        </label>
        <div class="panel-radio-group panel-pill-group panel-pill-group--accent">
          <label v-for="opt in moodOptions" :key="opt" class="panel-radio panel-pill">
            <input type="radio" v-model="form.mood" :value="opt" />
            <span class="panel-radio-label">{{ opt }}</span>
          </label>
        </div>
      </div>

      <!-- Detail Change -->
      <div class="panel-section">
        <label class="panel-label">
          <Star class="panel-label-icon" />
          디테일 변경 (선택)
        </label>
        <textarea
          v-model="form.additionalDetail"
          class="panel-textarea"
          rows="2"
          placeholder="예: 질감/소품/표정 등 추가 지시사항"
        ></textarea>
      </div>

      <!-- Narrative Prompt -->
      <div class="panel-section">
        <div class="panel-label-row">
          <label class="panel-label">
            <FileText class="panel-label-icon" />
            서술 프롬프트 (한국어)
          </label>
          <button
            :class="['panel-btn', isPromptEditing ? 'panel-btn--success' : 'panel-btn--text']"
            @click="togglePromptEditing"
          >
            {{ isPromptEditing ? '편집 완료' : '직접 편집' }}
          </button>
        </div>
        <textarea
          v-model="form.prompt"
          :class="['panel-textarea', 'panel-textarea--prompt', { 'panel-textarea--editing': isPromptEditing }]"
          rows="5"
          placeholder="예: 안개 낀 절벽 끝에 홀로 선 여행자, 바람에 코트가 흔들린다."
          :readonly="!isPromptEditing"
          @wheel="handlePromptWheel"
        ></textarea>
      </div>

      <!-- Generate Prompt -->
      <div class="panel-generate-row">
        <button
          class="panel-btn panel-btn--secondary panel-btn--full panel-btn--prompt-generate"
          :disabled="isGeneratingPrompt || isGeneratingImage"
          @click="generatePrompt"
        >
          <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon animate-spin" />
          <Sparkles v-else class="panel-btn-icon" />
          {{ isGeneratingPrompt ? '생성 중...' : aiPromptActionLabel }}
        </button>
        <button
          class="panel-btn panel-btn--secondary panel-btn--full"
          :disabled="isUiLocked"
          @click="generateFinalPrompt(true)"
        >
          <Loader2 v-if="isGeneratingFinalPrompt" class="panel-btn-icon animate-spin" />
          {{ isGeneratingFinalPrompt ? '생성 중...' : finalPromptActionLabel }}
        </button>
      </div>

      <!-- Generated Prompt -->
      <div v-if="isPromptGenerated" class="panel-section panel-section--prompt">
        <label class="panel-label">
          <FileText class="panel-label-icon" />
          최종 프롬프트 (영어)
        </label>
        <textarea
          v-if="form.usePromptOverride"
          v-model="form.promptEnFinalOverride"
          :class="['panel-textarea', 'panel-textarea--prompt', { 'panel-textarea--editing': isFinalEditing }]"
          rows="5"
          :readonly="!isFinalEditing"
          placeholder="최종 영어 프롬프트를 직접 입력하세요."
          @wheel="handlePromptWheel"
        ></textarea>
        <textarea
          v-else
          :value="form.promptEnFinal"
          class="panel-textarea panel-textarea--prompt"
          rows="5"
          readonly
          placeholder="자동으로 갱신됩니다."
          @wheel="handlePromptWheel"
        ></textarea>

        <div class="panel-prompt-actions">
          <button
            :class="['panel-btn', isFinalEditing ? 'panel-btn--success' : 'panel-btn--text']"
            :disabled="!form.promptEnFinal && !form.prompt"
            @click="toggleFinalEditing"
          >
            {{ isFinalEditing ? '편집 완료' : '영문 직접 편집' }}
          </button>
        </div>

        <div class="panel-prompt-actions panel-prompt-actions--right">
          <button
            v-if="!isPromptApproved"
            class="panel-btn panel-btn--success"
            :disabled="isGeneratingPrompt || isGeneratingImage"
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
      </fieldset>
    </template>

    <template #footer>
      <button
        class="panel-btn panel-btn--primary panel-btn--full"
        :disabled="!canGenerate"
        @click="generateImage"
        ref="generateButtonRef"
      >
        <Loader2 v-if="isGeneratingImage" class="panel-btn-icon animate-spin" />
        <Image v-else class="panel-btn-icon" />
        {{ isGeneratingImage ? '생성 중...' : '이미지 생성' }}
      </button>
    </template>
  </BasePanel>
</template>

<style scoped>
.master-panel__header-action {
  padding: 0.4rem 0.65rem;
  font-size: 0.75rem;
  gap: 0.35rem;
  border-radius: 0.65rem;
  line-height: 1;
  white-space: nowrap;
}

.master-panel__header-action .panel-btn-icon {
  width: 14px;
  height: 14px;
}

.master-panel :deep(.base-panel__actions) {
  width: 100%;
  flex: 1 1 100%;
  margin-left: 0;
  justify-content: flex-start;
}

.master-panel :deep(.base-panel__close) {
  margin-left: auto;
}

.master-panel__inactive-hint {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-bottom: 1rem;
  padding: 0.5rem 0.75rem;
  background: var(--rose-50, #FFFAFC);
  border: 1px solid var(--rose-200, #FFE8F2);
  border-radius: 0.75rem;
}

.master-panel__inactive-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--rose-600, #FF6B8A);
}

.master-panel__inactive-text {
  margin: 0;
  font-size: 0.75rem;
  color: var(--gray-700, #374151);
  line-height: 1.4;
  word-break: keep-all;
  white-space: normal;
}

.panel-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
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
</style>
