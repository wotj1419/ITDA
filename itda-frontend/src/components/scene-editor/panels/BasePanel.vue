<script setup lang="ts">
/**
 * BasePanel - 怨듯넻 ?⑤꼸 ?덉씠?꾩썐
 * Sticky Footer ?⑦꽩 ?곸슜
 * 
 * ?ㅺ퀎 臾몄꽌: docs/vue-flow-node-workflow-design.md Section 6.2
 */

// =============================================================================
// Props & Slots
// =============================================================================
import { computed, inject } from 'vue';
import type { Component, ComputedRef } from 'vue';
import { Lock, Trash2, X } from 'lucide-vue-next';

interface Props {
  title: string;
  icon?: Component;
}

defineProps<Props>();

const panelClose = inject<(() => void) | null>('nodePanelClose', null);
const panelDelete = inject<(() => void) | null>('nodePanelDelete', null);
const panelCanDelete = inject<ComputedRef<boolean> | null>('nodePanelCanDelete', null);
const panelLock = inject<ComputedRef<{ name: string } | null> | null>('nodePanelLock', null);
const panelBusy = inject<ComputedRef<boolean> | null>('nodePanelBusy', null);
const isLocked = computed(() => Boolean(panelLock?.value));
const isBusy = computed(() => Boolean(panelBusy?.value));
const isDeleteDisabled = computed(() => isLocked.value || isBusy.value);
const canDelete = computed(() => Boolean(panelCanDelete && panelCanDelete.value));
</script>

<template>
  <div class="base-panel" :class="{ 'base-panel--locked': isLocked }">
    <!-- Header (Sticky) -->
    <header class="base-panel__header">
      <div class="base-panel__title-row">
        <span v-if="icon" class="base-panel__icon">
          <component :is="icon" />
        </span>
        <h3 class="base-panel__title">{{ title }}</h3>
      </div>
      <div v-if="panelClose" class="base-panel__actions">
        <slot name="header-actions" />
        <button
          v-if="canDelete && panelDelete"
          type="button"
          class="base-panel__delete"
          :disabled="isDeleteDisabled"
          title="노드 삭제"
          aria-label="노드 삭제"
          @click="panelDelete"
        >
          <Trash2 class="base-panel__delete-icon" />
          <span class="base-panel__delete-label">노드 삭제</span>
        </button>
        <button
          v-if="panelClose"
          class="base-panel__close"
          type="button"
          title="닫기"
          @click="panelClose"
        >
          <X class="base-panel__close-icon" />
        </button>
      </div>
    </header>

    <!-- Content (Scrollable) -->
    <div class="base-panel__content">
      <div v-if="isLocked" class="base-panel__lock-banner" role="alert">
        <Lock class="base-panel__lock-icon" />
        <span class="base-panel__lock-text">
          {{ panelLock?.name || '다른 사용자' }} 님이 작업 중이에요.</span>
      </div>
      <div class="base-panel__content-body">
        <slot />
      </div>
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
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.base-panel__header {
  position: sticky;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem 0.75rem;
  padding: 2rem 2rem 1.5rem;
  background: white;
  border-bottom: 1px solid #F3F4F6;
  z-index: 1;
}

.base-panel__title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1 1 240px;
  min-width: 0;
}

.base-panel__icon {
  width: 34px;
  height: 34px;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff5f8;
  border: 1px solid #ffe3ee;
  color: var(--rose-500, #ff4d8d);
  box-shadow: 0 4px 10px rgba(255, 77, 141, 0.12);
}

.base-panel__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.base-panel__title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--gray-900, #111827);
  word-break: keep-all;
  white-space: normal;
}

.base-panel__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 0 0 auto;
  margin-left: auto;
}

.base-panel__content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 0 2rem 2rem;
  background: #fff;
}

.base-panel__content-body {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.base-panel__lock-banner {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 0.75rem;
  margin-bottom: 1rem;
  border-radius: 0.75rem;
  border: 1px solid #fed7aa;
  background: #fff7ed;
  color: #9a3412;
  font-size: 0.75rem;
  font-weight: 600;
}

.base-panel__lock-icon {
  width: 14px;
  height: 14px;
}

.base-panel--locked .base-panel__content-body,
.base-panel--locked .base-panel__footer {
  pointer-events: none;
  opacity: 0.6;
}

.base-panel--locked .base-panel__actions button:not(.base-panel__close) {
  pointer-events: none;
  opacity: 0.6;
}

.base-panel__footer {
  position: sticky;
  bottom: 0;
  padding: 1.5rem 2rem 2rem;
  background: white;
  border-top: 1px solid #F3F4F6;
  z-index: 1;
}

.base-panel__delete {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.65rem;
  border-radius: 0.65rem;
  border: 1px solid rgba(239, 68, 68, 0.25);
  background: #fff;
  color: #b42318;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
}

.base-panel__delete:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  background: #fff;
  border-color: rgba(239, 68, 68, 0.15);
}

.base-panel__delete:hover {
  background: #fff1f2;
  border-color: rgba(239, 68, 68, 0.45);
}

.base-panel__delete:active {
  transform: scale(0.98);
}

.base-panel__delete-icon {
  width: 14px;
  height: 14px;
}

.base-panel__delete-label {
  line-height: 1;
}

.base-panel__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 0.75rem;
  border: 1px solid #F3F4F6;
  background: white;
  color: var(--gray-300, #d1d5db);
  cursor: pointer;
  transition: all 0.2s ease;
}

.base-panel__close:hover {
  background: var(--gray-50, #f9fafb);
  color: var(--rose-500, #ff4d8d);
  border-color: rgba(255, 77, 141, 0.2);
}

.base-panel__close-icon {
  width: 20px;
  height: 20px;
}
</style>
