<script setup lang="ts">
import { Sparkles, Info } from 'lucide-vue-next'
import { useScenarioStore } from '../../stores/scenario'
import Button from '../common/Button.vue'
import FlowerLoader from '../common/FlowerLoader.vue'

const scenarioStore = useScenarioStore()

const genres = [
  { value: 'fantasy', label: '판타지' },
  { value: 'sf', label: 'SF' },
  { value: 'romance', label: '로맨스' },
  { value: 'action', label: '액션' },
  { value: 'thriller', label: '스릴러' },
  { value: 'comedy', label: '코미디' },
]

const moods = [
  { value: 'epic', label: '서사적' },
  { value: 'bright', label: '밝은' },
  { value: 'dark', label: '어두운' },
  { value: 'hopeful', label: '희망적' },
  { value: 'tense', label: '긴장감' },
  { value: 'comic', label: '유쾌한' },
]

const sceneCountOptions = [3, 4, 5, 6, 7]

const handleGeneratePrompt = () => {
  scenarioStore.generatePrompt()
}
</script>

<template>
  <div class="input-step">
    <div class="step-header">
      <Sparkles class="header-icon" />
      <h3 class="header-title">기본 정보 입력</h3>
    </div>
    <p class="step-description">
      장르와 분위기를 선택하고, 원하는 씬 개수를 설정해주세요.
    </p>

    <!-- Genre Selection -->
    <div class="form-group">
      <label class="form-label">장르 *</label>
      <div class="chip-group">
        <button
          v-for="genre in genres"
          :key="genre.value"
          :class="['chip', { selected: scenarioStore.input.genre === genre.value }]"
          @click="scenarioStore.input.genre = genre.value"
        >
          {{ genre.label }}
        </button>
      </div>
    </div>

    <!-- Mood Selection -->
    <div class="form-group">
      <label class="form-label">분위기/톤 *</label>
      <div class="chip-group">
        <button
          v-for="mood in moods"
          :key="mood.value"
          :class="['chip', { selected: scenarioStore.input.mood === mood.value }]"
          @click="scenarioStore.input.mood = mood.value"
        >
          {{ mood.label }}
        </button>
      </div>
    </div>

    <!-- Scene Count -->
    <div class="form-group">
      <label class="form-label">씬 개수</label>
      <div class="chip-group">
        <button
          v-for="count in sceneCountOptions"
          :key="count"
          :class="['chip chip-number', { selected: scenarioStore.input.sceneCount === count }]"
          @click="scenarioStore.input.sceneCount = count"
        >
          {{ count }}개
        </button>
      </div>
      <p class="form-hint"><Info class="hint-icon" /> 총 길이 60초 기준, 씬당 약 10~15초 권장</p>
    </div>

    <!-- Advanced Options (Collapsible) -->
    <details class="advanced-options">
      <summary class="advanced-summary">고급 옵션 (선택)</summary>
      <div class="advanced-content">
        <!-- Keywords -->
        <div class="form-group">
          <label class="form-label">주제/키워드</label>
          <input
            v-model="scenarioStore.input.keywords"
            type="text"
            class="form-input"
            placeholder="예: 우정, 복수, 희생"
          />
        </div>

        <!-- Character Hints -->
        <div class="form-group">
          <label class="form-label">메인 캐릭터 힌트</label>
          <input
            v-model="scenarioStore.input.characterHints"
            type="text"
            class="form-input"
            placeholder="예: 외로운 우주인, 반항하는 로봇"
          />
        </div>

        <!-- Background Hints -->
        <div class="form-group">
          <label class="form-label">배경 힌트</label>
          <input
            v-model="scenarioStore.input.backgroundHints"
            type="text"
            class="form-input"
            placeholder="예: 화성 기지, 중세 성"
          />
        </div>

        <!-- Reference Style -->
        <div class="form-group">
          <label class="form-label">참고할 작품/스타일</label>
          <input
            v-model="scenarioStore.input.referenceStyle"
            type="text"
            class="form-input"
            placeholder="예: 인터스텔라 느낌, 지브리 스타일"
          />
        </div>
      </div>
    </details>

    <!-- Generate Button -->
    <div class="step-footer">
    <div class="step-footer">
      <Button
        variant="primary"
        size="lg"
        :disabled="!scenarioStore.input.genre || !scenarioStore.input.mood || scenarioStore.isGenerating"
        @click="handleGeneratePrompt"
      >
        <template v-if="scenarioStore.isGenerating">
          <FlowerLoader />
          생성중
        </template>
        <template v-else>
          <Sparkles class="icon-sm" />
          시나리오 프롬프트 생성
        </template>
      </Button>
    </div>
    </div>
  </div>
</template>

<style scoped>
.input-step {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-icon {
  width: 24px;
  height: 24px;
  color: var(--rose-500);
}

.header-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--gray-900);
  margin: 0;
}

.step-description {
  color: var(--gray-500);
  font-size: 0.875rem;
  margin: 0;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--gray-700);
}

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
  background: var(--rose-50);
}

.chip.selected {
  background: var(--rose-100);
  border-color: var(--rose-400);
  color: var(--rose-600);
  font-weight: 500;
}

.chip-number {
  min-width: 60px;
  text-align: center;
}

.form-hint {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: var(--gray-500);
  margin: 0;
}

.hint-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.form-input {
  padding: 0.75rem 1rem;
  background: var(--gray-50);
  border: 1px solid var(--gray-200);
  border-radius: 8px;
  font-size: 0.875rem;
  color: var(--gray-900);
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

/* Advanced Options */
.advanced-options {
  background: var(--gray-50);
  border-radius: 12px;
  overflow: hidden;
}

.advanced-summary {
  padding: 0.875rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--gray-600);
  cursor: pointer;
  list-style: none;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.advanced-summary::-webkit-details-marker {
  display: none;
}

.advanced-summary::before {
  content: '▸';
  transition: transform 0.2s ease;
}

details[open] .advanced-summary::before {
  transform: rotate(90deg);
}

.advanced-content {
  padding: 0 1rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.step-footer {
  margin-top: 0.5rem;
}

/* Uses global .icon-sm from base.css */
</style>
