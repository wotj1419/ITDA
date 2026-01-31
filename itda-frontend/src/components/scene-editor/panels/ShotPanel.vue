<script setup lang="ts">
/**
 * ShotPanel - 샷 생성/편집 패널
 */
import { ref, computed, watch, nextTick, onUnmounted } from 'vue';
import type { Node } from '@vue-flow/core';
import type { MasterImageNodeData, ShotNodeData, StoryboardGridNodeData } from '../../../types/ui/sceneNodes';
import { JobStatus, NodeType, PromptStatus } from '../../../types/ui/sceneNodes';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useObjectStore } from '../../../stores/object';
import { useUIStore } from '../../../stores/ui';
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { Camera, Smile, PenLine, FileText, Sparkles, Check, RefreshCw, LayoutGrid, Loader2 } from 'lucide-vue-next';
import { gsap } from 'gsap';
import { resolveExpressionKey, resolveShotTypeKey } from '../../../utils/nodeSettings';
import { DEFAULT_GRID_LAYOUT } from '../../../utils/nodeDefaults';
import { aiService } from '../../../services';

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
  promptLang: 'EN' as 'EN' | 'KO',
});

const koDirty = ref(false);
const lastSyncedKo = ref('');
const promptSectionRef = ref<HTMLElement | null>(null);
const promptKoRef = ref<HTMLTextAreaElement | null>(null);
const isFinalEditing = ref(false);
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
const isKoOutOfSync = computed(() => koDirty.value);
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
  getPrompt: () => form.value.prompt,
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
    prompt: result.promptEnBase,
    promptKo: result.promptKo,
  }),
  getApprovedUpdate: () => ({
    prompt: form.value.prompt,
    promptKo: form.value.promptKo,
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
    form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  getPromptPreviewPayload: () => ({
    prompt: form.value.prompt,
    settings: {
      gridCellIndex: data.value?.gridCellIndex ?? 0,
      shotType: resolveShotTypeKey(form.value.shotTypes[0] ?? data.value?.shotType),
      expressionKey: resolveExpressionKey(form.value.expression),
      detailKo: form.value.additionalDetail,
      gridCellCutKo: gridCellCutKo.value,
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
    jobError: '샷 생성에 실패했습니다. 다시 시도해주세요.',
  },
});

const isParentReady = computed(() => {
  const parent = parentGridData.value as { jobStatus?: string; imageUrl?: string | null; thumbnailUrl?: string | null } | undefined;
  const hasImage = Boolean(parent?.thumbnailUrl || parent?.imageUrl);
  return parent?.jobStatus === JobStatus.SUCCEEDED && hasImage;
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

const isRewriting = ref(false);

async function rewritePrompt(): Promise<void> {
  if (!form.value.promptKo) return;
  if (isRewriting.value) return;
  const sourceKo = form.value.promptKo;
  isRewriting.value = true;
  try {
    const result = await aiService.rewritePrompt(form.value.promptKo);
    if (result.promptEnBase && result.promptEnBase.trim()) {
      form.value.prompt = result.promptEnBase;
      koDirty.value = false;
      lastSyncedKo.value = sourceKo;
      queuePromptPreview();
    }
  } catch (error) {
    console.error('Failed to rewrite prompt:', error);
  } finally {
    isRewriting.value = false;
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

function markKoDirty(): void {
  if (form.value.promptKo === lastSyncedKo.value) {
    koDirty.value = false;
    return;
  }
  koDirty.value = true;
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

async function focusKoEditor(): Promise<void> {
  form.value.promptLang = 'KO';
  await nextTick();
  if (promptSectionRef.value) {
    const container = promptSectionRef.value.closest('.base-panel__content') as HTMLElement | null;
    if (container) {
      const containerRect = container.getBoundingClientRect();
      const sectionRect = promptSectionRef.value.getBoundingClientRect();
      const currentScroll = container.scrollTop;
      const offset = sectionRect.top - containerRect.top;
      const centeredOffset = (container.clientHeight - sectionRect.height) / 2;
      const rawTarget = currentScroll + offset - centeredOffset;
      const maxScroll = Math.max(0, container.scrollHeight - container.clientHeight);
      const targetScroll = Math.min(Math.max(0, rawTarget), maxScroll);
      gsap.to(container, { scrollTop: targetScroll, duration: 0.45, ease: 'power2.out' });
    } else {
      promptSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
    gsap.killTweensOf(promptSectionRef.value);
    gsap.fromTo(
      promptSectionRef.value,
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
  if (promptKoRef.value) {
    promptKoRef.value.focus();
  }
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
    prompt: data.value.prompt || '',
    promptKo: data.value.promptKo || '',
    promptEnFinal: data.value.promptEnFinal || '',
    promptEnFinalOverride: data.value.promptEnFinalOverride || '',
    usePromptOverride: Boolean(data.value.promptEnFinalOverride),
    promptLang: 'EN',
  };
  koDirty.value = false;
  lastSyncedKo.value = form.value.promptKo;
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
    lastSyncedKo.value = normalized;
    koDirty.value = false;
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
  () => form.value.promptLang,
  (next, prev) => {
    if (prev === 'KO' && next === 'EN' && koDirty.value) {
      rewritePrompt();
    }
  }
);

watch(
  () => [
    form.value.prompt,
    form.value.promptKo,
    form.value.promptEnFinalOverride,
    form.value.usePromptOverride,
  ],
  () => {
    if (!data.value) return;
    const updates: Partial<ShotNodeData> = {};
    if (form.value.prompt !== (data.value.prompt ?? '')) {
      updates.prompt = form.value.prompt;
    }
    if (form.value.promptKo !== (data.value.promptKo ?? '')) {
      updates.promptKo = form.value.promptKo;
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
  if (isKoOutOfSync.value) {
    notifyBlocked('영어 반영 필요', '한국어 수정 내용을 영어에 반영해 주세요.');
    return;
  }
  generateShot();
}
</script>

<template>
  <BasePanel :title="`샷 ${shotLabel} 생성`" :icon="Camera">
    <template v-if="data">
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

      <!-- Additional Detail -->
      <div class="panel-section">
        <label class="panel-label">
          <PenLine class="panel-label-icon" />
          추가 디테일 (선택)
        </label>
        <textarea
          v-model="form.additionalDetail"
          class="panel-textarea"
          rows="2"
          placeholder="추가 지시사항 입력"
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
        <p class="panel-subtext">
          {{ form.promptLang === 'EN' ? '원본(편집 가능)' : '번역(수정 가능)' }}
        </p>
        <div v-if="form.promptLang === 'EN'">
          <textarea
            v-model="form.prompt"
            class="panel-textarea panel-textarea--prompt"
            rows="4"
            placeholder="예: A quiet alley glows with neon reflections as the character pauses."
          ></textarea>
        </div>
        <div v-else class="panel-translation-block">
          <textarea
            ref="promptKoRef"
            v-model="form.promptKo"
            class="panel-textarea panel-textarea--prompt"
            rows="4"
            @input="markKoDirty"
          ></textarea>
          <div class="panel-prompt-actions">
            <button
              class="panel-btn panel-btn--success panel-btn--sync"
              :class="{ 'panel-btn--sync--muted': !isKoOutOfSync }"
              :disabled="isRewriting || !form.promptKo || !isKoOutOfSync"
              @click="rewritePrompt"
            >
              <Loader2 v-if="isRewriting" class="panel-btn-icon panel-btn-icon--spin" />
              <RefreshCw v-else class="panel-btn-icon" />
              영어로 반영
            </button>
          </div>
        </div>
      </div>

      <!-- Generate Prompt -->
      <button
        class="panel-btn panel-btn--secondary panel-btn--full panel-btn--prompt-generate"
        :disabled="isGeneratingPrompt || isGeneratingShot"
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
          <button class="panel-btn panel-btn--text" @click="focusKoEditor">
            한국어로 편집
          </button>
        </div>

        <div class="panel-prompt-actions panel-prompt-actions--right">
          <span v-if="isKoOutOfSync" class="panel-subtext">
            영어 반영이 필요합니다.
          </span>
          <button class="panel-btn panel-btn--text" :disabled="isGeneratingPrompt || isGeneratingShot" @click="generatePrompt">
            <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon panel-btn-icon--spin" />
            <RefreshCw v-else class="panel-btn-icon" />
            재생성
          </button>
          <button
            v-if="!isPromptApproved"
            class="panel-btn panel-btn--success"
            :disabled="isGeneratingPrompt || isGeneratingShot || isKoOutOfSync"
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
      <button
        class="panel-btn panel-btn--primary panel-btn--full"
        :disabled="isGeneratingShot || isGeneratingPrompt || isKoOutOfSync"
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
