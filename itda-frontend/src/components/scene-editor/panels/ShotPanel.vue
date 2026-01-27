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
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { Camera, Smile, PenLine, FileText, Sparkles, Check, RefreshCw, LayoutGrid } from 'lucide-vue-next';
import { resolveExpressionKey, resolveShotTypeKey } from '../../../utils/nodeSettings';

interface Props {
  node: Node<ShotNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();

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
const gridLayout = computed(() => parentGridData.value?.layout || '2x3');
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

const {
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
    objectIds: activeMasterData.value?.objectIds,
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
const canGenerate = computed(() =>
  isPromptApproved.value && isParentReady.value && !isGeneratingShot.value
);

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
</script>

<template>
  <BasePanel :title="`샷 ${shotLabel} 생성`" :icon="Camera">
    <template v-if="data">
      <p v-if="!isParentReady" class="panel-hint">
        상위 GRID 이미지가 준비되어야 샷을 생성할 수 있습니다.
      </p>
      <!-- Grid Cell Info -->
      <div class="panel-info">
        <span class="panel-info-label">그리드 셀:</span>
        <span class="panel-info-value">#{{ data.gridCellIndex + 1 }}</span>
      </div>

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
        <p class="panel-hint">레이아웃: {{ gridLayout }}</p>
      </div>

      <!-- Shot Type -->
      <div class="panel-section">
        <label class="panel-label">
          <Camera class="panel-label-icon" />
          샷 타입 (다중 선택)
        </label>
        <div class="panel-checkbox-group">
          <label v-for="opt in shotTypeOptions" :key="opt" class="panel-checkbox">
            <input
              type="checkbox"
              :checked="form.shotTypes.includes(opt)"
              @change="toggleShotType(opt)"
            />
            <span class="panel-checkbox-label">{{ opt }}</span>
          </label>
        </div>
      </div>

      <!-- Expression -->
      <div class="panel-section">
        <label class="panel-label">
          <Smile class="panel-label-icon" />
          표정/분위기
        </label>
        <div class="panel-radio-group">
          <label v-for="opt in expressionOptions" :key="opt" class="panel-radio">
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
      <button
        class="panel-btn panel-btn--primary panel-btn--full"
        :disabled="!canGenerate"
        @click="generateShot"
      >
        <Camera class="panel-btn-icon" />
        샷 생성
      </button>
    </template>
  </BasePanel>
</template>
