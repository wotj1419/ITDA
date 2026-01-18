<script setup lang="ts">
/**
 * BasePanel - 공통 패널 레이아웃
 * Sticky Footer 패턴 적용
 * 
 * 설계 문서: docs/vue-flow-node-workflow-design.md Section 6.2
 */

// =============================================================================
// Props & Slots
// =============================================================================
import type { Component } from 'vue';

interface Props {
  title: string;
  icon?: Component;
}

defineProps<Props>();
</script>

<template>
  <div class="base-panel">
    <!-- Header (Sticky) -->
    <header class="base-panel__header">
      <span v-if="icon" class="base-panel__icon">
        <component :is="icon" />
      </span>
      <h3 class="base-panel__title">{{ title }}</h3>
    </header>

    <!-- Content (Scrollable) -->
    <div class="base-panel__content">
      <slot />
    </div>

    <!-- Footer (Sticky) -->
    <footer v-if="$slots.footer" class="base-panel__footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<style scoped>
.base-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.base-panel__header {
  position: sticky;
  top: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem 1.25rem;
  background: var(--rose-50, #FFFAFC);
  border-bottom: 1px solid var(--rose-100, #FFF0F5);
  z-index: 1;
}

.base-panel__icon {
  width: 32px;
  height: 32px;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 1px solid var(--rose-200, #FFE8F2);
  color: var(--rose-500, #FF85A1);
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.05));
}

.base-panel__icon :deep(svg) {
  width: 16px;
  height: 16px;
}

.base-panel__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--gray-900, #1A1A2E);
}

.base-panel__content {
  flex: 1;
  overflow-y: auto;
  padding: 1.25rem;
}

.base-panel__footer {
  position: sticky;
  bottom: 0;
  padding: 1rem 1.25rem;
  background: var(--gray-50, #FAFAFA);
  border-top: 1px solid var(--rose-100, #FFF0F5);
  z-index: 1;
}
</style>
