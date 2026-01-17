<script setup lang="ts">
import { Sparkles, X, ChevronLeft } from 'lucide-vue-next'
import { useScenarioStore } from '../../stores/scenario'
import StepIndicator from './StepIndicator.vue'
import ScenarioInputStep from './ScenarioInputStep.vue'
import ScenarioPromptStep from './ScenarioPromptStep.vue'
import ScenarioPlotStep from './ScenarioPlotStep.vue'
import ScenarioScenesStep from './ScenarioScenesStep.vue'
import Button from '../common/Button.vue'

const scenarioStore = useScenarioStore()

const handleClose = () => {
  scenarioStore.closeDrawer()
}

const handlePrevStep = () => {
  scenarioStore.prevStep()
}
</script>

<template>
  <Teleport to="body">
    <!-- Backdrop -->
    <Transition name="fade">
      <div
        v-if="scenarioStore.isDrawerOpen"
        class="drawer-backdrop"
        @click="handleClose"
      />
    </Transition>

    <!-- Drawer -->
    <Transition name="slide">
      <aside
        v-if="scenarioStore.isDrawerOpen"
        class="scenario-drawer"
        role="dialog"
        aria-modal="true"
        aria-labelledby="drawer-title"
      >
        <!-- Header -->
        <header class="drawer-header">
          <div class="header-left">
            <Sparkles class="header-icon" />
            <h2 id="drawer-title" class="drawer-title">AI 시나리오 생성</h2>
          </div>
          <button class="close-btn" aria-label="닫기" @click="handleClose">
            <X class="icon-close" />
          </button>
        </header>

        <!-- Step Indicator -->
        <div class="step-indicator-wrapper">
          <StepIndicator
            :current="scenarioStore.currentStep"
            :total="4"
            :labels="scenarioStore.stepLabels"
          />
        </div>

        <!-- Content -->
        <main class="drawer-content">
          <Transition name="step-fade" mode="out-in">
            <ScenarioInputStep v-if="scenarioStore.currentStep === 1" key="step1" />
            <ScenarioPromptStep v-else-if="scenarioStore.currentStep === 2" key="step2" />
            <ScenarioPlotStep v-else-if="scenarioStore.currentStep === 3" key="step3" />
            <ScenarioScenesStep v-else-if="scenarioStore.currentStep === 4" key="step4" />
          </Transition>
        </main>

        <!-- Footer Navigation -->
        <footer class="drawer-footer">
          <Button
            v-if="scenarioStore.currentStep > 1"
            variant="secondary"
            @click="handlePrevStep"
          >
            <ChevronLeft class="icon-sm" />
            이전
          </Button>
          <div v-else />
          
          <!-- Next button only shows on step 1 -->
          <!-- Other steps handle navigation through their own buttons -->
        </footer>
      </aside>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* Backdrop */
.drawer-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 1000;
}

/* Drawer Base */
.scenario-drawer {
  position: fixed;
  z-index: 1001;
  background: white;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
}

/* Desktop: Right Sidebar */
@media (min-width: 769px) {
  .scenario-drawer {
    top: 0;
    right: 0;
    bottom: 0;
    width: 420px;
    max-width: 90vw;
  }
}

/* Mobile: Fullscreen Modal */
@media (max-width: 768px) {
  .scenario-drawer {
    inset: 0;
    border-radius: 0;
  }
}

/* Header */
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--rose-100);
  background: var(--rose-50);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-icon {
  width: 22px;
  height: 22px;
  color: var(--rose-500);
}

.drawer-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--gray-900);
  margin: 0;
}

.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 1px solid var(--gray-200);
  border-radius: 8px;
  color: var(--gray-500);
  cursor: pointer;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: var(--gray-50);
  border-color: var(--gray-300);
  color: var(--gray-700);
}

.icon-close {
  width: 20px;
  height: 20px;
}

/* Step Indicator */
.step-indicator-wrapper {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--rose-50);
}

/* Content */
.drawer-content {
  flex: 1;
  padding: 1.25rem;
  overflow-y: auto;
}

/* Footer */
.drawer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--rose-100);
  background: var(--gray-50);
}

/* Animations */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Desktop slide from right */
@media (min-width: 769px) {
  .slide-enter-active,
  .slide-leave-active {
    transition: transform 0.3s ease;
  }

  .slide-enter-from,
  .slide-leave-to {
    transform: translateX(100%);
  }
}

/* Mobile slide from bottom */
@media (max-width: 768px) {
  .slide-enter-active,
  .slide-leave-active {
    transition: transform 0.3s ease;
  }

  .slide-enter-from,
  .slide-leave-to {
    transform: translateY(100%);
  }
}

/* Step content transition */
.step-fade-enter-active,
.step-fade-leave-active {
  transition: all 0.2s ease;
}

.step-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.step-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* Uses global .icon-sm from base.css */
</style>
