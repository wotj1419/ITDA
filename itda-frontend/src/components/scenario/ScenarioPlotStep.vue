<script setup lang="ts">
import { ref } from 'vue'
import { BookOpen, RefreshCw, Check, Edit3, Scroll, Info } from 'lucide-vue-next'
import { useScenarioStore } from '../../stores/scenario'
import Button from '../common/Button.vue'

const scenarioStore = useScenarioStore()
const isEditing = ref(false)
const editedText = ref('')

const startEdit = () => {
  editedText.value = scenarioStore.plot.text
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
  editedText.value = ''
}

const saveEdit = () => {
  scenarioStore.plot.text = editedText.value
  isEditing.value = false
}

const handleApprove = () => {
  scenarioStore.approvePlot()
}

const handleRegenerate = () => {
  scenarioStore.regeneratePlot()
}
</script>

<template>
  <div class="plot-step">
    <div class="step-header">
      <BookOpen class="header-icon" />
      <h3 class="header-title">전체 줄거리</h3>
    </div>
    <p class="step-description">
      AI가 생성한 전체 줄거리를 검토하고 필요시 수정해주세요.
    </p>

    <!-- Plot Content -->
    <div class="plot-card">
      <div class="plot-header">
        <span class="plot-label"><Scroll class="label-icon" /> 전체 줄거리</span>
        <div v-if="!isEditing" class="plot-actions">
          <button class="icon-button" title="편집" @click="startEdit">
            <Edit3 class="icon-sm" />
          </button>
        </div>
      </div>

      <!-- View Mode -->
      <div v-if="!isEditing" class="plot-content">
        <p class="plot-text">{{ scenarioStore.plot.text }}</p>
      </div>

      <!-- Edit Mode -->
      <div v-else class="plot-edit">
        <textarea
          v-model="editedText"
          class="plot-textarea"
          rows="8"
          placeholder="줄거리를 입력하세요..."
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
          scenarioStore.plot.status === 'approved' ? 'approved' : 'draft',
        ]"
      >
        {{ scenarioStore.plot.status === 'approved' ? '✓ 승인됨' : '대기 중' }}
      </span>
    </div>

    <!-- Action Buttons -->
    <div class="step-actions">
      <Button
        variant="secondary"
        :loading="scenarioStore.isGenerating"
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
        <Check class="icon-sm" />
        승인하고 씬 생성
      </Button>
    </div>

    <p class="step-hint">
      <Info class="hint-icon" /> 승인 후 이 줄거리를 바탕으로 씬별 스토리가 생성됩니다.
    </p>
  </div>
</template>

<style scoped>
.plot-step {
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

.plot-card {
  background: white;
  border: 1px solid var(--rose-100);
  border-radius: 12px;
  overflow: hidden;
}

.plot-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: var(--rose-50);
  border-bottom: 1px solid var(--rose-100);
}

.plot-label {
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

.plot-actions {
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

.plot-content {
  padding: 1rem;
  max-height: 300px;
  overflow-y: auto;
}

.plot-text {
  font-size: 0.9375rem;
  line-height: 1.7;
  color: var(--gray-700);
  margin: 0;
  white-space: pre-wrap;
}

.plot-edit {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.plot-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--gray-200);
  border-radius: 8px;
  font-size: 0.9375rem;
  line-height: 1.7;
  color: var(--gray-700);
  resize: vertical;
  font-family: inherit;
}

.plot-textarea:focus {
  outline: none;
  border-color: var(--rose-300);
  box-shadow: 0 0 0 3px rgba(244, 63, 94, 0.1);
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
