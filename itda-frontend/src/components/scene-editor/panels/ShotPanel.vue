<script setup lang="ts">
/**
 * ShotPanel - 샷 생성/편집 패널
 */
import { ref, computed, watch } from 'vue';
import type { Node } from '@vue-flow/core';
import type { MasterImageNodeData, ShotNodeData, StoryboardGridNodeData } from '../../../types/ui/sceneNodes';
import { JobStatus, NodeType, PromptStatus } from '../../../types/ui/sceneNodes';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useObjectStore } from '../../../stores/object';
import { useUIStore } from '../../../stores/ui';
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { Camera, Smile, PenLine, FileText, Sparkles, Check, RefreshCw, LayoutGrid, Loader2 } from 'lucide-vue-next';
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
  showAdvanced: false,
});

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
const gridCellCount = computed(() => {
  const match = gridLayout.value.match(/(\d+)x(\d+)/);
  if (!match) return 6;
  return Number(match[1]) * Number(match[2]);
});
const gridCellOptions = computed(() =>
  Array.from({ length: gridCellCount.value }, (_, index) => index)
);
const selectedGridCell = computed(() => data.value?.gridCellIndex ?? 0);
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
  }),
  getPromptOverride: () =>
    form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  getPromptPreviewPayload: () => ({
    prompt: form.value.prompt,
    settings: {
      gridCellIndex: data.value?.gridCellIndex ?? 0,
      shotType: resolveShotTypeKey(form.value.shotTypes[0] ?? data.value?.shotType),
      expressionKey: resolveExpressionKey(form.value.expression),
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
  if (form.value.shotTypes.length) {
    parts.push(`shotType: ${buildShotTypeValue(form.value.shotTypes)}`);
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

      <!-- Generate Prompt -->
      <button
        class="panel-btn panel-btn--secondary panel-btn--full panel-btn--prompt-generate"
        :disabled="isGeneratingPrompt || isGeneratingShot"
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
          <button class="panel-btn panel-btn--text" :disabled="isGeneratingPrompt || isGeneratingShot" @click="generatePrompt">
            <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon panel-btn-icon--spin" />
            <RefreshCw v-else class="panel-btn-icon" />
            재생성
          </button>
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
    </template>

    <template #footer>
      <button
        class="panel-btn panel-btn--primary panel-btn--full"
        :disabled="isGeneratingShot || isGeneratingPrompt"
        @click="handleGenerateShot"
      >
        <Camera class="panel-btn-icon" />
        샷 생성
      </button>
    </template>
  </BasePanel>
</template>
