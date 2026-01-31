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
const autoFilledNodes = new Set<string>();
let promptPreviewTimeout: ReturnType<typeof setTimeout> | null = null;
const koDirty = ref(false);
const lastSyncedKo = ref('');
const promptSectionRef = ref<HTMLElement | null>(null);
const promptKoRef = ref<HTMLTextAreaElement | null>(null);
const isFinalEditing = ref(false);
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
  promptLang: 'EN' as 'EN' | 'KO',
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
  getReferenceObjectIds: () => (form.value.objectIds.length ? [...form.value.objectIds] : undefined),
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
  isPromptApproved.value &&
  !isKoOutOfSync.value &&
  !isGeneratingImage.value &&
  !isGeneratingPrompt.value
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
    promptLang: 'EN',
  };
  koDirty.value = false;
  lastSyncedKo.value = form.value.promptKo;
  isFinalEditing.value = false;
  maybeAutofillPrompt();
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
      lastSyncedKo.value = normalized;
      koDirty.value = false;
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
  () => sceneHeaderData.value?.description,
  () => {
    maybeAutofillPrompt();
  }
);

watch(
  () => form.value.promptLang,
  (next, prev) => {
    if (prev === 'KO' && next === 'EN' && koDirty.value) {
      rewritePrompt();
    }
  }
);

watch(
  () => [
    form.value.prompt,
    form.value.promptKo,
    form.value.promptEnFinalOverride,
    form.value.usePromptOverride,
  ],
  () => {
    if (!data.value) return;
    const updates: Partial<MasterImageNodeData> = {};
    if (form.value.prompt !== (data.value.prompt ?? '')) {
      updates.prompt = form.value.prompt;
    }
    if (form.value.promptKo !== (data.value.promptKo ?? '')) {
      updates.promptKo = form.value.promptKo;
    }
    const nextOverride = form.value.usePromptOverride ? form.value.promptEnFinalOverride : '';
    if (nextOverride !== (data.value.promptEnFinalOverride ?? '')) {
      updates.promptEnFinalOverride = nextOverride;
    }
    if (Object.keys(updates).length > 0) {
      nodeStore.updateNodeLocal(props.node.id, updates);
    }
  }
);

