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
});

const shotTypeOptions = ['와이드샷', '미디엄샷', '클로즈업', '익스트림 클로즈업'];
const expressionOptions = ['기본', '미소', '슬픔', '놀람', '분노', '무표정'];

const data = computed(() => props.node.data as ShotNodeData | undefined);
const shotLabel = computed(() => String.fromCharCode(65 + (data.value?.gridCellIndex || 0)));
const isPromptGenerated = computed(() => data.value?.promptStatus !== PromptStatus.DRAFT);
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
  getPromptUpdate: (prompt) => ({
    shotType: buildShotTypeValue(form.value.shotTypes),
    shotTypes: [...form.value.shotTypes],
    expression: form.value.expression,
    additionalDetail: form.value.additionalDetail,
    prompt,
  }),
  getApprovedUpdate: () => ({ prompt: form.value.prompt }),
  getJobSettings: () => ({
    gridCellIndex: data.value?.gridCellIndex ?? 0,
    shotType: resolveShotTypeKey(form.value.shotTypes[0] ?? data.value?.shotType),
    expressionKey: resolveExpressionKey(form.value.expression),
    detailKo: form.value.additionalDetail,
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
  if (isGeneratingShot.value) return;
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
          AI 프롬프트
          <span class="panel-label-badge">생성됨</span>
        </label>
        <textarea v-model="form.prompt" class="panel-textarea panel-textarea--prompt" rows="3"></textarea>
        <div class="panel-prompt-actions panel-prompt-actions--right">
          <button class="panel-btn panel-btn--text" :disabled="isGeneratingPrompt || isGeneratingShot" @click="generatePrompt">
            <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon panel-btn-icon--spin" />
            <RefreshCw v-else class="panel-btn-icon" />
            재생성
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
