<script setup lang="ts">
/**
 * MasterImagePanel - 마스터 이미지 생성/편집 패널
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 6.2
 */
import { ref, computed, watch } from 'vue';
import type { Node } from '@vue-flow/core';
import type { MasterImageNodeData } from '../../../types/node';
import { PromptStatus, JobStatus } from '../../../types/node';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useGenerationToast } from '../../../composables/useGenerationToast';
import { aiService } from '../../../services';
import { Film, Palette, Sun, Smile, Sparkles, FileText, Image, Check, RefreshCw, Star, Users, Loader2 } from 'lucide-vue-next';

interface Props {
  node: Node<MasterImageNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();
const { startGenerationToast, finishGenerationToast } = useGenerationToast();

// 로딩 상태
const isGeneratingPrompt = ref(false);
const isGeneratingImage = ref(false);
const errorMessage = ref<string | null>(null);

// 폼 상태 - objectIds는 배열로 관리 (다중 선택)
const form = ref({
  style: '',
  timeOfDay: '',
  mood: '',
  objectIds: [] as string[],  // 등장 오브젝트 IDs (캐릭터 포함)
  prompt: '',
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
  errorMessage.value = null;
}, { immediate: true });

// 오브젝트 선택 토글
function toggleObject(objectId: string): void {
  const idx = form.value.objectIds.indexOf(objectId);
  if (idx >= 0) {
    form.value.objectIds.splice(idx, 1);
  } else {
    form.value.objectIds.push(objectId);
  }
}



async function generatePrompt(): Promise<void> {
  isGeneratingPrompt.value = true;
  errorMessage.value = null;

  try {
    // AI API 호출
    const prompt = await aiService.generatePrompt({
      nodeType: 'MASTER',
      style: form.value.style,
      timeOfDay: form.value.timeOfDay,
      mood: form.value.mood,
      objectIds: form.value.objectIds,
    });

    form.value.prompt = prompt;
    
    nodeStore.updateNode(props.node.id, {
      style: form.value.style,
      timeOfDay: form.value.timeOfDay,
      mood: form.value.mood,
      objectIds: form.value.objectIds,
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
  nodeStore.updateNode(props.node.id, {
    prompt: form.value.prompt,
    promptStatus: PromptStatus.APPROVED,
  });
}

async function generateImage(): Promise<void> {
  if (!form.value.prompt) return;

  isGeneratingImage.value = true;
  errorMessage.value = null;
  const toastId = startGenerationToast('image');

  try {
    // 노드 상태를 RUNNING으로 업데이트
    nodeStore.updateNode(props.node.id, { jobStatus: JobStatus.RUNNING });

    // 이미지 생성 요청
    const jobId = await aiService.generateNode(props.node.id, form.value.prompt, {
      nodeType: 'MASTER',
      settings: {
        style: form.value.style,
        timeOfDay: form.value.timeOfDay,
        mood: form.value.mood,
        objectIds: form.value.objectIds,
      },
    });
    console.log('Image generation job started:', jobId);

    // 폴링으로 완료 대기
    const result = await aiService.pollJobUntilComplete(jobId, (status) => {
      console.log('Job status:', status.status);
    });

    if (result.status === 'SUCCEEDED') {
      nodeStore.updateNode(props.node.id, {
        jobStatus: JobStatus.SUCCEEDED,
        imageUrl: result.resultUrl,
        thumbnailUrl: result.thumbnailUrl,
      });
      finishGenerationToast(toastId, 'image', 'success');
    } else {
      throw new Error(result.error?.message || 'Image generation failed');
    }
  } catch (error) {
    console.error('Failed to generate image:', error);
    errorMessage.value = '이미지 생성에 실패했습니다. 다시 시도해주세요.';
    nodeStore.updateNode(props.node.id, { jobStatus: JobStatus.FAILED });
    const reason = error instanceof Error ? error.message : '알 수 없는 오류';
    finishGenerationToast(toastId, 'image', 'error', { reason });
  } finally {
    isGeneratingImage.value = false;
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
        <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon panel-btn-icon--spin" />
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
      >
        <Loader2 v-if="isGeneratingImage" class="panel-btn-icon panel-btn-icon--spin" />
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
