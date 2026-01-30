<script setup lang="ts">
import { ref } from 'vue'
import { Sparkles } from 'lucide-vue-next'
import Button from '../common/Button.vue'
import Card from '../common/Card.vue'

interface Props {
  isGenerating?: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'generate', data: {
    genre: string
    mood: string
    sceneCount: number
    synopsis: string
  }): void
}>()

// Form state
const genre = ref('sf')
const mood = ref('tense')
const sceneCount = ref(5)
const synopsis = ref('')

// Options
const genreOptions = [
  { value: 'sf', label: 'SF' },
  { value: 'fantasy', label: '판타지' },
  { value: 'romance', label: '로맨스' },
  { value: 'action', label: '액션' },
  { value: 'thriller', label: '스릴러' },
  { value: 'documentary', label: '다큐멘터리' },
]

const moodOptions = [
  { value: 'tense', label: '긴장감' },
  { value: 'epic', label: '서사적' },
  { value: 'hopeful', label: '희망적' },
  { value: 'docu', label: '다큐' },
  { value: 'romantic', label: '로맨틱' },
  { value: 'dark', label: '어두운' },
]

const sceneCountOptions = [3, 4, 5, 6, 7, 8]

const handleGenerate = () => {
  emit('generate', {
    genre: genre.value,
    mood: mood.value,
    sceneCount: sceneCount.value,
    synopsis: synopsis.value,
  })
}
</script>

<template>
  <Card>
    <div class="form-header">
      <Sparkles class="header-icon" />
      <h3 class="header-title">스토리 구성 프롬프트</h3>
    </div>

    <div class="form-row">
      <div class="form-group">
        <label class="form-label">장르</label>
        <select v-model="genre" class="form-input form-select">
          <option
            v-for="option in genreOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label class="form-label">분위기</label>
        <select v-model="mood" class="form-input form-select">
          <option
            v-for="option in moodOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label class="form-label">씬 개수</label>
        <select v-model="sceneCount" class="form-input form-select">
          <option
            v-for="count in sceneCountOptions"
            :key="count"
            :value="count"
          >
            {{ count }}개
          </option>
        </select>
      </div>
    </div>

    <div class="form-group">
      <label class="form-label">전체 줄거리</label>
      <textarea
        v-model="synopsis"
        class="form-input form-textarea"
        placeholder="이야기의 전체적인 줄거리를 입력하세요..."
        rows="3"
      ></textarea>
    </div>

  <Button
    variant="primary"
    :loading="isGenerating"
    :disabled="isGenerating"
    @click="handleGenerate"
  >
    <template v-if="isGenerating">
      생성 중
    </template>
    <template v-else>
      <Sparkles class="icon-sm" />
      AI로 씬 생성
    </template>
  </Button>
  </Card>
</template>

<style scoped>
.form-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.header-icon {
  width: 20px;
  height: 20px;
  color: var(--rose-500);
}

.header-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-900);
  margin: 0;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-bottom: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--gray-700);
}

.form-textarea {
  min-height: 80px;
  resize: vertical;
  font-family: inherit;
}

/* Uses global .icon-sm from base.css */

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
