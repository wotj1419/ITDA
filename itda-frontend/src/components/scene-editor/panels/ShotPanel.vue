<script setup lang="ts">
/**
 * ShotPanel - 샷 생성/편집 패널
 */
import { ref, computed, watch, nextTick, onUnmounted } from 'vue';
import type { Node } from '@vue-flow/core';
import type { MasterImageNodeData, ShotNodeData, StoryboardGridNodeData } from '../../../types/ui/sceneNodes';
import { GenerationState, JobStatus, NodeType, PromptStatus } from '../../../types/ui/sceneNodes';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useObjectStore } from '../../../stores/object';
import { useUIStore } from '../../../stores/ui';
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { Camera, Smile, PenLine, FileText, Sparkles, Check, LayoutGrid, Loader2 } from 'lucide-vue-next';
import { resolveExpressionKey, resolveShotTypeKey } from '../../../utils/nodeSettings';
import { DEFAULT_GRID_LAYOUT } from '../../../utils/nodeDefaults';

interface Props {
  node: Node<ShotNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();
const uiStore = useUIStore();
const objectStore = useObjectStore();

const form = ref({
  shotTypes: [] as string[],
  expression: '',
  additionalDetail: '',
  prompt: '',
  promptKo: '',
  promptEnFinal: '',
  promptEnFinalOverride: '',
  usePromptOverride: false,
});

const isPromptEditing = ref(false);
const isFinalEditing = ref(false);
const isGeneratingFinalPrompt = ref(false);
const finalPromptSignature = ref('');
const finalPromptSourcePromptSnapshot = ref('');
const approvedFinalPromptSnapshot = ref('');
let promptPreviewTimeout: ReturnType<typeof setTimeout> | null = null;

const shotTypeOptions = ['와이드샷', '미디엄샷', '클로즈업', '익스트림 클로즈업'];
const expressionOptions = ['기본', '미소', '슬픔', '놀람', '분노', '무표정'];

const data = computed(() => props.node.data as ShotNodeData | undefined);
const shotLabel = computed(() => String.fromCharCode(65 + (data.value?.gridCellIndex || 0)));
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
const parentGridNode = computed(() =>
  nodeStore.nodes.find(
    (node) => node.id === data.value?.parentNodeId && node.data?.type === NodeType.STORYBOARD_GRID
  )
);
const parentGridData = computed(() => parentGridNode.value?.data as StoryboardGridNodeData | undefined);
const gridLayout = computed(() => parentGridData.value?.layout || DEFAULT_GRID_LAYOUT);
const parentGridMode = computed(() => parentGridData.value?.gridMode ?? 'SHOT_VARIATIONS');
const gridCellCount = computed(() => {
  const match = gridLayout.value.match(/(\d+)x(\d+)/);
  if (!match) return 6;
  return Number(match[1]) * Number(match[2]);
});
const gridCellOptions = computed(() =>
  Array.from({ length: gridCellCount.value }, (_, index) => index)
);
const selectedGridCell = computed(() => data.value?.gridCellIndex ?? 0);
const gridCellCutKo = computed(() => {
  if (parentGridMode.value !== 'STORY_BEATS') return '';
  const beats = parentGridData.value?.beats ?? [];
  const index = data.value?.gridCellIndex ?? 0;
  return (beats[index] ?? '').trim();
});
const gridCellShotTypeHint = computed(() => {
  if (parentGridMode.value !== 'SHOT_VARIATIONS') return '';
  const types = parentGridData.value?.shotTypes ?? [];
  const index = data.value?.gridCellIndex ?? 0;
  return (types[index] ?? '').trim();
});
const sceneHeaderData = computed(() =>
  nodeStore.nodes.find((node) => node.data?.type === NodeType.SCENE_HEADER)?.data
);
const activeMasterData = computed(() => {
  const active = nodeStore.nodes.find(
    (node) => node.data?.type === NodeType.MASTER_IMAGE && (node.data as MasterImageNodeData).isActive
  );
  if (active?.data) return active.data as MasterImageNodeData;
  const fallback = nodeStore.nodes.find((node) => node.data?.type === NodeType.MASTER_IMAGE);
  return fallback?.data as MasterImageNodeData | undefined;
});

const objectNameMap = computed(() => {
  const map = new Map<number, string>();
  objectStore.objects.forEach((item) => map.set(item.objectId, item.name));
  return map;
});

const activeMasterObjectNames = computed(() =>
  (activeMasterData.value?.objectIds || [])
    .map((id) => objectNameMap.value.get(id))
    .filter((name): name is string => Boolean(name))
);

const FINAL_PROMPT_PREFIX = 'Single cinematic still, one frame only, full-frame.';
const FINAL_PROMPT_REFERENCE =
  'Use the reference image only for continuity: same characters, wardrobe, lighting, color palette, and environment.';
const FINAL_PROMPT_NEGATIVE = 'No split frames, no panels, no collage, no borders.';

function buildFinalPrompt(base?: string | null): string {
  const trimmed = (base ?? '').trim();
  if (!trimmed) return '';
  const lower = trimmed.toLowerCase();
  const lines: string[] = [];
  if (!lower.includes('single cinematic still') && !lower.includes('one frame only')) {
    lines.push(FINAL_PROMPT_PREFIX);
  }
  if (!lower.includes('reference image') && !lower.includes('continuity')) {
    lines.push(FINAL_PROMPT_REFERENCE);
  }
  lines.push(trimmed);
  if (!/no split frames|no panels|no collage|no borders/i.test(trimmed)) {
    lines.push(FINAL_PROMPT_NEGATIVE);
  }
  return lines.join('\n');
}

function buildFinalPromptSignature(): string {
  return JSON.stringify({
    shotTypes: [...form.value.shotTypes],
    expression: form.value.expression,
    additionalDetail: form.value.additionalDetail.trim(),
    prompt: form.value.prompt.trim(),
    gridCellIndex: data.value?.gridCellIndex ?? 0,
    gridCellCutKo: gridCellCutKo.value,
    usePromptOverride: form.value.usePromptOverride,
    promptEnFinalOverride: form.value.promptEnFinalOverride.trim(),
  });
}

const {
  isGeneratingPrompt,
  isGeneratingJob: isGeneratingShot,
  clearError,
  generatePrompt,
  approvePrompt,
  refreshPromptPreview,
  runGeneration: generateShot,
} = useNodeGeneration({
  nodeId: props.node.id,
  nodeType: 'SHOT',
  toastType: 'shot',
  getPrompt: () => buildFinalPrompt(form.value.prompt),
  getPromptPayload: () => ({
    nodeType: 'SHOT',
    sceneOneLine: buildSceneOneLine(),
    style: activeMasterData.value?.style,
    timeOfDay: activeMasterData.value?.timeOfDay,
    mood: activeMasterData.value?.mood,
    objects: activeMasterObjectNames.value,
    shotType: buildShotTypeValue(form.value.shotTypes),
    expression: form.value.expression,
    additionalDetail: form.value.additionalDetail,
  }),
  getPromptUpdate: (result) => ({
    shotType: buildShotTypeValue(form.value.shotTypes),
    shotTypes: [...form.value.shotTypes],
    expression: form.value.expression,
    additionalDetail: form.value.additionalDetail,
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
    gridCellIndex: data.value?.gridCellIndex ?? 0,
    shotType: resolveShotTypeKey(form.value.shotTypes[0] ?? data.value?.shotType),
    expressionKey: resolveExpressionKey(form.value.expression),
    detailKo: form.value.additionalDetail,
    gridCellCutKo: gridCellCutKo.value,
  }),
  getReferenceObjectIds: () =>
    activeMasterData.value?.objectIds?.length ? [...activeMasterData.value.objectIds] : undefined,
  getPromptOverride: () =>
    form.value.usePromptOverride ? buildFinalPrompt(form.value.promptEnFinalOverride) : '',
  getPromptPreviewPayload: () => ({
    prompt: buildFinalPrompt(form.value.prompt),
    settings: {
      gridCellIndex: data.value?.gridCellIndex ?? 0,
      shotType: resolveShotTypeKey(form.value.shotTypes[0] ?? data.value?.shotType),
      expressionKey: resolveExpressionKey(form.value.expression),
      detailKo: form.value.additionalDetail,
      gridCellCutKo: gridCellCutKo.value,
    },
    promptEnFinalOverride: form.value.usePromptOverride
      ? buildFinalPrompt(form.value.promptEnFinalOverride)
      : '',
  }),
  onPromptPreview: (result) => ({
    promptEnFinal: result.promptEnFinal,
  }),
  getJobSuccessUpdate: ({ resultUrl, thumbnailUrl }) => ({
    imageUrl: resultUrl || null,
    thumbnailUrl: thumbnailUrl || resultUrl || null,
  }),
  messages: {
    jobError: '샷 생성에 실패했습니다. 다시 시도해주세요.',
  },
});

const isParentReady = computed(() => {
  const parent = parentGridData.value as { jobStatus?: string; imageUrl?: string | null; thumbnailUrl?: string | null } | undefined;
  const hasImage = Boolean(parent?.thumbnailUrl || parent?.imageUrl);
  return parent?.jobStatus === JobStatus.SUCCEEDED && hasImage;
});
const isNodeGenerating = computed(() =>
  data.value?.jobStatus === JobStatus.PENDING ||
  data.value?.jobStatus === JobStatus.RUNNING ||
  data.value?.generationState === GenerationState.REQUESTED
);
const isUiLocked = computed(
  () => isNodeGenerating.value || isGeneratingPrompt.value || isGeneratingFinalPrompt.value || isGeneratingShot.value
);
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
const canGenerateShotResult = computed(() => {
  // 동일 최종 프롬프트로도 재생성을 허용하므로 dirty 여부는 활성 조건에서 제외한다.
  void isFinalPromptDirty.value;
  return !isUiLocked.value && isPromptApproved.value;
});

function normalizeShotTypes(value?: string | null): string[] {
  if (!value) return [];
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function buildShotTypeValue(types: string[]): string {
  return types.map((item) => item.trim()).filter(Boolean).join(', ');
}

function buildSceneOneLine(): string {
  const parts: string[] = [];
  const header = sceneHeaderData.value as { title?: string; description?: string } | undefined;
  if (header?.title) parts.push(`scene: ${header.title}`);
  if (header?.description) parts.push(`description: ${header.description}`);
  if (parentGridData.value?.layout) parts.push(`layout: ${parentGridData.value.layout}`);
  if (parentGridData.value?.shotTypes?.length) {
    parts.push(`grid shotTypes: ${parentGridData.value.shotTypes.join(', ')}`);
  }
  if (parentGridData.value?.compositionHint) {
    parts.push(`composition: ${parentGridData.value.compositionHint}`);
  }
  if (parentGridMode.value === 'STORY_BEATS' && gridCellCutKo.value) {
    parts.push(`gridCut: ${gridCellCutKo.value}`);
  }
  if (form.value.shotTypes.length) {
    parts.push(`shotType: ${buildShotTypeValue(form.value.shotTypes)}`);
  } else if (gridCellShotTypeHint.value) {
    parts.push(`shotType: ${gridCellShotTypeHint.value}`);
  }
  if (form.value.expression) parts.push(`expression: ${form.value.expression}`);
  if (form.value.additionalDetail) parts.push(`detail: ${form.value.additionalDetail}`);
  return parts.join(', ');
}

function toggleShotType(type: string): void {
  const idx = form.value.shotTypes.indexOf(type);
  if (idx >= 0) {
    form.value.shotTypes.splice(idx, 1);
  } else {
    form.value.shotTypes.push(type);
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

function togglePromptEditing(): void {
  isPromptEditing.value = !isPromptEditing.value;
}

async function generateFinalPrompt(force = false): Promise<void> {
  if (isGeneratingFinalPrompt.value || isGeneratingPrompt.value || isGeneratingShot.value) return;
  if (!form.value.prompt.trim()) return;
  if (!force && !isNarrativePromptDirtyForFinal.value) return;
  isGeneratingFinalPrompt.value = true;
  try {
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

watch(() => props.node.id, () => {
  if (!data.value) return;
  const fallbackShotTypes =
    data.value.shotTypes && data.value.shotTypes.length > 0
      ? [...data.value.shotTypes]
      : normalizeShotTypes(data.value.shotType);
  form.value = {
    shotTypes: fallbackShotTypes,
    expression: data.value.expression || '',
    additionalDetail: data.value.additionalDetail || '',
    prompt: data.value.promptKo || data.value.prompt || '',
    promptKo: data.value.promptKo || data.value.prompt || '',
    promptEnFinal: data.value.promptEnFinal || '',
    promptEnFinalOverride: data.value.promptEnFinalOverride || '',
    usePromptOverride: Boolean(data.value.promptEnFinalOverride),
  };
  isPromptEditing.value = false;
  isFinalEditing.value = false;
  finalPromptSignature.value = form.value.promptEnFinal.trim() ? buildFinalPromptSignature() : '';
  finalPromptSourcePromptSnapshot.value = form.value.promptEnFinal.trim() ? form.value.prompt.trim() : '';
  approvedFinalPromptSnapshot.value = '';
  syncFinalPromptSourceSnapshot();
  syncApprovedFinalPromptSnapshot();
  clearError();
}, { immediate: true });

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
  () => [data.value?.shotTypes, data.value?.shotType, data.value?.expression, data.value?.additionalDetail],
  () => {
    if (!data.value) return;
    const fallbackShotTypes =
      data.value.shotTypes && data.value.shotTypes.length > 0
        ? [...data.value.shotTypes]
        : normalizeShotTypes(data.value.shotType);
    form.value.shotTypes = fallbackShotTypes;
    form.value.expression = data.value.expression || '';
    form.value.additionalDetail = data.value.additionalDetail || '';
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
    const updates: Partial<ShotNodeData> = {};
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
  () => [parentGridMode.value, gridCellShotTypeHint.value, data.value?.gridCellIndex],
  () => {
    if (parentGridMode.value !== 'SHOT_VARIATIONS') return;
    if (form.value.shotTypes.length > 0) return;
    if (!gridCellShotTypeHint.value) return;
    form.value.shotTypes = [gridCellShotTypeHint.value];
  }
);

watch(
  () => ({
    prompt: form.value.prompt,
    shotTypes: form.value.shotTypes.slice(),
    expression: form.value.expression,
    additionalDetail: form.value.additionalDetail,
    style: activeMasterData.value?.style,
    timeOfDay: activeMasterData.value?.timeOfDay,
    mood: activeMasterData.value?.mood,
    objectIds: activeMasterData.value?.objectIds ?? [],
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

function selectGridCell(index: number): void {
  nodeStore.updateNode(props.node.id, { gridCellIndex: index });
}

function notifyBlocked(title: string, message: string): void {
  uiStore.showToast({
    type: 'warning',
    title,
    message,
  });
}

function handleGenerateShot(): void {
  if (isGeneratingShot.value || isGeneratingPrompt.value) return;
  if (!isParentReady.value) {
    notifyBlocked('샷 생성 불가', '상위 GRID 이미지가 준비되어야 샷을 생성할 수 있습니다.');
    return;
  }
  if (!isPromptGenerated.value) {
    notifyBlocked('프롬프트 필요', '먼저 프롬프트를 생성해 주세요.');
    return;
  }
  if (!isPromptApproved.value) {
    notifyBlocked('프롬프트 승인 필요', '승인 후 샷을 생성할 수 있습니다.');
    return;
  }
  generateShot();
}
</script>

<template>
  <BasePanel :title="`샷 ${shotLabel} 생성`" :icon="Camera">
    <template v-if="data">
      <fieldset class="panel-lock-fieldset" :disabled="isUiLocked">
      <!-- Grid Cell Selection -->
      <div class="panel-section">
        <label class="panel-label">
          <LayoutGrid class="panel-label-icon" />
          그리드 셀 선택
        </label>
        <div class="panel-button-group">
          <button
            v-for="idx in gridCellOptions"
            :key="idx"
            type="button"
            :class="['panel-button-option', { active: idx === selectedGridCell }]"
            @click="selectGridCell(idx)"
          >
            {{ idx + 1 }}
          </button>
        </div>
      </div>

      <!-- Shot Type -->
      <div class="panel-section">
        <label class="panel-label">
          <Camera class="panel-label-icon" />
          샷 타입 (다중 선택)
        </label>
        <div class="panel-radio-group panel-pill-group panel-pill-group--accent">
          <label v-for="opt in shotTypeOptions" :key="opt" class="panel-radio panel-pill">
            <input
              type="checkbox"
              :checked="form.shotTypes.includes(opt)"
              @change="toggleShotType(opt)"
            />
            <span class="panel-radio-label">{{ opt }}</span>
          </label>
        </div>
      </div>

      <!-- Expression -->
      <div class="panel-section">
        <label class="panel-label">
          <Smile class="panel-label-icon" />
          표정/분위기
        </label>
        <div class="panel-radio-group panel-pill-group panel-pill-group--accent">
          <label v-for="opt in expressionOptions" :key="opt" class="panel-radio panel-pill">
            <input type="radio" v-model="form.expression" :value="opt" />
            <span class="panel-radio-label">{{ opt }}</span>
          </label>
        </div>
      </div>

      <!-- Detail Change -->
      <div class="panel-section">
        <label class="panel-label">
          <PenLine class="panel-label-icon" />
          디테일 변경 (선택)
        </label>
        <textarea
          v-model="form.additionalDetail"
          class="panel-textarea"
          rows="2"
          placeholder="예: 소품/질감/표정 등 추가 지시사항"
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
          placeholder="예: 네온 불빛이 반사되는 골목에서 캐릭터가 멈춰 서 있는 장면"
          :readonly="!isPromptEditing"
          @wheel="handlePromptWheel"
        ></textarea>
      </div>

      <!-- Generate Prompt -->
      <div class="panel-generate-row">
        <button
          class="panel-btn panel-btn--secondary panel-btn--full panel-btn--prompt-generate"
          :disabled="isGeneratingPrompt || isGeneratingShot"
          @click="generatePrompt"
        >
          <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon panel-btn-icon--spin" />
          <Sparkles v-else class="panel-btn-icon" />
          {{ isGeneratingPrompt ? '생성 중...' : aiPromptActionLabel }}
        </button>
        <button
          class="panel-btn panel-btn--secondary panel-btn--full"
          :disabled="isUiLocked"
          @click="generateFinalPrompt(true)"
        >
          <Loader2 v-if="isGeneratingFinalPrompt" class="panel-btn-icon panel-btn-icon--spin" />
          <FileText v-else class="panel-btn-icon" />
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
            :disabled="isGeneratingPrompt || isGeneratingShot"
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
        :disabled="!canGenerateShotResult"
        @click="handleGenerateShot"
      >
        <Camera class="panel-btn-icon" />
        샷 생성
      </button>
    </template>
  </BasePanel>
</template>

<style scoped>
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
