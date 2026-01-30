<script setup lang="ts">
/**
 * MasterImagePanel - 마스터 이미지 생성/편집 패널
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 6.2
 */
import { ref, computed, watch, nextTick, onUnmounted } from 'vue';
import type { Node } from '@vue-flow/core';
import type { MasterImageNodeData } from '../../../types/ui/sceneNodes';
import { NodeType, PromptStatus } from '../../../types/ui/sceneNodes';
import BasePanel from './BasePanel.vue';
import { useSceneNodeStore } from '../../../stores/sceneNode';
import { useObjectStore } from '../../../stores/object';
import { useNodeGeneration } from '../../../composables/useNodeGeneration';
import { Film, Palette, Sun, Smile, Sparkles, FileText, Image, Check, RefreshCw, Star, Users, Loader2 } from 'lucide-vue-next';
import { gsap } from 'gsap';
import { resolveMoodKey, resolveStyleKey, resolveTimeOfDayKey } from '../../../utils/nodeSettings';
import {
  DEFAULT_MASTER_MOOD,
  DEFAULT_MASTER_STYLE,
  DEFAULT_MASTER_TIME_OF_DAY,
} from '../../../utils/nodeDefaults';
import { aiService } from '../../../services';

interface Props {
  node: Node<MasterImageNodeData>;
}

const props = defineProps<Props>();
const nodeStore = useSceneNodeStore();
const objectStore = useObjectStore();
// 폼 상태 - objectIds는 배열로 관리 (다중 선택)
const form = ref({
  style: DEFAULT_MASTER_STYLE,
  timeOfDay: DEFAULT_MASTER_TIME_OF_DAY,
  mood: DEFAULT_MASTER_MOOD,
  objectIds: [] as number[],  // 등장 오브젝트 IDs (캐릭터 포함)
  prompt: '',
  promptKo: '',
  promptEnFinal: '',
  promptEnFinalOverride: '',
  usePromptOverride: false,
  showAdvanced: false,
});

const {
  isGeneratingPrompt,
  isGeneratingJob: isGeneratingImage,
  errorMessage,
  clearError,
  generatePrompt,
  approvePrompt,
  refreshPromptPreview,
  runGeneration: generateImage,
} = useNodeGeneration({
  nodeId: props.node.id,
  nodeType: 'MASTER',
  toastType: 'image',
  getPrompt: () => form.value.prompt,
  getPromptPayload: () => ({
    nodeType: 'MASTER',
    sceneOneLine: buildSceneOneLine(),
    style: form.value.style,
    timeOfDay: form.value.timeOfDay,
    mood: form.value.mood,
    objects: selectedObjectNames.value,
  }),
  getPromptUpdate: (result) => ({
    style: form.value.style,
    timeOfDay: form.value.timeOfDay,
    mood: form.value.mood,
    objectIds: form.value.objectIds,
    prompt: result.promptEnBase,
    promptKo: result.promptKo,
  }),
  getApprovedUpdate: () => ({
    prompt: form.value.prompt,
    promptKo: form.value.promptKo,
    promptEnFinalOverride: form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  }),
  getJobSettings: () => ({
    styleKey: resolveStyleKey(form.value.style),
    timeOfDayKey: resolveTimeOfDayKey(form.value.timeOfDay),
    moodKey: resolveMoodKey(form.value.mood) ?? 'NEUTRAL',
    objectIds: form.value.objectIds,
  }),
  getPromptOverride: () =>
    form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  getPromptPreviewPayload: () => ({
    prompt: form.value.prompt,
    settings: {
      styleKey: resolveStyleKey(form.value.style),
      timeOfDayKey: resolveTimeOfDayKey(form.value.timeOfDay),
      moodKey: resolveMoodKey(form.value.mood) ?? 'NEUTRAL',
      objectIds: form.value.objectIds,
    },
    promptEnFinalOverride: form.value.usePromptOverride ? form.value.promptEnFinalOverride : '',
  }),
  onPromptPreview: (result) => ({
    promptEnFinal: result.promptEnFinal,
  }),
  getJobSuccessUpdate: ({ resultUrl, thumbnailUrl }) => ({
    imageUrl: resultUrl || null,
    thumbnailUrl: thumbnailUrl || resultUrl || null,
  }),
  messages: {
    jobError: '이미지 생성에 실패했습니다. 다시 시도해주세요.',
  },
});

