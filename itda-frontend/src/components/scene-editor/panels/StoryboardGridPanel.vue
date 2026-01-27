<script setup lang="ts">
/**
 * StoryboardGridPanel - 스토리보드 그리드 생성/편집 패널
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import type { Node as VueFlowNode } from '@vue-flow/core';
import type { StoryboardGridNodeData, GridLayout, GridMode } from '../../../types/ui/sceneNodes';
import { JobStatus, NodeType, PromptStatus } from '../../../types/ui/sceneNodes';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { LayoutGrid, Camera, Target, FileText, Sparkles, Check, RefreshCw } from 'lucide-vue-next';
import { mapShotTypeLabelsToKeys } from '../../../utils/nodeSettings';

interface Props {
  node: VueFlowNode<StoryboardGridNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();
const shotTypeHelpRef = ref<HTMLElement | null>(null);
const isShotTypeHelpOpen = ref(false);

const form = ref({
  gridMode: 'SHOT_VARIATIONS' as GridMode,
  layout: '2x3' as GridLayout,
  shotTypes: [] as string[],
  compositionHint: '',
  beats: [] as string[],
  continuityRules: '',
  prompt: '',
});

const {
  isGeneratingJob: isGeneratingGrid,
  clearError,
  generatePrompt,
  approvePrompt,
  runGeneration: generateGrid,
} = useNodeGeneration({
  nodeId: props.node.id,
  nodeType: 'GRID',
  toastType: 'grid',
  getPrompt: () => form.value.prompt,
  getPromptPayload: () => ({
    nodeType: 'GRID',
    sceneOneLine: buildSceneOneLine(),
    layout: form.value.layout,
    ...(form.value.gridMode === 'SHOT_VARIATIONS'
      ? {
        shotTypes: form.value.shotTypes,
        compositionHint: form.value.compositionHint,
      }
      : {}),
  }),
  getPromptUpdate: (prompt) => ({
    gridMode: form.value.gridMode,
    layout: form.value.layout,
    shotTypes: form.value.shotTypes,
    compositionHint: form.value.compositionHint,
    beats: form.value.beats,
    continuityRules: form.value.continuityRules,
    prompt,
  }),
  getApprovedUpdate: () => ({ prompt: form.value.prompt }),
  getJobSettings: () => {
    if (form.value.gridMode === 'STORY_BEATS') {
      return {
        gridMode: 'STORY_BEATS',
        layout: form.value.layout,
        beatsKo: form.value.beats,
        continuityRulesKo: form.value.continuityRules,
      };
    }
    return {
      gridMode: 'SHOT_VARIATIONS',
      layout: form.value.layout,
      shotTypes: mapShotTypeLabelsToKeys(form.value.shotTypes),
      compositionHintKo: form.value.compositionHint,
    };
  },
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
  { value: 'STORY_BEATS', label: '스토리 비트' },
] as const;
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
const isPromptGenerated = computed(() => data.value?.promptStatus !== PromptStatus.DRAFT);
const isPromptApproved = computed(() => data.value?.promptStatus === PromptStatus.APPROVED);
const isStoryBeats = computed(() => form.value.gridMode === 'STORY_BEATS');
const sceneHeaderData = computed(() =>
  nodeStore.nodes.find((node) => node.data?.type === NodeType.SCENE_HEADER)?.data
);
const parentMasterData = computed(() =>
  nodeStore.nodes.find((node) => node.id === data.value?.parentNodeId)?.data
);
const isParentReady = computed(() => {
  const parent = parentMasterData.value as { jobStatus?: string; imageUrl?: string | null; thumbnailUrl?: string | null } | undefined;
  const hasImage = Boolean(parent?.thumbnailUrl || parent?.imageUrl);
  return parent?.jobStatus === JobStatus.SUCCEEDED && hasImage;
});
const canGenerate = computed(() =>
  isPromptApproved.value && isParentReady.value && !isGeneratingGrid.value
);

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
    const beats = form.value.beats.filter((beat) => beat.trim().length > 0);
    if (beats.length) parts.push(`beats: ${beats.join(' | ')}`);
    if (form.value.continuityRules) parts.push(`continuityRules: ${form.value.continuityRules}`);
  }
  return parts.join(', ');
}

function getPanelCount(layout: GridLayout): number {
  const parts = layout.split('x').map((value) => Number(value));
  const rows = parts[0] ?? Number.NaN;
  const cols = parts[1] ?? Number.NaN;
  if (!Number.isFinite(rows) || !Number.isFinite(cols)) return 0;
  return rows * cols;
}

function normalizeBeats(beats: string[], layout: GridLayout): string[] {
  const count = getPanelCount(layout);
  const next = beats.slice(0, count);
  while (next.length < count) {
    next.push('');
  }
  return next;
}

watch(() => props.node.id, () => {
  if (!data.value) return;
  const layout = data.value.layout || '2x3';
  const gridMode = data.value.gridMode ?? 'SHOT_VARIATIONS';
  form.value = {
    gridMode,
    layout,
    shotTypes: data.value.shotTypes || [],
    compositionHint: data.value.compositionHint || '',
    beats: normalizeBeats(data.value.beats || [], layout),
    continuityRules: data.value.continuityRules || '',
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
    const layout = data.value.layout || '2x3';
    form.value.gridMode = data.value.gridMode ?? 'SHOT_VARIATIONS';
    form.value.layout = layout;
    form.value.shotTypes = [...(data.value.shotTypes || [])];
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
    if (nextMode === 'STORY_BEATS') {
      form.value.beats = normalizeBeats(form.value.beats, form.value.layout);
    }
  }
);

function toggleShotType(type: string): void {
  const idx = form.value.shotTypes.indexOf(type);
  if (idx >= 0) {
    form.value.shotTypes.splice(idx, 1);
  } else {
    form.value.shotTypes.push(type);
  }
}

function toggleShotTypeHelp(event: MouseEvent): void {
  event.stopPropagation();
  isShotTypeHelpOpen.value = !isShotTypeHelpOpen.value;
}

function closeShotTypeHelp(): void {
  isShotTypeHelpOpen.value = false;
}

function handleDocumentClick(event: MouseEvent): void {
  if (!isShotTypeHelpOpen.value) return;
  const target = event.target as Node | null;
  if (!shotTypeHelpRef.value || !target) return;
  if (!shotTypeHelpRef.value.contains(target)) {
    closeShotTypeHelp();
  }
}

function handleDocumentKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape' && isShotTypeHelpOpen.value) {
    closeShotTypeHelp();
  }
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick);
  document.addEventListener('keydown', handleDocumentKeydown);
});

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick);
  document.removeEventListener('keydown', handleDocumentKeydown);
});

</script>

<template>
  <BasePanel title="스토리보드 그리드 생성" :icon="LayoutGrid">
    <template v-if="data">
      <p v-if="!isParentReady" class="panel-hint">
        상위 MASTER 이미지가 준비되어야 그리드를 생성할 수 있습니다.
      </p>
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
          <div ref="shotTypeHelpRef" class="panel-info">
            <button
              type="button"
              class="panel-info-button"
              aria-label="샷 타입 안내"
              :aria-expanded="isShotTypeHelpOpen"
              @click="toggleShotTypeHelp"
            >
              <span class="panel-info-icon">i</span>
            </button>
            <div v-if="isShotTypeHelpOpen" class="panel-info-popover">
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

      <!-- Story Beats -->
      <div v-if="isStoryBeats" class="panel-section">
        <label class="panel-label">
          <FileText class="panel-label-icon" />
          비트 입력
        </label>
        <div class="panel-beats">
          <div v-for="(_, index) in form.beats" :key="`beat-${index}`" class="panel-beat-item">
            <div class="panel-beat-label">비트 {{ index + 1 }}</div>
            <textarea
              v-model="form.beats[index]"
              class="panel-textarea panel-textarea--beat"
              rows="2"
              placeholder="예: 0~4s: 사건 설명"
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
        @click="generateGrid"
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
