<script setup lang="ts">
/**
 * ShotPanel - 샷 생성/편집 패널
 */
import { ref, computed, watch } from 'vue';
import type { Node } from '@vue-flow/core';
import type { ShotNodeData, StoryboardGridNodeData } from '../../../types/node';
import { NodeType, PromptStatus, JobStatus } from '../../../types/node';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useGenerationToast } from '../../../composables/useGenerationToast';
import { aiService } from '../../../services';
import { Camera, Smile, PenLine, FileText, Sparkles, Check, RefreshCw, LayoutGrid } from 'lucide-vue-next';

interface Props {
  node: Node<ShotNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();
const { startGenerationToast, finishGenerationToast } = useGenerationToast();

// 로딩 상태
const isGeneratingPrompt = ref(false);
const isGeneratingShot = ref(false);
const errorMessage = ref<string | null>(null);

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
const canGenerate = computed(() => isPromptApproved.value && !isGeneratingShot.value);
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
  errorMessage.value = null;
}, { immediate: true });

async function generatePrompt(): Promise<void> {
  isGeneratingPrompt.value = true;
  errorMessage.value = null;

  try {
    const shotTypeValue = buildShotTypeValue(form.value.shotTypes);
    const prompt = await aiService.generatePrompt({
      nodeType: 'SHOT',
      shotType: shotTypeValue,
      expression: form.value.expression,
      additionalDetail: form.value.additionalDetail,
    });

    form.value.prompt = prompt;
    nodeStore.updateNode(props.node.id, {
      shotType: shotTypeValue,
      shotTypes: [...form.value.shotTypes],
      expression: form.value.expression,
      additionalDetail: form.value.additionalDetail,
      prompt: form.value.prompt,
      promptStatus: PromptStatus.GENERATED,
    });
  } catch (error) {
    console.error('Failed to generate prompt:', error);
    errorMessage.value = '프롬프트 생성에 실패했습니다. 다시 시도해주세요.';
  } finally {
    isGeneratingPrompt.value = false;
  }
}

function approvePrompt(): void {
  nodeStore.updateNode(props.node.id, { prompt: form.value.prompt, promptStatus: PromptStatus.APPROVED });
}

async function generateShot(): Promise<void> {
  if (!form.value.prompt) return;

  isGeneratingShot.value = true;
  errorMessage.value = null;
  const toastId = startGenerationToast('shot');

  try {
    nodeStore.updateNode(props.node.id, { jobStatus: JobStatus.RUNNING });

    const jobId = await aiService.generateNode(props.node.id, form.value.prompt);
    console.log('Shot generation job started:', jobId);

    const result = await aiService.pollJobUntilComplete(jobId, (status) => {
      console.log('Job status:', status.status);
    });

    if (result.status === 'SUCCEEDED') {
      nodeStore.updateNode(props.node.id, {
        jobStatus: JobStatus.SUCCEEDED,
        imageUrl: result.resultUrl,
        thumbnailUrl: result.thumbnailUrl,
      });
      finishGenerationToast(toastId, 'shot', 'success');
    } else {
      throw new Error(result.error?.message || 'Shot generation failed');
    }
  } catch (error) {
    console.error('Failed to generate shot:', error);
    errorMessage.value = '샷 생성에 실패했습니다. 다시 시도해주세요.';
    nodeStore.updateNode(props.node.id, { jobStatus: JobStatus.FAILED });
    const reason = error instanceof Error ? error.message : '알 수 없는 오류';
    finishGenerationToast(toastId, 'shot', 'error', { reason });
  } finally {
    isGeneratingShot.value = false;
  }
}

function selectGridCell(index: number): void {
  nodeStore.updateNode(props.node.id, { gridCellIndex: index });
}
</script>

<template>
  <BasePanel :title="`샷 ${shotLabel} 생성`" :icon="Camera">
    <template v-if="data">
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
