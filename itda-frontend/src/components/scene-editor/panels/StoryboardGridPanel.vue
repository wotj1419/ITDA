<script setup lang="ts">
/**
 * StoryboardGridPanel - 스토리보드 그리드 생성/편집 패널
 */
import { ref, computed, watch, nextTick, onUnmounted } from 'vue';
import type { Node as VueFlowNode } from '@vue-flow/core';
import type { StoryboardGridNodeData, GridLayout, GridMode, MasterImageNodeData } from '../../../types/ui/sceneNodes';
import { GenerationState, JobStatus, NodeType, PromptStatus } from '../../../types/ui/sceneNodes';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useUIStore } from '../../../stores/ui';
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { useGenerationToast } from '../../../composables/useGenerationToast';
import { useHelpPopover } from '../../../composables/useHelpPopover';
import { LayoutGrid, Camera, Target, FileText, Check, Loader2, Star } from 'lucide-vue-next';
import { gsap } from 'gsap';
import { mapShotTypeLabelsToKeys } from '../../../utils/nodeSettings';
import { DEFAULT_GRID_LAYOUT, DEFAULT_GRID_SHOT_TYPES } from '../../../utils/nodeDefaults';

interface Props {
  node: VueFlowNode<StoryboardGridNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();
const uiStore = useUIStore();
const { startGenerationToast, finishGenerationToast } = useGenerationToast();
const shotTypeHelp = useHelpPopover({
  storageKey: 'scene-editor:shot-type-help',
  defaultOpen: true,
  openOnce: true,
});

const form = ref({
  gridMode: 'SHOT_VARIATIONS' as GridMode,
  layout: DEFAULT_GRID_LAYOUT as GridLayout,
  shotTypes: [...DEFAULT_GRID_SHOT_TYPES] as string[],
  compositionHint: '',
  beats: [] as string[],
  continuityRules: '',
  additionalDetail: '',
  prompt: '',
  promptKo: '',
  promptEnFinal: '',
  promptEnFinalOverride: '',
  usePromptOverride: false,
});

const isPromptEditing = ref(false);
const isFinalEditing = ref(false);
const finalPromptSignature = ref('');
const finalPromptSourcePromptSnapshot = ref('');
const approvedFinalPromptSnapshot = ref('');
const timelineCutsSectionRef = ref<HTMLElement | null>(null);
const isTimelineCutsEditorOpen = ref(false);
let promptPreviewTimeout: ReturnType<typeof setTimeout> | null = null;

const {
  isGeneratingPrompt,
  isGeneratingJob: isGeneratingGrid,
  clearError,
  generatePrompt,
  approvePrompt,
  refreshPromptPreview,
  runGeneration: generateGrid,
} = useNodeGeneration({
  nodeId: props.node.id,
  nodeType: 'GRID',
  toastType: 'grid',
  getPrompt: () => form.value.prompt,
  getPromptPayload: () => ({
    nodeType: 'GRID',
    sceneOneLine: buildSceneOneLine(),
    prompt: form.value.prompt,
    additionalDetail: form.value.additionalDetail,
    gridMode: form.value.gridMode,
    layout: form.value.layout,
    timelineIntervalSeconds: TIMELINE_CUT_INTERVAL_SECONDS,
    ...(form.value.gridMode === 'SHOT_VARIATIONS'
      ? {
        shotTypes: form.value.shotTypes,
        compositionHint: form.value.compositionHint,
      }
      : {}),
  }),
  getPromptUpdate: (result) => ({
    gridMode: form.value.gridMode,
    layout: form.value.layout,
    shotTypes: form.value.shotTypes,
    compositionHint: form.value.compositionHint,
    beats: (form.value.gridMode === 'STORY_BEATS' && result.timelineCuts && result.timelineCuts.length > 0)
      ? normalizeBeats(result.timelineCuts, form.value.layout)
      : form.value.beats,
    continuityRules: form.value.continuityRules,
    prompt: result.promptKo || result.promptEnBase,
    promptKo: result.promptKo || result.promptEnBase,
  }),
  getImproveInstruction: () => form.value.additionalDetail,
  getApprovedUpdate: () => ({
    prompt: form.value.prompt,
    promptKo: form.value.prompt,
    promptEnFinalOverride: form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  }),
  getJobSettings: () => {
    if (form.value.gridMode === 'STORY_BEATS') {
      return {
        gridMode: 'STORY_BEATS',
        layout: form.value.layout,
        beatsKo: form.value.beats,
        continuityRulesKo: form.value.continuityRules,
        detailKo: form.value.additionalDetail,
      };
    }
    return {
      gridMode: 'SHOT_VARIATIONS',
      layout: form.value.layout,
      shotTypes: mapShotTypeLabelsToKeys(form.value.shotTypes),
      compositionHintKo: form.value.compositionHint,
      detailKo: form.value.additionalDetail,
    };
  },
  getReferenceObjectIds: () =>
    activeMasterObjectIds.value.length ? [...activeMasterObjectIds.value] : undefined,
  getPromptOverride: () =>
    form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  getPromptPreviewPayload: () => ({
    prompt: form.value.prompt,
    settings: (() => {
      if (form.value.gridMode === 'STORY_BEATS') {
        return {
          gridMode: 'STORY_BEATS',
          layout: form.value.layout,
          beatsKo: form.value.beats,
          continuityRulesKo: form.value.continuityRules,
          detailKo: form.value.additionalDetail,
        };
      }
      return {
        gridMode: 'SHOT_VARIATIONS',
        layout: form.value.layout,
        shotTypes: mapShotTypeLabelsToKeys(form.value.shotTypes),
        compositionHintKo: form.value.compositionHint,
        detailKo: form.value.additionalDetail,
      };
    })(),
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
    jobError: '그리드 생성에 실패했습니다. 다시 시도해주세요.',
  },
});

const layoutOptions: GridLayout[] = ['2x2', '2x3', '3x3'];
const gridModeOptions = [
  { value: 'SHOT_VARIATIONS', label: '샷 변주' },
  { value: 'STORY_BEATS', label: '타임라인 컷' },
] as const;
const TIMELINE_CUT_INTERVAL_SECONDS = 2;
const shotTypeHelpItems = [
  {
    label: '와이드샷',
    description: '넓은 배경과 인물 전체를 함께 담아 분위기를 보여줍니다.',
  },
  {
    label: '미디엄샷',
    description: '인물의 상반신 위주로 감정과 제스처를 전달합니다.',
  },
  {
    label: '클로즈업',
    description: '얼굴이나 중요한 디테일을 강조해 감정을 집중시킵니다.',
  },
  {
    label: '익스트림 클로즈업',
    description: '눈/손 등 극단적 디테일을 크게 확대합니다.',
  },
  {
    label: '오버숄더',
    description: '상대와의 대화를 어깨 너머 시점으로 보여줍니다.',
  },
  {
    label: 'POV',
    description: '인물 시점으로 장면을 보여줍니다.',
  },
];
const shotTypeOptions = shotTypeHelpItems.map((item) => item.label);

const data = computed(() => props.node.data as StoryboardGridNodeData | undefined);
const isGeneratingFinalPrompt = computed(() => Boolean(data.value?.isFinalPromptGenerating));
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
const isStoryBeats = computed(() => form.value.gridMode === 'STORY_BEATS');
const timelineCutsCount = computed(() => form.value.beats.filter((beat) => beat.trim().length > 0).length);
const timelineCutsButtonLabel = computed(() => {
  if (isTimelineCutsEditorOpen.value) return '접기';
  if (timelineCutsCount.value > 0) return `컷 편집하기 (${timelineCutsCount.value}개 입력됨)`;
  return '컷 편집하기';
});
const sceneHeaderData = computed(() =>
  nodeStore.nodes.find((node) => node.data?.type === NodeType.SCENE_HEADER)?.data
);
const activeMasterNode = computed(() =>
  nodeStore.nodes.find((node) => node.data?.type === NodeType.MASTER_IMAGE && (node.data as MasterImageNodeData).isActive)
    ?? nodeStore.nodes.find((node) => node.data?.type === NodeType.MASTER_IMAGE)
);
const activeMasterPrompt = computed(() => {
  const masterData = activeMasterNode.value?.data as MasterImageNodeData | undefined;
  return {
    prompt: masterData?.prompt?.trim() ?? '',
    promptKo: masterData?.promptKo ?? '',
  };
});
const activeMasterObjectIds = computed(() => {
  const masterData = activeMasterNode.value?.data as MasterImageNodeData | undefined;
  return masterData?.objectIds ?? [];
});
const parentMasterData = computed(() =>
  nodeStore.nodes.find((node) => node.id === data.value?.parentNodeId)?.data
);
const isParentReady = computed(() => {
  const parent = parentMasterData.value as { jobStatus?: string; imageUrl?: string | null; thumbnailUrl?: string | null } | undefined;
  const hasImage = Boolean(parent?.thumbnailUrl || parent?.imageUrl);
  return parent?.jobStatus === JobStatus.SUCCEEDED && hasImage;
});
const isNodeGenerating = computed(() =>
  data.value?.jobStatus === JobStatus.RUNNING ||
  data.value?.generationState === GenerationState.REQUESTED
);
const isUiLocked = computed(
  () => isNodeGenerating.value || isGeneratingPrompt.value || isGeneratingFinalPrompt.value || isGeneratingGrid.value
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
  data.value?.promptStatus === PromptStatus.DRAFT ? 'AI 프롬프트 생성' : 'AI 프롬프트 재생성'
);
const finalPromptActionLabel = computed(() =>
  form.value.promptEnFinal.trim().length > 0 ? '최종 프롬프트 재생성' : '최종 프롬프트 생성'
);
const canGenerateGridResult = computed(() => {
  // 동일 최종 프롬프트로도 재생성을 허용하므로 dirty 여부는 활성 조건에서 제외한다.
  void isFinalPromptDirty.value;
  return !isUiLocked.value && isPromptApproved.value && !isFinalEditing.value;
});

function buildSceneOneLine(): string {
  const parts: string[] = [];
  const header = sceneHeaderData.value as { title?: string; description?: string } | undefined;
  if (header?.title) parts.push(`scene: ${header.title}`);
  if (header?.description) parts.push(`description: ${header.description}`);
  if (form.value.layout) parts.push(`layout: ${form.value.layout}`);
  if (form.value.gridMode === 'SHOT_VARIATIONS') {
    if (form.value.shotTypes.length) parts.push(`shotTypes: ${form.value.shotTypes.join(', ')}`);
    if (form.value.compositionHint) parts.push(`composition: ${form.value.compositionHint}`);
  } else {
    parts.push(`timelineInterval: ${TIMELINE_CUT_INTERVAL_SECONDS}s`);
    const beats = form.value.beats.filter((beat) => beat.trim().length > 0);
    if (beats.length) parts.push(`timelineCuts: ${beats.join(' | ')}`);
    if (form.value.continuityRules) parts.push(`continuityRules: ${form.value.continuityRules}`);
  }
  if (form.value.additionalDetail) parts.push(`detail: ${form.value.additionalDetail}`);
  return parts.join(', ');
}

function buildFinalPromptSignature(): string {
  return JSON.stringify({
    gridMode: form.value.gridMode,
    layout: form.value.layout,
    shotTypes: [...form.value.shotTypes],
    compositionHint: form.value.compositionHint.trim(),
    beats: [...form.value.beats],
    continuityRules: form.value.continuityRules.trim(),
    additionalDetail: form.value.additionalDetail.trim(),
    prompt: form.value.prompt.trim(),
    usePromptOverride: form.value.usePromptOverride,
    promptEnFinalOverride: form.value.promptEnFinalOverride.trim(),
  });
}

function getPanelCount(layout: GridLayout): number {
  const parts = layout.split('x');
  if (parts.length !== 2) return 0;
  const rows = Number(parts[0]);
  const cols = Number(parts[1]);
  if (!Number.isFinite(rows) || !Number.isFinite(cols)) return 0;
  return rows * cols;
}

function formatTimelineRange(index: number): string {
  const start = index * TIMELINE_CUT_INTERVAL_SECONDS;
  const end = start + TIMELINE_CUT_INTERVAL_SECONDS;
  return `${start}-${end}s`;
}

function normalizeBeats(beats: string[], layout: GridLayout): string[] {
  const count = getPanelCount(layout);
  const next = beats.slice(0, count);
  while (next.length < count) {
    next.push('');
  }
  return next;
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
  const layout = data.value.layout || DEFAULT_GRID_LAYOUT;
  const gridMode = data.value.gridMode ?? 'SHOT_VARIATIONS';
  form.value = {
    gridMode,
    layout,
    shotTypes: (data.value.shotTypes && data.value.shotTypes.length > 0)
      ? data.value.shotTypes
      : [...DEFAULT_GRID_SHOT_TYPES],
    compositionHint: data.value.compositionHint || '',
    beats: normalizeBeats(data.value.beats || [], layout),
    continuityRules: data.value.continuityRules || '',
    additionalDetail: data.value.additionalDetail || '',
    prompt: data.value.promptKo || data.value.prompt || '',
    promptKo: data.value.promptKo || data.value.prompt || '',
    promptEnFinal: data.value.promptEnFinal || '',
    promptEnFinalOverride: data.value.promptEnFinalOverride || '',
    usePromptOverride: Boolean(data.value.promptEnFinalOverride),
  };
  isPromptEditing.value = false;
  isFinalEditing.value = false;
  isTimelineCutsEditorOpen.value = false;
  maybeAutofillPromptFromMaster();
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
    data.value?.gridMode,
    data.value?.layout,
    data.value?.shotTypes,
    data.value?.compositionHint,
    data.value?.beats,
    data.value?.continuityRules,
  ],
  () => {
    if (!data.value) return;
    const layout = data.value.layout || DEFAULT_GRID_LAYOUT;
    form.value.gridMode = data.value.gridMode ?? 'SHOT_VARIATIONS';
    form.value.layout = layout;
    form.value.shotTypes = (data.value.shotTypes && data.value.shotTypes.length > 0)
      ? [...data.value.shotTypes]
      : [...DEFAULT_GRID_SHOT_TYPES];
    form.value.compositionHint = data.value.compositionHint || '';
    form.value.beats = normalizeBeats(data.value.beats || [], layout);
    form.value.continuityRules = data.value.continuityRules || '';
  }
);

watch(
  () => form.value.layout,
  (nextLayout) => {
    form.value.beats = normalizeBeats(form.value.beats, nextLayout);
  }
);

watch(
  () => form.value.gridMode,
  (nextMode) => {
    if (data.value?.gridMode !== nextMode) {
      nodeStore.updateNodeLocal(props.node.id, { gridMode: nextMode });
    }
    if (nextMode === 'STORY_BEATS') {
      form.value.beats = normalizeBeats(form.value.beats, form.value.layout);
      maybeAutofillPromptFromMaster();
    } else {
      isTimelineCutsEditorOpen.value = false;
    }
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
    const updates: Partial<StoryboardGridNodeData> = {};
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
    gridMode: form.value.gridMode,
    layout: form.value.layout,
    shotTypes: form.value.shotTypes.slice(),
    compositionHint: form.value.compositionHint,
    beats: form.value.beats.slice(),
    continuityRules: form.value.continuityRules,
    additionalDetail: form.value.additionalDetail,
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

function toggleShotType(type: string): void {
  const idx = form.value.shotTypes.indexOf(type);
  if (idx >= 0) {
    form.value.shotTypes.splice(idx, 1);
  } else {
    form.value.shotTypes.push(type);
  }
}

function maybeAutofillPromptFromMaster(): void {
  if (form.value.gridMode !== 'STORY_BEATS') return;
  if (form.value.prompt.trim()) return;
  const masterPrompt = activeMasterPrompt.value.promptKo || activeMasterPrompt.value.prompt;
  if (!masterPrompt) return;
  form.value.prompt = masterPrompt;
  form.value.promptKo = masterPrompt;
}

async function openTimelineCutsEditor(): Promise<void> {
  isTimelineCutsEditorOpen.value = true;
  await nextTick();
  if (!timelineCutsSectionRef.value) return;
  const container = timelineCutsSectionRef.value.closest('.base-panel__content') as HTMLElement | null;
  if (container) {
    const containerRect = container.getBoundingClientRect();
    const sectionRect = timelineCutsSectionRef.value.getBoundingClientRect();
    const currentScroll = container.scrollTop;
    const offset = sectionRect.top - containerRect.top;
    const centeredOffset = (container.clientHeight - sectionRect.height) / 2;
    const rawTarget = currentScroll + offset - centeredOffset;
    const maxScroll = Math.max(0, container.scrollHeight - container.clientHeight);
    const targetScroll = Math.min(Math.max(0, rawTarget), maxScroll);
    gsap.to(container, { scrollTop: targetScroll, duration: 0.45, ease: 'power2.out' });
  } else {
    timelineCutsSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }
  gsap.killTweensOf(timelineCutsSectionRef.value);
  gsap.fromTo(
    timelineCutsSectionRef.value,
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

function toggleTimelineCutsEditor(): void {
  if (isTimelineCutsEditorOpen.value) {
    isTimelineCutsEditorOpen.value = false;
    return;
  }
  void openTimelineCutsEditor();
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
  if (isGeneratingFinalPrompt.value || isGeneratingPrompt.value || isGeneratingGrid.value) return;
  if (!form.value.prompt.trim()) return;
  if (!force && !isNarrativePromptDirtyForFinal.value) return;
  const toastId = startGenerationToast('final_prompt');
  nodeStore.updateNodeLocal(props.node.id, { isFinalPromptGenerating: true });
  try {
    form.value.usePromptOverride = false;
    form.value.promptEnFinalOverride = '';
    isFinalEditing.value = false;
    const previewResult = await refreshPromptPreview(true);
    const nextPromptEnFinal = previewResult?.promptEnFinal ?? form.value.promptEnFinal;
    form.value.promptEnFinal = nextPromptEnFinal;
    await nextTick();
    await nodeStore.updateNode(props.node.id, {
      prompt: form.value.prompt,
      promptKo: form.value.promptKo,
      promptEnFinal: nextPromptEnFinal,
      promptEnFinalOverride: '',
    });
    finalPromptSignature.value = buildFinalPromptSignature();
    finalPromptSourcePromptSnapshot.value = form.value.prompt.trim();
    finishGenerationToast(toastId, 'final_prompt', 'success');
  } catch (error) {
    console.error('Failed to generate final prompt:', error);
    finishGenerationToast(toastId, 'final_prompt', 'error', {
      reason: error instanceof Error ? error.message : '알 수 없는 오류',
    });
  } finally {
    nodeStore.updateNodeLocal(props.node.id, { isFinalPromptGenerating: false });
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

function notifyBlocked(title: string, message: string): void {
  uiStore.showToast({
    type: 'warning',
    title,
    message,
  });
}

function handleGenerateGrid(): void {
  if (isGeneratingGrid.value || isGeneratingPrompt.value) return;
  if (!isParentReady.value) {
    notifyBlocked('그리드 생성 불가', '상위 MASTER 이미지가 준비되어야 그리드를 생성할 수 있습니다.');
    return;
  }
  if (!isPromptGenerated.value) {
    notifyBlocked('프롬프트 필요', '먼저 프롬프트를 생성해 주세요.');
    return;
  }
  if (!isPromptApproved.value) {
    notifyBlocked('프롬프트 승인 필요', '승인 후 그리드를 생성할 수 있습니다.');
    return;
  }
  generateGrid();
}

</script>

<template>
  <BasePanel title="스토리보드 그리드 생성" :icon="LayoutGrid">
    <template v-if="data">
      <fieldset class="panel-lock-fieldset" :disabled="isUiLocked">
      <!-- Grid Mode -->
      <div class="panel-section">
        <label class="panel-label">
          <LayoutGrid class="panel-label-icon" />
          그리드 모드
        </label>
        <div class="panel-button-group">
          <button
            v-for="opt in gridModeOptions"
            :key="opt.value"
            :class="['panel-button-option', { active: form.gridMode === opt.value }]"
            @click="form.gridMode = opt.value"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>

      <!-- Layout -->
      <div class="panel-section">
        <label class="panel-label">
          <LayoutGrid class="panel-label-icon" />
          레이아웃
        </label>
        <div class="panel-button-group">
          <button
            v-for="opt in layoutOptions"
            :key="opt"
            :class="['panel-button-option', { active: form.layout === opt }]"
            @click="form.layout = opt"
          >
            {{ opt }}
          </button>
        </div>
      </div>

      <!-- Shot Types -->
      <div v-if="form.gridMode === 'SHOT_VARIATIONS'" class="panel-section">
        <div class="panel-label-row">
          <label class="panel-label">
            <Camera class="panel-label-icon" />
            샷 타입 (다중 선택)
          </label>
          <div :ref="shotTypeHelp.popoverRef" class="panel-info">
            <button
              type="button"
              class="panel-info-button"
              aria-label="샷 타입 안내"
              :aria-expanded="shotTypeHelp.isOpen.value"
              @click="shotTypeHelp.toggle"
            >
              <span class="panel-info-icon">i</span>
            </button>
            <div v-if="shotTypeHelp.isOpen.value" class="panel-info-popover">
              <div class="panel-info-title">샷 타입 안내</div>
              <ul class="panel-info-list">
                <li v-for="item in shotTypeHelpItems" :key="item.label" class="panel-info-item">
                  <span class="panel-info-label">{{ item.label }}</span>
                  <span class="panel-info-desc">{{ item.description }}</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
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

      <!-- Composition Hint -->
      <div v-if="form.gridMode === 'SHOT_VARIATIONS'" class="panel-section">
        <label class="panel-label">
          <Target class="panel-label-icon" />
          구도 힌트 (선택)
        </label>
        <input
          v-model="form.compositionHint"
          class="panel-input"
          placeholder="예: 대칭 구도, 삼분할 등"
        />
      </div>

      <!-- Timeline Cuts -->
      <div v-if="isStoryBeats" class="panel-section" ref="timelineCutsSectionRef">
        <div class="panel-label-row">
          <label class="panel-label">
            <FileText class="panel-label-icon" />
            타임라인 컷
          </label>
          <button class="panel-btn panel-btn--text" type="button" @click="toggleTimelineCutsEditor">
            {{ timelineCutsButtonLabel }}
          </button>
        </div>
        <p class="panel-subtext">
          AI로 생성/재생성을 누르면 2초 간격의 정지 프레임이 자동으로 채워집니다.
        </p>
        <div v-if="isTimelineCutsEditorOpen" class="panel-beats">
          <div v-for="(_, index) in form.beats" :key="`beat-${index}`" class="panel-beat-item">
            <div class="panel-beat-label">컷 {{ index + 1 }} · {{ formatTimelineRange(index) }}</div>
            <textarea
              v-model="form.beats[index]"
              class="panel-textarea panel-textarea--beat"
              rows="2"
              placeholder="예: 0-2s 구간 장면 설명"
            ></textarea>
          </div>
        </div>
      </div>

      <!-- Continuity Rules -->
      <div v-if="isStoryBeats" class="panel-section">
        <label class="panel-label">
          <Target class="panel-label-icon" />
          연속성 규칙 (선택)
        </label>
        <input
          v-model="form.continuityRules"
          class="panel-input"
          placeholder="예: 인물/의상/조명 유지"
        />
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
          placeholder="예: 소품/질감/감정 톤 등 추가 지시사항"
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
          placeholder="예: 캐릭터의 결심 순간을 담은 장면 서술"
          :readonly="!isPromptEditing"
          @wheel="handlePromptWheel"
        ></textarea>
      </div>

      <!-- Generate Prompt -->
      <div class="panel-generate-row">
        <button
          class="panel-btn panel-btn--secondary panel-btn--full panel-btn--prompt-generate"
          :disabled="isGeneratingPrompt || isGeneratingGrid"
          @click="generatePrompt"
        >
          <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon panel-btn-icon--spin" />
          {{ isGeneratingPrompt ? '생성 중...' : aiPromptActionLabel }}
        </button>
      </div>

      <!-- Generated Prompt -->
      <div class="panel-section panel-section--prompt">
        <div class="panel-label-row">
          <label class="panel-label">
            <FileText class="panel-label-icon" />
            최종 프롬프트 (영어)
          </label>
          <button
            v-if="isPromptGenerated"
            :class="['panel-btn', isFinalEditing ? 'panel-btn--success' : 'panel-btn--text']"
            :disabled="!form.promptEnFinal && !form.prompt"
            @click="toggleFinalEditing"
          >
            {{ isFinalEditing ? '편집 완료' : '영문 직접 편집' }}
          </button>
        </div>

        <template v-if="isPromptGenerated">
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
        </template>
        <textarea
          v-else
          :value="form.promptEnFinal"
          class="panel-textarea panel-textarea--prompt"
          rows="5"
          readonly
          placeholder="프롬프트를 생성해주세요."
          @wheel="handlePromptWheel"
        ></textarea>

        <button
          class="panel-btn panel-btn--secondary panel-btn--full panel-btn--final-prompt"
          :disabled="isUiLocked || !form.prompt.trim()"
          @click="generateFinalPrompt(true)"
        >
          <Loader2 v-if="isGeneratingFinalPrompt" class="panel-btn-icon panel-btn-icon--spin" />
          {{ isGeneratingFinalPrompt ? '생성 중...' : finalPromptActionLabel }}
        </button>

        <template v-if="isPromptGenerated">
          <div class="panel-prompt-actions panel-prompt-actions--right">
            <button
              v-if="!isPromptApproved"
              class="panel-btn panel-btn--success"
              :disabled="isUiLocked || isFinalEditing"
              @click="approvePrompt"
            >
              <Check class="panel-btn-icon" /> 승인
            </button>
            <span v-else class="panel-status panel-status--success">
              <Check class="panel-status-icon" />
              승인됨
            </span>
          </div>
        </template>
      </div>
      </fieldset>
    </template>

    <template #footer>
      <button
        class="panel-btn panel-btn--primary panel-btn--full"
        :disabled="!canGenerateGridResult"
        @click="handleGenerateGrid"
      >
        <LayoutGrid class="panel-btn-icon" />
        그리드 생성
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

.panel-beats {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.panel-beat-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.panel-beat-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--gray-700, #374151);
}
</style>
