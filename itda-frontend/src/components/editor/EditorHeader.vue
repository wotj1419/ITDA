<script setup lang="ts">
/**
 * EditorHeader - 에디터 헤더 컴포넌트
 * 브레드크럼 네비게이션과 줌 레벨, 액션 버튼 표시
 */
import { RouterLink } from 'vue-router';
import Button from '../common/Button.vue';
import { ArrowLeft, Layers } from 'lucide-vue-next';

// =============================================================================
// Props
// =============================================================================

interface Props {
  projectTitle: string;
  sceneTitle: string;
  zoomLevel: string;
  projectId: number;
  sceneId?: number;
}

defineProps<Props>();
</script>

<template>
  <header class="editor-header">
    <!-- Left: Back Button & Breadcrumb -->
    <div class="header-left">
      <RouterLink
        :to="{ name: 'project-detail', params: { id: projectId } }"
        class="back-btn"
      >
        <ArrowLeft class="icon" />
      </RouterLink>

      <nav class="breadcrumb" aria-label="Breadcrumb">
        <RouterLink to="/dashboard" class="breadcrumb-link">
          AI Movie Studio
        </RouterLink>
        <span class="separator">/</span>
        <RouterLink
          :to="{ name: 'project-detail', params: { id: projectId } }"
          class="breadcrumb-link"
        >
          {{ projectTitle }}
        </RouterLink>
        <span class="separator">/</span>
        <span class="breadcrumb-current">{{ sceneTitle }}</span>
      </nav>
    </div>

    <!-- Right: Zoom & Actions -->
    <div class="header-right">
      <span class="zoom-indicator">{{ zoomLevel }}</span>

      <RouterLink
        :to="sceneId
          ? { name: 'scene-timeline', params: { id: projectId, sceneId } }
          : { name: 'timeline', params: { id: projectId } }"
        class="btn btn-secondary btn-sm"
      >
        <Layers class="icon-sm" />
        Scene Timeline
      </RouterLink>

      <Button variant="primary">Export Scene</Button>
    </div>
  </header>
</template>

<style scoped>
/* ==========================================================================
   Header
   ========================================================================== */

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  height: 64px; /* Align with sidebar header */
  background: white;
  border-bottom: 1px solid var(--rose-100);
}

/* ==========================================================================
   Left Section
   ========================================================================== */

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: var(--gray-600);
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: var(--gray-100);
  color: var(--gray-900);
}

/* ==========================================================================
   Breadcrumb
   ========================================================================== */

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.breadcrumb-link {
  color: var(--gray-500);
  text-decoration: none;
  transition: color 0.2s ease;
}

.breadcrumb-link:hover {
  color: var(--rose-500);
}

.separator {
  color: var(--gray-400);
}

.breadcrumb-current {
  color: var(--gray-900);
  font-weight: 500;
}

/* ==========================================================================
   Right Section
   ========================================================================== */

.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.zoom-indicator {
  font-size: 0.75rem;
  font-family: 'JetBrains Mono', monospace;
  background: var(--gray-100);
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  color: var(--gray-600);
}

.icon {
  width: 20px;
  height: 20px;
}

/* Uses global .icon-sm and button styles from base.css */
</style>
