<script setup lang="ts">
/**
 * StoryboardGridPanel - 스토리보드 그리드 생성/편집 패널
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import type { Node as VueFlowNode } from '@vue-flow/core';
import type { StoryboardGridNodeData, GridLayout } from '../../../types/ui/sceneNodes';
import { PromptStatus } from '../../../types/ui/sceneNodes';
import BasePanel from './BasePanel.vue';
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { LayoutGrid, Camera, Target, FileText, Sparkles, Check, RefreshCw } from 'lucide-vue-next';

interface Props {
  node: VueFlowNode<StoryboardGridNodeData>;
}

const props = defineProps<Props>();
const shotTypeHelpRef = ref<HTMLElement | null>(null);
const isShotTypeHelpOpen = ref(false);

const form = ref({
  layout: '2x3' as GridLayout,
  shotTypes: [] as string[],
  compositionHint: '',
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
    layout: form.value.layout,
    shotTypes: form.value.shotTypes,
    compositionHint: form.value.compositionHint,
  }),
  getPromptUpdate: (prompt) => ({
    layout: form.value.layout,
    shotTypes: form.value.shotTypes,
    compositionHint: form.value.compositionHint,
    prompt,
  }),
  getApprovedUpdate: () => ({ prompt: form.value.prompt }),
  getJobSettings: () => ({
    layout: form.value.layout,
    shotTypes: form.value.shotTypes,
    compositionHint: form.value.compositionHint,
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
const canGenerate = computed(() => isPromptApproved.value && !isGeneratingGrid.value);

watch(() => props.node.id, () => {
  if (!data.value) return;
  form.value = {
    layout: data.value.layout || '2x3',
    shotTypes: data.value.shotTypes || [],
    compositionHint: data.value.compositionHint || '',
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
      <div class="panel-section">
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
      <div class="panel-section">
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
</style>
