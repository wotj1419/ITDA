<script setup lang="ts">
/**
 * MasterImagePanel - 마스터 이미지 생성/편집 패널
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 6.2
 */
import { ref, computed, watch, nextTick, onUnmounted } from 'vue';
import type { Node } from '@vue-flow/core';
import type { MasterImageNodeData } from '../../../types/ui/sceneNodes';
import { PromptStatus } from '../../../types/ui/sceneNodes';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { Film, Palette, Sun, Smile, Sparkles, FileText, Image, Check, RefreshCw, Star, Users, Loader2 } from 'lucide-vue-next';
import { gsap } from 'gsap';

interface Props {
  node: Node<MasterImageNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();
// 폼 상태 - objectIds는 배열로 관리 (다중 선택)
const form = ref({
  style: '',
  timeOfDay: '',
  mood: '',
  objectIds: [] as string[],  // 등장 오브젝트 IDs (캐릭터 포함)
  prompt: '',
});

const {
  isGeneratingPrompt,
  isGeneratingJob: isGeneratingImage,
  errorMessage,
  clearError,
  generatePrompt,
  approvePrompt,
  runGeneration: generateImage,
} = useNodeGeneration({
  nodeId: props.node.id,
  nodeType: 'MASTER',
  toastType: 'image',
  getPrompt: () => form.value.prompt,
  getPromptPayload: () => ({
    nodeType: 'MASTER',
    style: form.value.style,
    timeOfDay: form.value.timeOfDay,
    mood: form.value.mood,
    objectIds: form.value.objectIds,
  }),
  getPromptUpdate: (prompt) => ({
    style: form.value.style,
    timeOfDay: form.value.timeOfDay,
    mood: form.value.mood,
    objectIds: form.value.objectIds,
    prompt,
  }),
  getApprovedUpdate: () => ({ prompt: form.value.prompt }),
  getJobSettings: () => ({
    style: form.value.style,
    timeOfDay: form.value.timeOfDay,
    mood: form.value.mood,
    objectIds: form.value.objectIds,
  }),
  getJobSuccessUpdate: ({ resultUrl, thumbnailUrl }) => ({
    imageUrl: resultUrl || null,
    thumbnailUrl: thumbnailUrl || resultUrl || null,
  }),
  messages: {
    jobError: '이미지 생성에 실패했습니다. 다시 시도해주세요.',
  },
});

const styleOptions = ['실사', '애니메이션', '픽사', '수채화', '유화'];
const timeOptions = ['아침', '낮', '저녁', '밤'];
const moodOptions = ['편안', '고독', '긴장', '행복', '우울'];

// TODO: 실제로는 Store/API에서 캐릭터/오브젝트 목록을 가져와야 함
const objectOptions = [
  { id: 'char-nahido', name: '나희도', type: 'character' },
  { id: 'char-baekijin', name: '백이진', type: 'character' },
  { id: 'char-goyurim', name: '고유림', type: 'character' },
  { id: 'obj-robot-bell', name: '로봇 벨', type: 'object' },
  { id: 'obj-spaceship', name: '우주선', type: 'object' },
];

const data = computed(() => props.node.data as MasterImageNodeData | undefined);
const isPromptGenerated = computed(() => data.value?.promptStatus !== PromptStatus.DRAFT);
const isPromptApproved = computed(() => data.value?.promptStatus === PromptStatus.APPROVED);
const canGenerate = computed(() => isPromptApproved.value && !isGeneratingImage.value);
const generateButtonRef = ref<HTMLButtonElement | null>(null);
let generateButtonTween: gsap.core.Tween | null = null;

// 노드 변경 시 폼 동기화
watch(() => props.node.id, () => {
  if (!data.value) return;
  form.value = {
    style: data.value.style || '',
    timeOfDay: data.value.timeOfDay || '',
    mood: data.value.mood || '',
    objectIds: data.value.objectIds || [],
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
  isGeneratingImage,
  async (running) => {
    if (running) {
      await nextTick();
      if (!generateButtonRef.value) return;
      generateButtonTween?.kill();
      generateButtonTween = gsap.to(generateButtonRef.value, {
        scale: 1.02,
        boxShadow: '0 10px 26px rgba(255, 107, 138, 0.35)',
        duration: 0.6,
        ease: 'power1.inOut',
        yoyo: true,
        repeat: -1,
      });
      return;
    }

    generateButtonTween?.kill();
    generateButtonTween = null;
    if (generateButtonRef.value) {
      gsap.set(generateButtonRef.value, { clearProps: 'transform,boxShadow' });
    }
  },
  { immediate: true }
);

onUnmounted(() => {
  generateButtonTween?.kill();
  generateButtonTween = null;
});

// 오브젝트 선택 토글
function toggleObject(objectId: string): void {
  const idx = form.value.objectIds.indexOf(objectId);
  if (idx >= 0) {
    form.value.objectIds.splice(idx, 1);
  } else {
    form.value.objectIds.push(objectId);
  }
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

      <!-- 등장 오브젝트 (다중 선택 체크박스) -->
      <div class="panel-section">
        <label class="panel-label">
          <Users class="panel-label-icon" />
          등장 오브젝트
        </label>
        <div class="panel-checkbox-group">
          <label v-for="obj in objectOptions" :key="obj.id" class="panel-checkbox">
            <input 
              type="checkbox" 
              :checked="form.objectIds.includes(obj.id)"
              @change="toggleObject(obj.id)"
            />
            <span class="panel-checkbox-label">
              {{ obj.name }}
              <span class="panel-checkbox-tag">{{ obj.type === 'character' ? '캐릭터' : '오브젝트' }}</span>
            </span>
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

      <!-- Error Message -->
      <div v-if="errorMessage" class="panel-error">
        {{ errorMessage }}
      </div>

      <!-- Generate Prompt -->
      <button 
        class="panel-btn panel-btn--secondary panel-btn--full" 
        :disabled="isGeneratingPrompt"
        @click="generatePrompt"
      >
        <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon animate-spin" />
        <Sparkles v-else class="panel-btn-icon" />
        {{ isGeneratingPrompt ? '생성 중...' : '프롬프트 생성' }}
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
        ref="generateButtonRef"
      >
        <Loader2 v-if="isGeneratingImage" class="panel-btn-icon animate-spin" />
        <Image v-else class="panel-btn-icon" />
        {{ isGeneratingImage ? '생성 중...' : '이미지 생성' }}
      </button>
    </template>
  </BasePanel>
</template>

<style scoped>
/* 체크박스 태그 스타일 */
.panel-checkbox-tag {
  font-size: 0.65rem;
  padding: 0.125rem 0.375rem;
  background: var(--rose-100, #FFF0F5);
  color: var(--rose-600, #DB2777);
  border-radius: 0.25rem;
  margin-left: 0.5rem;
}
</style>
