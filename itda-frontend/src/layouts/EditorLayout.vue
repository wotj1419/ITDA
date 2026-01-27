<script setup lang="ts">
/**
 * EditorLayout - 에디터 전용 3컬럼 레이아웃
 * 왼쪽 사이드바 / 캔버스 영역 / 오른쪽 속성 패널 구조
 */
import { computed } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { useUIStore } from '../stores/ui';
import { useSidebarShortcut } from '../composables/useSidebarShortcut';
import Badge from '../components/common/Badge.vue';
import {
  BookOpen,
  Clapperboard,
  Layers,
  Menu,
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

// Keyboard shortcut (Ctrl+B)
useSidebarShortcut();

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
    label: 'Full Timeline',
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
      <!-- Back Button & Toggle -->
      <div class="sidebar-section border-bottom sidebar-header-row">
        <button
          class="menu-btn"
          @click="uiStore.toggleSidebar"
          :title="uiStore.sidebarExpanded ? 'Collapse' : 'Expand'"
        >
          <Menu class="icon-md" />
        </button>
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
        <slot name="properties" />
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

  /* Scene Editor "Original Premium" theme (scoped to editor only) */
  --editor-soft-pink: #ff4d8d;
  --editor-dot-pink: #ffd6e5;
  --rose-canvas: #fafafb;
  --rose-200: var(--editor-dot-pink);
  --rose-300: var(--editor-dot-pink);
  --rose-500: var(--editor-soft-pink);
  --rose-600: #ff3d85;
  --shadow-md: 0 8px 18px rgba(255, 77, 141, 0.14);
  --shadow-lg: 0 12px 26px rgba(255, 77, 141, 0.18);
  --shadow-xl: 0 20px 40px -10px rgba(255, 77, 141, 0.2);

  font-family: 'Plus Jakarta Sans', 'Noto Sans KR', 'Inter', -apple-system,
    BlinkMacSystemFont, sans-serif;
}

/* ==========================================================================
   Sidebar
   ========================================================================== */

.sidebar {
  width: 260px;
  height: 100vh;
  background: white;
  border-right: 1px solid var(--gray-100);
  display: flex;
  flex-direction: column;
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  position: relative;
}

.sidebar-collapsed {
  width: 64px;
}

/* Text elements - smooth fade transition */
.sidebar-text,
.nav-label {
  opacity: 1;
  transition: opacity 0.15s ease 0.2s; /* Fade in after sidebar expands */
  white-space: nowrap;
  overflow: hidden;
}

.sidebar-collapsed .sidebar-text,
.sidebar-collapsed .nav-label {
  opacity: 0;
  transition: opacity 0.1s ease; /* Fade out quickly when collapsing */
  pointer-events: none;
}

.sidebar-section {
  padding: 1rem;
  position: relative;
}

.border-bottom {
  border-bottom: 1px solid var(--gray-100);
}

.border-top {
  border-top: 1px solid var(--gray-100);
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
  gap: 0.5rem;
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
  background: rgba(255, 77, 141, 0.08);
  color: var(--rose-500);
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

/* ==========================================================================
   Sidebar Toggle
   ========================================================================== */

.sidebar-header-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  height: 56px;
}

.menu-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  color: var(--gray-500);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
  flex-shrink: 0;
}

.menu-btn:hover {
  background: var(--rose-50);
  color: var(--rose-600);
}

.back-link {
  flex: 1;
  padding-left: 0.5rem;
}

.sidebar-collapsed .back-link {
  display: none;
}

.sidebar-collapsed .menu-btn {
  margin: 0;
}

.icon-md {
  width: 24px;
  height: 24px;
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
  background: var(--rose-canvas);
}

/* Collapsed sidebar = reference-style icon tiles */
.sidebar-collapsed .sidebar-nav {
  padding: 1.5rem 0;
  align-items: center;
  gap: 1.5rem;
}

.sidebar-collapsed .nav-item {
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: 0.75rem;
  border: 1px solid var(--gray-100);
  background: white;
  justify-content: center;
  gap: 0;
}

.sidebar-collapsed .nav-item:hover {
  border-color: var(--rose-200);
  background: #fff9fb;
  color: var(--rose-500);
}

.sidebar-collapsed .nav-item.active {
  border-color: rgba(255, 77, 141, 0.15);
  background: rgba(255, 77, 141, 0.08);
  color: var(--rose-500);
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