watch(
  () => ({
    prompt: form.value.prompt,
    style: form.value.style,
    timeOfDay: form.value.timeOfDay,
    mood: form.value.mood,
    objectIds: form.value.objectIds.slice(),
    usePromptOverride: form.value.usePromptOverride,
  }),
  () => {
    queuePromptPreview();
  },
  { deep: true }
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
  if (promptPreviewTimeout) {
    clearTimeout(promptPreviewTimeout);
    promptPreviewTimeout = null;
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

const isRewriting = ref(false);
const isKoOutOfSync = computed(() => koDirty.value);

async function rewritePrompt(): Promise<void> {
  if (!form.value.promptKo) return;
  if (isRewriting.value) return;
  const sourceKo = form.value.promptKo;
  isRewriting.value = true;
  try {
    const result = await aiService.rewritePrompt(form.value.promptKo);
    if (result.promptEnBase && result.promptEnBase.trim()) {
      form.value.prompt = result.promptEnBase;
      koDirty.value = false;
      lastSyncedKo.value = sourceKo;
      queuePromptPreview();
    }
  } catch (error) {
    console.error('Failed to rewrite prompt:', error);
  } finally {
    isRewriting.value = false;
  }
}

function toggleFinalEditing(): void {
  if (!form.value.usePromptOverride) {
    form.value.usePromptOverride = true;
    form.value.promptEnFinalOverride = form.value.promptEnFinal || form.value.prompt;
    isFinalEditing.value = true;
    return;
  }

  if (isFinalEditing.value && !form.value.promptEnFinalOverride.trim()) {
    form.value.promptEnFinalOverride = form.value.promptEnFinal || form.value.prompt;
  }

  isFinalEditing.value = !isFinalEditing.value;
}

function maybeAutofillPrompt(): void {
  const nodeId = props.node.id;
  if (autoFilledNodes.has(nodeId)) return;
  if (form.value.prompt.trim()) return;
  const header = sceneHeaderData.value as { description?: string } | undefined;
  const description = header?.description?.trim();
  if (!description) return;
  const hasHangul = /[ㄱ-ㅎㅏ-ㅣ가-힣]/.test(description);
  if (hasHangul) {
    if (!form.value.promptKo.trim()) {
      form.value.promptKo = description;
    }
    form.value.promptLang = 'KO';
  }
  form.value.prompt = description;
  autoFilledNodes.add(nodeId);
}

function markKoDirty(): void {
  if (form.value.promptKo === lastSyncedKo.value) {
    koDirty.value = false;
    return;
  }
  koDirty.value = true;
}

async function focusKoEditor(): Promise<void> {
  form.value.promptLang = 'KO';
  await nextTick();
  if (promptSectionRef.value) {
    const container = promptSectionRef.value.closest('.base-panel__content') as HTMLElement | null;
    if (container) {
      const containerRect = container.getBoundingClientRect();
      const sectionRect = promptSectionRef.value.getBoundingClientRect();
      const currentScroll = container.scrollTop;
      const offset = sectionRect.top - containerRect.top;
      const centeredOffset = (container.clientHeight - sectionRect.height) / 2;
      const rawTarget = currentScroll + offset - centeredOffset;
      const maxScroll = Math.max(0, container.scrollHeight - container.clientHeight);
      const targetScroll = Math.min(Math.max(0, rawTarget), maxScroll);
      gsap.to(container, { scrollTop: targetScroll, duration: 0.45, ease: 'power2.out' });
    } else {
      promptSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
    gsap.killTweensOf(promptSectionRef.value);
    gsap.fromTo(
      promptSectionRef.value,
      { boxShadow: '0 0 0 0 rgba(255, 107, 138, 0)', backgroundColor: 'rgba(255, 250, 252, 0)' },
      {
        boxShadow: '0 0 0 12px rgba(255, 107, 138, 0.35)',
        backgroundColor: 'rgba(255, 250, 252, 0.9)',
        duration: 0.35,
        yoyo: true,
        repeat: 1,
        ease: 'power2.out',
        clearProps: 'boxShadow,backgroundColor',
      }
    );
  }
  if (promptKoRef.value) {
    promptKoRef.value.focus();
  }
}

function queuePromptPreview(): void {
  if (!form.value.prompt.trim()) return;
  if (form.value.usePromptOverride) return;
  if (promptPreviewTimeout) clearTimeout(promptPreviewTimeout);
  promptPreviewTimeout = setTimeout(() => {
    promptPreviewTimeout = null;
    refreshPromptPreview();
  }, 600);
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

      <!-- Narrative Prompt -->
      <div class="panel-section" ref="promptSectionRef">
        <div class="panel-label-row">
          <label class="panel-label">
            <FileText class="panel-label-icon" />
            서술 프롬프트
          </label>
          <div class="panel-segmented" role="tablist" aria-label="Prompt language">
            <button
              type="button"
              class="panel-segmented__btn"
              :class="{ 'is-active': form.promptLang === 'EN' }"
              @click="form.promptLang = 'EN'"
            >
              EN
            </button>
            <button
              type="button"
              class="panel-segmented__btn"
              :class="{ 'is-active': form.promptLang === 'KO' }"
              @click="form.promptLang = 'KO'"
            >
              KO
            </button>
          </div>
        </div>
        <p class="panel-subtext">
          {{ form.promptLang === 'EN' ? '원본(편집 가능)' : '번역(수정 가능)' }}
        </p>
        <div v-if="form.promptLang === 'EN'">
          <textarea
            v-model="form.prompt"
            class="panel-textarea panel-textarea--prompt"
            rows="4"
            placeholder="예: A lone traveler stands at the edge of a foggy cliff, wind lifting their coat."
          ></textarea>
        </div>
        <div v-else class="panel-translation-block">
          <textarea
            ref="promptKoRef"
            v-model="form.promptKo"
            class="panel-textarea panel-textarea--prompt"
            rows="4"
            @input="markKoDirty"
          ></textarea>
          <div class="panel-prompt-actions">
            <button
              class="panel-btn panel-btn--success panel-btn--sync"
              :class="{ 'panel-btn--sync--muted': !isKoOutOfSync }"
              :disabled="isRewriting || !form.promptKo || !isKoOutOfSync"
              @click="rewritePrompt"
            >
              <Loader2 v-if="isRewriting" class="panel-btn-icon panel-btn-icon--spin" />
              <RefreshCw v-else class="panel-btn-icon" />
              영어로 반영
            </button>
          </div>
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
        {{ isGeneratingPrompt ? '생성 중...' : 'AI로 다듬기' }}
      </button>

      <!-- Generated Prompt -->
      <div v-if="isPromptGenerated" class="panel-section panel-section--prompt">
        <label class="panel-label">
          <FileText class="panel-label-icon" />
          최종 프롬프트 (영어)
        </label>
        <textarea
          v-if="form.usePromptOverride"
          v-model="form.promptEnFinalOverride"
          class="panel-textarea panel-textarea--prompt"
          rows="4"
          :readonly="!isFinalEditing"
          placeholder="최종 영어 프롬프트를 직접 입력하세요."
        ></textarea>
        <textarea
          v-else
          :value="form.promptEnFinal"
          class="panel-textarea panel-textarea--prompt"
          rows="4"
          readonly
          placeholder="자동으로 갱신됩니다."
        ></textarea>

        <div class="panel-prompt-actions">
          <button
            class="panel-btn panel-btn--text"
            :disabled="!form.promptEnFinal && !form.prompt"
            @click="toggleFinalEditing"
          >
            {{ isFinalEditing ? '편집 완료' : '영문 직접 편집' }}
          </button>
          <button class="panel-btn panel-btn--text" @click="focusKoEditor">
            한국어로 편집
          </button>
        </div>

        <div class="panel-prompt-actions panel-prompt-actions--right">
          <span v-if="isKoOutOfSync" class="panel-subtext">
            영어 반영이 필요합니다.
          </span>
          <button class="panel-btn panel-btn--text" :disabled="isGeneratingPrompt || isGeneratingImage" @click="generatePrompt">
            <Loader2 v-if="isGeneratingPrompt" class="panel-btn-icon panel-btn-icon--spin" />
            <RefreshCw v-else class="panel-btn-icon" />
            재생성
          </button>
          <button
            v-if="!isPromptApproved"
            class="panel-btn panel-btn--success"
            :disabled="isGeneratingPrompt || isGeneratingImage || isKoOutOfSync"
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

.panel-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
}

.panel-segmented {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.15rem;
  background: var(--gray-100, #F3F4F6);
  border-radius: 999px;
}

.panel-segmented__btn {
  border: 0;
  background: transparent;
  padding: 0.2rem 0.65rem;
  font-size: 0.7rem;
  color: var(--gray-600, #4B5563);
  border-radius: 999px;
  cursor: pointer;
}

.panel-segmented__btn.is-active {
  background: var(--gray-900, #111827);
  color: var(--gray-50, #F9FAFB);
  box-shadow: 0 2px 6px rgba(17, 24, 39, 0.18);
}

.panel-subtext {
  margin: 0 0 0.5rem;
  font-size: 0.75rem;
  color: var(--gray-500, #6B7280);
}

.panel-translation-block .panel-prompt-actions {
  margin-top: 0.4rem;
}


.panel-btn--sync--muted {
  background: var(--gray-100, #F3F4F6);
  color: var(--gray-500, #6B7280);
  border-color: var(--gray-200, #E5E7EB);
}

.panel-btn--sync--muted:hover {
  background: var(--gray-100, #F3F4F6);
  color: var(--gray-500, #6B7280);
  border-color: var(--gray-200, #E5E7EB);
}
</style>
