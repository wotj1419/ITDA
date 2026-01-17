<script setup lang="ts">
interface Props {
  current: number
  total: number
  labels?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  labels: () => [],
})
</script>

<template>
  <div class="step-indicator">
    <div
      v-for="step in total"
      :key="step"
      :class="[
        'step',
        {
          completed: step < current,
          active: step === current,
          pending: step > current,
        },
      ]"
    >
      <div class="step-circle">
        <span v-if="step < current" class="checkmark">✓</span>
        <span v-else>{{ step }}</span>
      </div>
      <span v-if="labels[step - 1]" class="step-label">{{ labels[step - 1] }}</span>
      <div v-if="step < total" class="step-line" />
    </div>
  </div>
</template>

<style scoped>
.step-indicator {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  gap: 0;
  padding: 0.5rem 0;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  flex: 1;
  max-width: 100px;
}

.step-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 600;
  transition: all 0.3s ease;
  position: relative;
  z-index: 1;
}

.step.pending .step-circle {
  background: var(--gray-100);
  color: var(--gray-400);
  border: 2px solid var(--gray-200);
}

.step.active .step-circle {
  background: var(--rose-500);
  color: white;
  border: 2px solid var(--rose-500);
  box-shadow: 0 0 0 4px rgba(244, 63, 94, 0.2);
}

.step.completed .step-circle {
  background: var(--rose-100);
  color: var(--rose-600);
  border: 2px solid var(--rose-300);
}

.checkmark {
  font-size: 0.75rem;
}

.step-label {
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: var(--gray-500);
  text-align: center;
  white-space: nowrap;
}

.step.active .step-label {
  color: var(--rose-600);
  font-weight: 500;
}

.step.completed .step-label {
  color: var(--gray-700);
}

.step-line {
  position: absolute;
  top: 16px;
  left: calc(50% + 20px);
  width: calc(100% - 40px);
  height: 2px;
  background: var(--gray-200);
}

.step.completed .step-line {
  background: var(--rose-300);
}

.step.active .step-line {
  background: linear-gradient(to right, var(--rose-300), var(--gray-200));
}
</style>
