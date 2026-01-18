<script setup lang="ts">
/**
 * MasterImagePanel - 마스터 이미지 생성/편집 패널
 */
import { ref, computed, watch } from 'vue';
import type { Node } from '@vue-flow/core';
import type { MasterImageNodeData } from '../../../types/node';
import { PromptStatus } from '../../../types/node';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { Film, Palette, Sun, Smile, Sparkles, FileText, Image, Check, RefreshCw, Star } from 'lucide-vue-next';

interface Props {
  node: Node<MasterImageNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();

const form = ref({
  style: '',
  timeOfDay: '',
  mood: '',
  prompt: '',
});

const styleOptions = ['실사', '애니메이션', '픽사', '수채화', '유화'];
const timeOptions = ['아침', '낮', '저녁', '밤'];
const moodOptions = ['편안', '고독', '긴장', '행복', '우울'];

const data = computed(() => props.node.data as MasterImageNodeData | undefined);
const isPromptGenerated = computed(() => data.value?.promptStatus !== PromptStatus.DRAFT);
const isPromptApproved = computed(() => data.value?.promptStatus === PromptStatus.APPROVED);
const canGenerate = computed(() => isPromptApproved.value);

watch(() => props.node.id, () => {
  if (!data.value) return;
  form.value = {
    style: data.value.style || '',
    timeOfDay: data.value.timeOfDay || '',
    mood: data.value.mood || '',
    prompt: data.value.prompt || '',
  };
}, { immediate: true });

function generatePrompt(): void {
  const promptText = `Wide establishing shot of a scene, style: ${form.value.style}, time: ${form.value.timeOfDay}, mood: ${form.value.mood}`;
  form.value.prompt = promptText;
  nodeStore.updateNode(props.node.id, {
    ...form.value,
    promptStatus: PromptStatus.GENERATED,
  });
}

function approvePrompt(): void {
  nodeStore.updateNode(props.node.id, {
    prompt: form.value.prompt,
    promptStatus: PromptStatus.APPROVED,
  });
}

function generateImage(): void {
  console.log('Generate image with:', form.value);
}

function setActive(): void {
  nodeStore.setActiveMaster(props.node.id);
}
</script>

<template>
  <BasePanel title="마스터 이미지 생성" :icon="Film">
    <template v-if="data">
      <!-- Active Status -->
      <div v-if="!data.isActive" class="panel-alert">
        <button class="panel-btn panel-btn--secondary" @click="setActive">
          <Star class="panel-btn-icon" />
          Active로 설정
        </button>
      </div>

      <!-- Style Selection -->
      <div class="panel-section">
        <label class="panel-label">
          <Palette class="panel-label-icon" />
          스타일
        </label>
        <div class="panel-radio-group">
          <label v-for="opt in styleOptions" :key="opt" class="panel-radio">
            <input type="radio" v-model="form.style" :value="opt" />
            <span class="panel-radio-label">{{ opt }}</span>
          </label>
        </div>
      </div>

      <!-- Time of Day -->
      <div class="panel-section">
        <label class="panel-label">
          <Sun class="panel-label-icon" />
          시간대
        </label>
        <div class="panel-radio-group">
          <label v-for="opt in timeOptions" :key="opt" class="panel-radio">
            <input type="radio" v-model="form.timeOfDay" :value="opt" />
            <span class="panel-radio-label">{{ opt }}</span>
          </label>
        </div>
      </div>

      <!-- Mood -->
      <div class="panel-section">
        <label class="panel-label">
          <Smile class="panel-label-icon" />
          분위기
        </label>
        <div class="panel-radio-group">
          <label v-for="opt in moodOptions" :key="opt" class="panel-radio">
            <input type="radio" v-model="form.mood" :value="opt" />
            <span class="panel-radio-label">{{ opt }}</span>
          </label>
        </div>
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
        <textarea v-model="form.prompt" class="panel-textarea panel-textarea--prompt" rows="4"></textarea>
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
        @click="generateImage"
      >
        <Image class="panel-btn-icon" />
        이미지 생성
      </button>
    </template>
  </BasePanel>
</template>
