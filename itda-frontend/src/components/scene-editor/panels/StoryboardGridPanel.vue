<script setup lang="ts">
/**
 * StoryboardGridPanel - 스토리보드 그리드 생성/편집 패널
 */
import { ref, computed, watch } from 'vue';
import type { Node } from '@vue-flow/core';
import type { StoryboardGridNodeData, GridLayout } from '../../../types/node';
import { PromptStatus, JobStatus } from '../../../types/node';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { aiService } from '../../../services';
import { LayoutGrid, Camera, Target, FileText, Sparkles, Check, RefreshCw, Loader2 } from 'lucide-vue-next';

interface Props {
  node: Node<StoryboardGridNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();

// 로딩 상태
const isGeneratingPrompt = ref(false);
const isGeneratingGrid = ref(false);
const errorMessage = ref<string | null>(null);

const form = ref({
  layout: '2x3' as GridLayout,
  shotTypes: [] as string[],
  compositionHint: '',
  prompt: '',
});

const layoutOptions: GridLayout[] = ['2x2', '2x3', '3x3'];
const shotTypeOptions = ['와이드샷', '미디엄샷', '클로즈업', '익스트림 클로즈업', '오버숄더', 'POV'];

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
  errorMessage.value = null;
}, { immediate: true });

function toggleShotType(type: string): void {
  const idx = form.value.shotTypes.indexOf(type);
  if (idx >= 0) {
    form.value.shotTypes.splice(idx, 1);
  } else {
    form.value.shotTypes.push(type);
  }
}

async function generatePrompt(): Promise<void> {
  isGeneratingPrompt.value = true;
  errorMessage.value = null;

  try {
    const prompt = await aiService.generatePrompt({
      nodeType: 'GRID',
      layout: form.value.layout,
      shotTypes: form.value.shotTypes,
      compositionHint: form.value.compositionHint,
    });

    form.value.prompt = prompt;
    nodeStore.updateNode(props.node.id, { ...form.value, promptStatus: PromptStatus.GENERATED });
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

async function generateGrid(): Promise<void> {
  if (!form.value.prompt) return;

  isGeneratingGrid.value = true;
  errorMessage.value = null;

  try {
    nodeStore.updateNode(props.node.id, { jobStatus: JobStatus.RUNNING });

    const jobId = await aiService.generateNode(props.node.id, form.value.prompt);
    console.log('Grid generation job started:', jobId);

    const result = await aiService.pollJobUntilComplete(jobId, (status) => {
      console.log('Job status:', status.status);
    });

    if (result.status === 'SUCCEEDED') {
      nodeStore.updateNode(props.node.id, {
        jobStatus: JobStatus.SUCCEEDED,
        imageUrl: result.resultUrl,
        thumbnailUrl: result.thumbnailUrl,
      });
    } else {
      throw new Error(result.error?.message || 'Grid generation failed');
    }
  } catch (error) {
    console.error('Failed to generate grid:', error);
    errorMessage.value = '그리드 생성에 실패했습니다. 다시 시도해주세요.';
    nodeStore.updateNode(props.node.id, { jobStatus: JobStatus.FAILED });
  } finally {
    isGeneratingGrid.value = false;
  }
}
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
