<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Sparkles, ArrowRight, ChevronDown } from 'lucide-vue-next'
import ModalBase from '../common/ModalBase.vue'
import { useProjectStore } from '../../stores/project'
import { useUIStore } from '../../stores/ui'

const router = useRouter()
const projectStore = useProjectStore()
const uiStore = useUIStore()

const form = reactive({
  title: '',
  description: '',
  genre: '',
  mood: '',
  sceneCount: 4,
  plot: '',
})

const isAISectionOpen = ref(false)
const isGenerating = ref(false)
const isCreating = ref(false)
const aiGenerated = ref(false)

const genres = [
  { value: '', label: '선택하세요' },
  { value: 'fantasy', label: '판타지' },
  { value: 'sf', label: 'SF' },
  { value: 'romance', label: '로맨스' },
  { value: 'action', label: '액션' },
  { value: 'thriller', label: '스릴러' },
  { value: 'comedy', label: '코미디' },
  { value: 'documentary', label: '다큐멘터리' },
]

const moods = [
  { value: '', label: '선택하세요' },
  { value: 'epic', label: '서사적' },
  { value: 'bright', label: '밝은' },
  { value: 'dark', label: '어두운' },
  { value: 'hopeful', label: '희망적' },
  { value: 'tense', label: '긴장감' },
  { value: 'comic', label: '코믹' },
]

const sceneOptions = [3, 4, 5, 6, 7]

const toggleAISection = () => {
  isAISectionOpen.value = !isAISectionOpen.value
}

const generateScenario = async () => {
  isGenerating.value = true
  // Simulate AI generation
  await new Promise((resolve) => setTimeout(resolve, 2000))
  isGenerating.value = false
  aiGenerated.value = true
  uiStore.showToast({
    type: 'success',
    title: 'AI 시나리오 생성 완료',
    message: `${form.sceneCount}개의 씬이 자동 생성되었습니다.`,
  })
}

const createProject = async () => {
  if (!form.title.trim()) {
    uiStore.showToast({
      type: 'error',
      title: '프로젝트 제목을 입력해주세요',
    })
    return
  }

  isCreating.value = true

  const newProject = await projectStore.addProject({
    title: form.title,
    description: form.description,
    genre: form.genre,
  })

  isCreating.value = false

  if (newProject) {
    uiStore.showToast({
      type: 'success',
      title: '프로젝트 생성 완료',
      message: `${form.title} 프로젝트가 생성되었습니다.`,
    })
    uiStore.closeModal()

    // Reset form
    form.title = ''
    form.description = ''
    form.genre = ''
    form.mood = ''
    form.sceneCount = 4
    form.plot = ''
    aiGenerated.value = false

    // Navigate to project detail
    router.push(`/project/${newProject.projectId}`)
  }
}

const handleClose = () => {
  // Reset form on close
  form.title = ''
  form.description = ''
  form.genre = ''
  form.mood = ''
  form.sceneCount = 4
  form.plot = ''
  aiGenerated.value = false
  isAISectionOpen.value = false
}
</script>