const styleOptions = ['시네마틱', '애니메이션', '픽사', '수채화', '유화'];
const timeOptions = ['아침', '낮', '저녁', '밤'];
const moodOptions = ['중립', '편안', '고독', '긴장', '행복', '우울'];

const objectOptions = computed(() =>
  objectStore.objects.map((item) => ({
    id: item.objectId,
    name: item.name,
    type: item.type,
  }))
);

const objectNameMap = computed(() => {
  const map = new Map<number, string>();
  objectOptions.value.forEach((item) => map.set(item.id, item.name));
  return map;
});

const selectedObjectNames = computed(() =>
  form.value.objectIds
    .map((id) => objectNameMap.value.get(id))
    .filter((name): name is string => Boolean(name))
);

const data = computed(() => props.node.data as MasterImageNodeData | undefined);
const sceneHeaderData = computed(() =>
  nodeStore.nodes.find((node) => node.data?.type === NodeType.SCENE_HEADER)?.data
);
const hasPromptContent = computed(() => {
  if (!data.value) return false;
  return Boolean(
    (data.value.prompt ?? '').trim() ||
    (data.value.promptKo ?? '').trim() ||
    (data.value.promptEnFinal ?? '').trim() ||
    (data.value.promptEnFinalOverride ?? '').trim()
  );
});
const isPromptGenerated = computed(
  () => hasPromptContent.value || data.value?.promptStatus !== PromptStatus.DRAFT
);
const isPromptApproved = computed(() => data.value?.promptStatus === PromptStatus.APPROVED);
const canGenerate = computed(() =>
  isPromptApproved.value && !isGeneratingImage.value && !isGeneratingPrompt.value
);

function buildSceneOneLine(): string {
  const parts: string[] = [];
  const header = sceneHeaderData.value as { title?: string; description?: string } | undefined;
  if (header?.title) parts.push(`scene: ${header.title}`);
  if (header?.description) parts.push(`description: ${header.description}`);
  if (form.value.style) parts.push(`style: ${form.value.style}`);
  if (form.value.timeOfDay) parts.push(`time: ${form.value.timeOfDay}`);
  if (form.value.mood) parts.push(`mood: ${form.value.mood}`);
  if (selectedObjectNames.value.length) {
    parts.push(`objects: ${selectedObjectNames.value.join(', ')}`);
  }
  return parts.join(', ');
}
const generateButtonRef = ref<HTMLButtonElement | null>(null);
let generateButtonTween: gsap.core.Tween | null = null;

function normalizeIds(ids: number[] | undefined | null): number[] {
  return Array.isArray(ids) ? ids : [];
}

function areNumberArraysEqual(a: number[], b: number[]): boolean {
  if (a === b) return true;
  if (a.length !== b.length) return false;
  for (let index = 0; index < a.length; index += 1) {
    if (a[index] !== b[index]) return false;
  }
  return true;
}

let persistLookTimeout: ReturnType<typeof setTimeout> | null = null;
function queuePersistLook(): void {
  if (!data.value) return;
  if (persistLookTimeout) clearTimeout(persistLookTimeout);
  persistLookTimeout = setTimeout(() => {
    persistLookTimeout = null;
    nodeStore.updateNode(props.node.id, {
      style: form.value.style,
      timeOfDay: form.value.timeOfDay,
      mood: form.value.mood,
      objectIds: [...form.value.objectIds],
    });
  }, 300);
}

// 노드 변경 시 폼 동기화
watch(() => props.node.id, () => {
  if (!data.value) return;
  form.value = {
    style: data.value.style || DEFAULT_MASTER_STYLE,
    timeOfDay: data.value.timeOfDay || DEFAULT_MASTER_TIME_OF_DAY,
    mood: data.value.mood || DEFAULT_MASTER_MOOD,
    objectIds: data.value.objectIds || [],
    prompt: data.value.prompt || '',
    promptKo: data.value.promptKo || '',
    promptEnFinal: data.value.promptEnFinal || '',
    promptEnFinalOverride: data.value.promptEnFinalOverride || '',
    usePromptOverride: Boolean(data.value.promptEnFinalOverride),
    showAdvanced: false,
  };
  clearError();
}, { immediate: true });

