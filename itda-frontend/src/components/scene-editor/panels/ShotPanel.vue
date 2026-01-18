<script setup lang="ts">
/**
 * ShotPanel - 샷 생성/편집 패널
 */
import { ref, computed, watch } from 'vue';
import type { Node } from '@vue-flow/core';
import type { ShotNodeData } from '../../../types/node';
import { PromptStatus } from '../../../types/node';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { Camera, Smile, PenLine, FileText, Sparkles, Check, RefreshCw } from 'lucide-vue-next';

interface Props {
  node: Node<ShotNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();

const form = ref({
  shotType: '',
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
const canGenerate = computed(() => isPromptApproved.value);

watch(() => props.node.id, () => {
  if (!data.value) return;
  form.value = {
    shotType: data.value.shotType || '',
    expression: data.value.expression || '',
    additionalDetail: data.value.additionalDetail || '',
    prompt: data.value.prompt || '',
  };
}, { immediate: true });

function generatePrompt(): void {
  const promptText = `High quality ${form.value.shotType} shot, expression: ${form.value.expression}. ${form.value.additionalDetail}`;
  form.value.prompt = promptText;
  nodeStore.updateNode(props.node.id, { ...form.value, promptStatus: PromptStatus.GENERATED });
}

function approvePrompt(): void {
  nodeStore.updateNode(props.node.id, { prompt: form.value.prompt, promptStatus: PromptStatus.APPROVED });
}

function generateShot(): void {
  console.log('Generate shot:', form.value);
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

      <!-- Shot Type -->
      <div class="panel-section">
        <label class="panel-label">
          <Camera class="panel-label-icon" />
          샷 타입
        </label>
        <select v-model="form.shotType" class="panel-select">
          <option value="">선택하세요</option>
          <option v-for="opt in shotTypeOptions" :key="opt" :value="opt">{{ opt }}</option>
        </select>
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