<template>
  <ModalBase
    modal-id="new-project"
    title="새 프로젝트 만들기"
    size="lg"
    @close="handleClose"
  >
    <form @submit.prevent="createProject">
      <!-- Title -->
      <div class="form-group">
        <label class="form-label required">프로젝트 제목</label>
        <input
          v-model="form.title"
          type="text"
          class="form-input"
          placeholder="예: 용사의 모험"
          required
        />
      </div>

      <!-- Description -->
      <div class="form-group">
        <label class="form-label">설명</label>
        <textarea
          v-model="form.description"
          class="form-input form-textarea"
          placeholder="프로젝트에 대한 간단한 설명을 입력하세요"
        ></textarea>
      </div>

      <!-- Genre & Mood -->
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">장르</label>
          <select v-model="form.genre" class="form-input form-select">
            <option v-for="g in genres" :key="g.value" :value="g.value">
              {{ g.label }}
            </option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">분위기</label>
          <select v-model="form.mood" class="form-input form-select">
            <option v-for="m in moods" :key="m.value" :value="m.value">
              {{ m.label }}
            </option>
          </select>
        </div>
      </div>

      <!-- AI Scenario Generation Section -->
      <div class="form-section">
        <button
          type="button"
          class="form-section-header"
          @click="toggleAISection"
        >
          <span class="form-section-title">
            <Sparkles class="icon-sm" style="color: var(--rose-400);" />
            AI 시나리오 생성 (선택)
          </span>
          <ChevronDown
            class="icon-sm toggle-icon"
            :class="{ rotated: isAISectionOpen }"
          />
        </button>

        <div v-if="isAISectionOpen" class="form-section-content">
          <!-- Scene Count -->
          <div class="form-group">
            <label class="form-label">씬 개수</label>
            <div class="chip-group">
              <button
                v-for="count in sceneOptions"
                :key="count"
                type="button"
                :class="['chip', { selected: form.sceneCount === count }]"
                @click="form.sceneCount = count"
              >
                {{ count }}개
              </button>
            </div>
            <div class="form-helper">총 길이 60초 기준, 씬당 약 10~15초 권장</div>
          </div>

          <!-- Plot -->
          <div class="form-group">
            <label class="form-label">줄거리</label>
            <textarea
              v-model="form.plot"
              class="form-input form-textarea"
              placeholder="예: 젊은 용사가 마을을 구하기 위해 모험을 떠나는 이야기. 숲속에서 현자를 만나고, 최종 보스와 대결한다."
            ></textarea>
          </div>

          <button
            type="button"
            class="btn btn-secondary w-full"
            :disabled="isGenerating"
            @click="generateScenario"
          >
            <span v-if="isGenerating" class="btn-spinner"></span>
            <template v-else-if="aiGenerated">
              <span class="checkmark">✓</span>
              {{ form.sceneCount }}개 씬 생성 완료!
            </template>
            <template v-else>
              <Sparkles class="icon-sm" />
              AI로 씬 생성하기
            </template>
          </button>
        </div>
      </div>
    </form>

    <template #footer>
      <button
        type="button"
        class="btn btn-secondary"
        @click="uiStore.closeModal"
      >
        취소
      </button>
      <button
        type="button"
        class="btn btn-primary"
        :disabled="isCreating || !form.title.trim()"
        @click="createProject"
      >
        <span v-if="isCreating" class="btn-spinner"></span>
        <template v-else>
          프로젝트 생성
          <ArrowRight class="icon-sm" />
        </template>
      </button>
    </template>
  </ModalBase>
</template>

<style scoped>
.form-group {
  margin-bottom: 1.25rem;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--gray-700);
  margin-bottom: 0.5rem;
}

.form-label.required::after {
  content: ' *';
  color: var(--rose-500);
}

.form-input {
  width: 100%;
  padding: 0.75rem 1rem;
  background: var(--rose-50);
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: 0.875rem;
  color: var(--gray-700);
  transition: all 0.2s ease;
}

.form-input::placeholder {
  color: var(--gray-400);
}

.form-input:focus {
  outline: none;
  background: white;
  border-color: var(--rose-300);
  box-shadow: 0 0 0 3px rgba(255, 133, 161, 0.1);
}

.form-textarea {
  min-height: 80px;
  resize: vertical;
}

.form-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%236B7280' d='M2.5 4.5L6 8l3.5-3.5'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 1rem center;
  padding-right: 2.5rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-helper {
  font-size: 0.75rem;
  color: var(--gray-500);
  margin-top: 0.5rem;
}

/* Form Section */
.form-section {
  background: var(--rose-50);
  border-radius: 12px;
  margin-top: 1.5rem;
  overflow: hidden;
}

.form-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 1rem;
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
}

.form-section-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--gray-700);
}

.toggle-icon {
  color: var(--gray-400);
  transition: transform 0.2s ease;
}

.toggle-icon.rotated {
  transform: rotate(180deg);
}

.form-section-content {
  padding: 0 1rem 1rem;
}

/* Chips */
.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.chip {
  padding: 0.5rem 1rem;
  background: white;
  border: 1px solid var(--rose-200);
  border-radius: 9999px;
  font-size: 0.875rem;
  color: var(--gray-600);
  cursor: pointer;
  transition: all 0.2s ease;
}

.chip:hover {
  border-color: var(--rose-300);
}

.chip.selected {
  background: var(--rose-100);
  border-color: var(--rose-400);
  color: var(--rose-600);
}

/* Utility */
.w-full {
  width: 100%;
}

.icon-sm {
  width: 16px;
  height: 16px;
}

/* Button spinner - 컴포넌트 전용 */
.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.checkmark {
  color: var(--success);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
