<script setup lang="ts">
import type { MergeStatus } from '../../stores/timeline'
import Button from '../common/Button.vue'
import { Check, Play, Download, Loader2 } from 'lucide-vue-next'

interface Props {
  status: MergeStatus
  progress: number
  statusText: string
  downloadUrl?: string | null
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'preview'): void
  (e: 'download'): void
}>()
</script>

<template>
  <div v-if="status !== 'idle'" class="merge-section">
    <!-- Progress -->
    <div v-if="status === 'merging'" class="merge-progress">
      <div class="progress-header">
        <Loader2 class="spinner" />
        <div class="progress-info">
          <span class="progress-title">영상 병합 중...</span>
          <span class="progress-status">{{ statusText }}</span>
        </div>
      </div>
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: `${progress}%` }"></div>
      </div>
      <div class="progress-percent">{{ progress }}%</div>
    </div>

    <!-- Complete -->
    <div v-if="status === 'done'" class="merge-complete">
      <div class="complete-header">
        <div class="complete-icon">
          <Check class="check-icon" />
        </div>
        <div>
          <h3 class="complete-title">병합 완료!</h3>
          <p class="complete-subtitle">영상이 준비되었습니다.</p>
        </div>
      </div>
      <div class="complete-actions">
        <Button variant="secondary" @click="emit('preview')">
          <Play class="btn-icon" />
          미리보기
        </Button>
        <Button variant="primary" @click="emit('download')">
          <Download class="btn-icon" />
          다운로드 (MP4)
        </Button>
      </div>
    </div>

    <!-- Error -->
    <div v-if="status === 'error'" class="merge-error">
      <div class="error-content">
        <span class="error-icon">⚠️</span>
        <span>병합 중 오류가 발생했습니다. 다시 시도해주세요.</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.merge-section {
  background: linear-gradient(135deg, var(--rose-50), var(--rose-100));
  padding: 1.5rem;
  border-radius: 12px;
  border: 1px solid var(--rose-200);
}

/* Progress */
.merge-progress {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.spinner {
  width: 20px;
  height: 20px;
  color: var(--rose-500);
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.progress-info {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.progress-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--gray-900);
}

.progress-status {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.progress-bar {
  height: 8px;
  background: var(--rose-200);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--rose-400), var(--rose-500));
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-percent {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--rose-600);
  text-align: right;
}

/* Complete */
.merge-complete {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.complete-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.complete-icon {
  width: 48px;
  height: 48px;
  background: var(--success-bg, #dcfce7);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.check-icon {
  width: 24px;
  height: 24px;
  color: var(--success, #22c55e);
}

.complete-title {
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 0.125rem;
  color: var(--gray-900);
}

.complete-subtitle {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin: 0;
}

.complete-actions {
  display: flex;
  gap: 0.75rem;
}

.btn-icon {
  width: 16px;
  height: 16px;
}

/* Error */
.merge-error {
  background: #fef2f2;
  padding: 1rem;
  border-radius: 8px;
  border: 1px solid #fecaca;
}

.error-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #dc2626;
}

.error-icon {
  font-size: 1.25rem;
}
</style>