watch(
  () => ({
    style: form.value.style,
    timeOfDay: form.value.timeOfDay,
    mood: form.value.mood,
    objectIds: form.value.objectIds.slice(),
  }),
  (next) => {
    if (!data.value) return;
    const savedObjectIds = normalizeIds(data.value.objectIds);
    const nextObjectIds = normalizeIds(next.objectIds);

    const unchanged =
      next.style === (data.value.style || DEFAULT_MASTER_STYLE) &&
      next.timeOfDay === (data.value.timeOfDay || DEFAULT_MASTER_TIME_OF_DAY) &&
      next.mood === (data.value.mood || DEFAULT_MASTER_MOOD) &&
      areNumberArraysEqual(nextObjectIds, savedObjectIds);

    if (unchanged) return;

    nodeStore.updateNodeLocal(props.node.id, {
      style: next.style,
      timeOfDay: next.timeOfDay,
      mood: next.mood,
      objectIds: [...nextObjectIds],
    });
    queuePersistLook();
  },
  { deep: true }
);

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
  () => data.value?.promptKo,
  (nextPromptKo) => {
    const normalized = nextPromptKo ?? '';
    if (normalized !== form.value.promptKo) {
      form.value.promptKo = normalized;
    }
  }
);

watch(
  () => data.value?.promptEnFinal,
  (nextPromptEnFinal) => {
    const normalized = nextPromptEnFinal ?? '';
    if (normalized !== form.value.promptEnFinal) {
      form.value.promptEnFinal = normalized;
    }
  }
);

watch(
  () => data.value?.promptEnFinalOverride,
  (nextPromptOverride) => {
    const normalized = nextPromptOverride ?? '';
    if (normalized !== form.value.promptEnFinalOverride) {
      form.value.promptEnFinalOverride = normalized;
    }
    form.value.usePromptOverride = Boolean(normalized);
  }
);

watch(
  () => [
    data.value?.style,
    data.value?.timeOfDay,
    data.value?.mood,
    data.value?.objectIds,
  ],
  () => {
    if (!data.value) return;
    form.value.style = data.value.style || DEFAULT_MASTER_STYLE;
    form.value.timeOfDay = data.value.timeOfDay || DEFAULT_MASTER_TIME_OF_DAY;
    form.value.mood = data.value.mood || DEFAULT_MASTER_MOOD;
    form.value.objectIds = [...(data.value.objectIds || [])];
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
  if (persistLookTimeout) {
    clearTimeout(persistLookTimeout);
    persistLookTimeout = null;
  }
});
// 오브젝트 선택 토글
function toggleObject(objectId: number): void {
  const idx = form.value.objectIds.indexOf(objectId);
  if (idx >= 0) {
    form.value.objectIds.splice(idx, 1);
  } else {
    form.value.objectIds.push(objectId);
  }
}

const isTranslating = ref(false);
const isRewriting = ref(false);

async function translatePrompt(): Promise<void> {
  if (!form.value.prompt) return;
  if (isTranslating.value) return;
  isTranslating.value = true;
  try {
    const result = await aiService.translatePrompt(form.value.prompt);
    if (result.promptKo && result.promptKo.trim()) {
      form.value.promptKo = result.promptKo;
    }
  } catch (error) {
    console.error('Failed to translate prompt:', error);
  } finally {
    isTranslating.value = false;
  }
}

async function rewritePrompt(): Promise<void> {
  if (!form.value.promptKo) return;
  if (isRewriting.value) return;
  isRewriting.value = true;
  try {
    const result = await aiService.rewritePrompt(form.value.promptKo);
    if (result.promptEnBase && result.promptEnBase.trim()) {
      form.value.prompt = result.promptEnBase;
      await refreshPromptPreview();
    }
  } catch (error) {
    console.error('Failed to rewrite prompt:', error);
  } finally {
    isRewriting.value = false;
  }
}

