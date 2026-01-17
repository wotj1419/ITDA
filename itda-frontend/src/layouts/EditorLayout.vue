<script setup lang="ts">
/**
 * EditorLayout - 에디터 전용 3컬럼 레이아웃
 * 왼쪽 사이드바 / 캔버스 영역 / 오른쪽 속성 패널 구조
 */
import { computed } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { useUIStore } from '../stores/ui';
import Badge from '../components/common/Badge.vue';
import {
  ArrowLeft,
  BookOpen,
  Clapperboard,
  Layers,
  ChevronLeft,
  ChevronRight,
} from 'lucide-vue-next';

// =============================================================================
// Props
// =============================================================================

interface Props {
  projectTitle?: string;
  sceneTitle?: string;
  sceneBadge?: string;
}

const props = withDefaults(defineProps<Props>(), {
  projectTitle: 'Project',
  sceneTitle: 'Scene',
  sceneBadge: 'Scene Editor',
});

// =============================================================================
// State & Composables
// =============================================================================

const route = useRoute();
const uiStore = useUIStore();

// =============================================================================
// Computed
// =============================================================================

const projectId = computed(() => Number(route.params.projectId));

const navItems = computed(() => [
  {
    key: 'story',
    icon: BookOpen,
    label: 'Story',
    to: { name: 'project-detail', params: { id: projectId.value } },
    active: false,
  },
  {
    key: 'scene-editor',
    icon: Clapperboard,
    label: 'Scene Editor',
    to: null,
    active: true,
  },
  {
    key: 'timeline',
    icon: Layers,
    label: 'Timeline',
    to: { name: 'timeline', params: { id: projectId.value } },
    active: false,
  },
]);

const sidebarClasses = computed(() => [
  'sidebar',
  { 'sidebar-collapsed': !uiStore.sidebarExpanded },
]);
</script>

<template>
  <div class="app-container editor-app">
    <!-- Sidebar -->
    <aside :class="sidebarClasses">
      <!-- Back Button -->
      <div class="sidebar-section border-bottom">
        <RouterLink
          :to="{ name: 'project-detail', params: { id: projectId } }"
          class="nav-item"
          data-tooltip="Back to Project"
        >
          <ArrowLeft class="nav-icon" />
          <span class="nav-label">Back to Project</span>
        </RouterLink>
      </div>

      <!-- Project Info -->
      <div class="sidebar-section border-bottom sidebar-text">
        <h2 class="project-title">{{ projectTitle }}</h2>
        <Badge variant="rose">{{ sceneBadge }}</Badge>
      </div>

      <!-- Navigation -->
      <nav class="sidebar-nav">
        <template v-for="item in navItems" :key="item.key">
          <RouterLink
            v-if="item.to"
            :to="item.to"
            class="nav-item"
            :data-tooltip="item.label"
          >
            <component :is="item.icon" class="nav-icon" />
            <span class="nav-label">{{ item.label }}</span>
          </RouterLink>
          <button
            v-else
            :class="['nav-item', { active: item.active }]"
            :data-tooltip="item.label"
          >
            <component :is="item.icon" class="nav-icon" />
            <span class="nav-label">{{ item.label }}</span>
          </button>
        </template>
      </nav>

      <!-- Footer with Toggle -->
      <div class="sidebar-section border-top">
        <div class="sidebar-text text-xs text-muted">{{ sceneTitle }}</div>
        <button class="sidebar-toggle" @click="uiStore.toggleSidebar">
          <ChevronLeft v-if="uiStore.sidebarExpanded" class="toggle-icon" />
          <ChevronRight v-else class="toggle-icon" />
        </button>
      </div>
    </aside>

    <!-- Main Content - 3 Column Layout -->
    <main class="editor-main">
      <!-- Header Slot -->
      <slot name="header" />

      <!-- Editor Content -->
      <div class="editor-content">
        <!-- Canvas Area -->
        <div class="editor-canvas-area">
          <slot name="canvas" />
        </div>

        <!-- Right Sidebar (Properties) -->
        <aside class="editor-properties">
          <slot name="properties" />
        </aside>
      </div>

      <!-- Bottom Bar (Mini Timeline) -->
      <slot name="bottom" />
    </main>
  </div>
</template>

<style scoped>
/* ==========================================================================
   Layout Container
   ========================================================================== */

.editor-app {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ==========================================================================
   Sidebar
   ========================================================================== */

.sidebar {
  width: 260px;
  height: 100vh;
  background: white;
  border-right: 1px solid var(--rose-100);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  flex-shrink: 0;
}

.sidebar-collapsed {
  width: 72px;
}

.sidebar-collapsed .sidebar-text,
.sidebar-collapsed .nav-label {
  display: none;
}

.sidebar-section {
  padding: 1rem;
  position: relative;
}

.border-bottom {
  border-bottom: 1px solid var(--rose-100);
}

.border-top {
  border-top: 1px solid var(--rose-100);
  margin-top: auto;
}

.project-title {
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 0.25rem;
}

/* ==========================================================================
   Navigation
   ========================================================================== */

.sidebar-nav {
  flex: 1;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border-radius: 8px;
  color: var(--gray-600);
  text-decoration: none;
  background: transparent;
  border: none;
  width: 100%;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.nav-item:hover {
  background: var(--rose-50);
  color: var(--gray-900);
}

.nav-item.active {
  background: var(--rose-100);
  color: var(--rose-600);
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

/* ==========================================================================
   Sidebar Toggle
   ========================================================================== */

.sidebar-toggle {
  position: absolute;
  right: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  background: white;
  border: 1px solid var(--rose-200);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-500);
  z-index: 10;
}

.toggle-icon {
  width: 14px;
  height: 14px;
}

/* ==========================================================================
   Editor Main Area
   ========================================================================== */

.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.editor-canvas-area {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, var(--rose-50) 0%, #fdf2f8 100%);
}

.editor-properties {
  width: 400px;
  background: white;
  border-left: 1px solid var(--rose-200);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* ==========================================================================
   Utility Classes
   ========================================================================== */

.text-xs {
  font-size: 0.75rem;
}

.text-muted {
  color: var(--gray-500);
}
</style>
