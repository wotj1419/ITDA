<script setup lang="ts">
import { ref } from 'vue'
import { FileText, RefreshCw, Check, Edit3, Clapperboard, Info } from 'lucide-vue-next'
import { useScenarioStore } from '../../stores/scenario'
import Button from '../common/Button.vue'
import FlowerLoader from '../common/FlowerLoader.vue'

const scenarioStore = useScenarioStore()
const isEditing = ref(false)
const editedText = ref('')

const startEdit = () => {
  editedText.value = scenarioStore.prompt.text
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
  editedText.value = ''
}

const saveEdit = () => {
  scenarioStore.prompt.text = editedText.value
  isEditing.value = false
}

const handleApprove = () => {
  scenarioStore.approvePrompt()
}

const handleRegenerate = () => {
  scenarioStore.regeneratePrompt()
}
</script>

<template>
  <div class="prompt-step">
    <div class="step-header">
      <FileText class="header-icon" />
      <h3 class="header-title">시나리오 프롬프트 검토</h3>
    </div>
    <p class="step-description">
      AI가 생성한 시나리오 방향을 검토하고 필요시 수정해주세요.
    </p>

    <!-- Prompt Content -->
    <div class="prompt-card">
      <div class="prompt-header">
        <span class="prompt-label"><Clapperboard class="label-icon" /> 시나리오 방향</span>
        <div v-if="!isEditing" class="prompt-actions">
          <button class="icon-button" title="편집" @click="startEdit">
            <Edit3 class="icon-sm" />
          </button>
        </div>
      </div>

      <!-- View Mode -->
      <div v-if="!isEditing" class="prompt-content">
        <p class="prompt-text">{{ scenarioStore.prompt.text }}</p>
      </div>

      <!-- Edit Mode -->
      <div v-else class="prompt-edit">
        <textarea
          v-model="editedText"
          class="prompt-textarea"
          rows="6"
          placeholder="시나리오 방향을 입력하세요..."
        />
        <div class="edit-actions">
          <Button variant="secondary" size="sm" @click="cancelEdit">
            취소
          </Button>
          <Button variant="primary" size="sm" @click="saveEdit">
            저장
          </Button>
        </div>
      </div>
    </div>

    <!-- Status Badge -->
    <div class="status-row">
      <span
        :class="[
          'status-badge',
          scenarioStore.prompt.status === 'approved' ? 'approved' : 'draft',
        ]"
      >
        {{ scenarioStore.prompt.status === 'approved' ? '✓ 승인됨' : '대기 중' }}
      </span>
    </div>

    <!-- Action Buttons -->
    <div class="step-actions">
      <Button
        variant="secondary"
        :disabled="scenarioStore.isGenerating || isEditing"
        @click="handleRegenerate"
      >
        <RefreshCw class="icon-sm" />
        다시 생성
      </Button>
      <Button
        variant="primary"
        :disabled="scenarioStore.isGenerating || isEditing"
        @click="handleApprove"
      >
        <template v-if="scenarioStore.isGenerating">
          <FlowerLoader />
          생성중
        </template>
        <template v-else>
          <Check class="icon-sm" />
          승인하고 다음
        </template>
      </Button>
    </div>

    <p class="step-hint">
      <Info class="hint-icon" /> 승인 후 이 프롬프트를 바탕으로 전체 줄거리가 생성됩니다.
    </p>
  </div>
</template>

<style scoped>
.prompt-step {
  display: flex;
  flex-direction: column;
  gap: 1rem;
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

.prompt-card {
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 12px;
  overflow: hidden;
}

.prompt-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: var(--rose-50);
  border-bottom: 1px solid var(--rose-100);
}

.prompt-label {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--gray-700);
}

.label-icon {
  width: 16px;
  height: 16px;
}

.prompt-actions {
  display: flex;
  gap: 0.25rem;
}

.icon-button {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 1px solid var(--gray-200);
  border-radius: 6px;
  color: var(--gray-500);
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-button:hover {
  background: var(--gray-50);
  color: var(--gray-700);
  border-color: var(--gray-300);
}

.prompt-content {
  padding: 1rem;
}

.prompt-text {
  font-size: 0.9375rem;
  line-height: 1.6;
  color: var(--gray-700);
  margin: 0;
  white-space: pre-wrap;
}

.prompt-edit {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.prompt-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--gray-200);
  border-radius: 8px;
  font-size: 0.9375rem;
  line-height: 1.6;
  color: var(--gray-700);
  resize: vertical;
  font-family: inherit;
}

.prompt-textarea:focus {
  outline: none;
  border-color: var(--rose-300);
  box-shadow: 0 0 0 3px rgba(255, 133, 161, 0.1);
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.status-row {
  display: flex;
  justify-content: flex-start;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.375rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 500;
}

.status-badge.draft {
  background: var(--gray-100);
  color: var(--gray-600);
}

.status-badge.approved {
  background: var(--success-soft);
  color: var(--success-700);
}

.step-actions {
  display: flex;
  gap: 0.75rem;
}

.step-hint {
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

/* Uses global .icon-sm from base.css */
</style>