function enableFinalOverride(): void {
  if (!form.value.usePromptOverride) {
    form.value.usePromptOverride = true;
  }
  if (!form.value.promptEnFinalOverride.trim()) {
    form.value.promptEnFinalOverride = form.value.promptEnFinal || form.value.prompt;
  }
}

function clearFinalOverride(): void {
  form.value.usePromptOverride = false;
  form.value.promptEnFinalOverride = '';
}



function setActive(): void {
  if (data.value?.isActive) return;
  nodeStore.setActiveMaster(props.node.id);
}
</script>

<template>
  <BasePanel title="마스터 이미지 생성" :icon="Film" class="master-panel">
    <template v-if="data" #header-actions>
      <button
        class="panel-btn master-panel__header-action"
        :class="data.isActive ? 'panel-btn--confirmed' : 'panel-btn--secondary'"
        @click="setActive"
        :title="data.isActive ? '현재 Active' : 'Active로 설정'"
      >
        <Star class="panel-btn-icon" />
        {{ data.isActive ? '마스터 활성' : '활성화' }}
      </button>
    </template>
    <template v-if="data">
      <div v-if="!data.isActive" class="master-panel__inactive-hint">
        <span class="master-panel__inactive-title">안내</span>
        <p class="master-panel__inactive-text">
          이 마스터가 활성화되어야 하위 생성이 정상 동작합니다.
        </p>
      </div>

      <!-- Style Selection -->
      <div class="panel-section">
        <label class="panel-label">
          <Palette class="panel-label-icon" />
          스타일
        </label>
        <div class="panel-radio-group panel-style-grid">
          <label
            v-for="opt in styleOptions"
            :key="opt"
            :class="['panel-radio', 'panel-style-card', { 'panel-style-card--primary': opt === '시네마틱' }]"
          >
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
        <div class="panel-radio-group panel-pill-group panel-pill-group--accent">
          <label v-for="opt in timeOptions" :key="opt" class="panel-radio panel-pill">
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
        <div class="panel-radio-group panel-pill-group panel-pill-group--accent">
          <label v-for="obj in objectOptions" :key="obj.id" class="panel-radio panel-pill">
            <input
              type="checkbox"
              :checked="form.objectIds.includes(obj.id)"
              @change="toggleObject(obj.id)"
            />
            <span class="panel-radio-label">{{ obj.name }}</span>
          </label>
        </div>
      </div>

      <!-- Mood -->
      <div class="panel-section">
        <label class="panel-label">
          <Smile class="panel-label-icon" />
          분위기
        </label>
        <div class="panel-radio-group panel-pill-group panel-pill-group--accent">
          <label v-for="opt in moodOptions" :key="opt" class="panel-radio panel-pill">
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
        class="panel-btn panel-btn--secondary panel-btn--full panel-btn--prompt-generate" 
        :disabled="isGeneratingPrompt || isGeneratingImage"
        @click="generatePrompt"
      >
        <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon animate-spin" />
        <Sparkles v-else class="panel-btn-icon" />
        {{ isGeneratingPrompt ? '생성 중...' : '프롬프트 생성' }}
      </button>

      <!-- Generated Prompt -->
      <div v-if="isPromptGenerated" class="panel-section panel-section--prompt">
        <label class="panel-label">
          <FileText class="panel-label-icon" />
          프롬프트
          <span class="panel-label-badge">생성됨</span>
        </label>
        <label class="panel-label" style="margin-top: 0.75rem;">
          <FileText class="panel-label-icon" />
          생성용 프롬프트 (영어)
        </label>
        <textarea
          :value="form.promptEnFinal"
          class="panel-textarea panel-textarea--prompt"
          rows="4"
          readonly
          placeholder="생성용 프롬프트 미리보기로 확인하세요."
        ></textarea>

        <div class="panel-prompt-actions">
          <button class="panel-btn panel-btn--text" :disabled="!form.prompt" @click="refreshPromptPreview">
            <RefreshCw class="panel-btn-icon" />
            생성용 프롬프트 미리보기
          </button>
          <button class="panel-btn panel-btn--text" :disabled="!form.promptEnFinal" @click="enableFinalOverride">
            영문 직접 편집
          </button>
          <button class="panel-btn panel-btn--text" @click="form.showAdvanced = !form.showAdvanced">
            <span class="panel-btn-icon">⋯</span>
            고급 설정
          </button>
        </div>

        <div v-if="form.usePromptOverride" class="panel-section" style="margin-top: 0.75rem;">
          <label class="panel-label">
            <FileText class="panel-label-icon" />
            생성용 프롬프트 직접 수정
          </label>
          <textarea
            v-model="form.promptEnFinalOverride"
            class="panel-textarea panel-textarea--prompt"
            rows="4"
            placeholder="최종 영어 프롬프트를 직접 입력하세요."
          ></textarea>
          <div class="panel-prompt-actions panel-prompt-actions--right">
            <button class="panel-btn panel-btn--text" @click="clearFinalOverride">
              오버라이드 해제
            </button>
          </div>
        </div>

        <div v-if="form.showAdvanced" class="panel-section" style="margin-top: 0.75rem;">
          <label class="panel-label">
            <FileText class="panel-label-icon" />
            서술 프롬프트 (한국어)
          </label>
          <textarea v-model="form.promptKo" class="panel-textarea panel-textarea--prompt" rows="3"></textarea>
          <div class="panel-prompt-actions">
            <button class="panel-btn panel-btn--text" :disabled="isTranslating || !form.prompt" @click="translatePrompt">
              <Loader2 v-if="isTranslating" class="panel-btn-icon panel-btn-icon--spin" />
              <RefreshCw v-else class="panel-btn-icon" />
              EN → KO
            </button>
            <button class="panel-btn panel-btn--text" :disabled="isRewriting || !form.promptKo" @click="rewritePrompt">
              <Loader2 v-if="isRewriting" class="panel-btn-icon panel-btn-icon--spin" />
              <RefreshCw v-else class="panel-btn-icon" />
              KO → EN
            </button>
          </div>

          <label class="panel-label" style="margin-top: 0.75rem;">
            <FileText class="panel-label-icon" />
            서술 프롬프트 (영어)
          </label>
          <textarea v-model="form.prompt" class="panel-textarea panel-textarea--prompt" rows="3"></textarea>
        </div>

        <div class="panel-prompt-actions panel-prompt-actions--right">
          <button class="panel-btn panel-btn--text" :disabled="isGeneratingPrompt || isGeneratingImage" @click="generatePrompt">
            <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon panel-btn-icon--spin" />
            <RefreshCw v-else class="panel-btn-icon" />
            재생성
          </button>
          <button
            v-if="!isPromptApproved"
            class="panel-btn panel-btn--success"
            :disabled="isGeneratingPrompt || isGeneratingImage"
            @click="approvePrompt"
          >
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
.master-panel__header-action {
  padding: 0.4rem 0.65rem;
  font-size: 0.75rem;
  gap: 0.35rem;
  border-radius: 0.65rem;
  line-height: 1;
  white-space: nowrap;
}

.master-panel__header-action .panel-btn-icon {
  width: 14px;
  height: 14px;
}

.master-panel :deep(.base-panel__actions) {
  width: 100%;
  flex: 1 1 100%;
  margin-left: 0;
  justify-content: flex-start;
}

.master-panel :deep(.base-panel__close) {
  margin-left: auto;
}

.master-panel__inactive-hint {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-bottom: 1rem;
  padding: 0.5rem 0.75rem;
  background: var(--rose-50, #FFFAFC);
  border: 1px solid var(--rose-200, #FFE8F2);
  border-radius: 0.75rem;
}

.master-panel__inactive-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--rose-600, #FF6B8A);
}

.master-panel__inactive-text {
  margin: 0;
  font-size: 0.75rem;
  color: var(--gray-700, #374151);
  line-height: 1.4;
  word-break: keep-all;
  white-space: normal;
}
</style>
