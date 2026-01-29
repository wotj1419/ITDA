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
import PresencePanel from '../components/collab/PresencePanel.vue';
import {
  BookOpen,
  Clapperboard,
  Layers,
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
    label: '스토리',
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
    label: '전체 타임라인',
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
          :class="{ 'menu-btn--open': uiStore.sidebarExpanded }"
          @click="uiStore.toggleSidebar"
          :title="uiStore.sidebarExpanded ? 'Collapse' : 'Expand'"
        >
          <span class="toggle" aria-hidden="true">
            <span class="bars bar1"></span>
            <span class="bars bar2"></span>
            <span class="bars bar3"></span>
          </span>
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

      <!-- Presence -->
      <div class="sidebar-section border-top">
        <div class="sidebar-text">
          <PresencePanel />
        </div>
      </div>

      <!-- Footer -->
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
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  position: relative;
}

.sidebar-collapsed {
  width: 72px;
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
  overflow: visible;
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
  position: relative;
  height: 44px;
}

.sidebar-collapsed .nav-item {
  width: 44px;
  height: 44px;
  padding: 0;
  justify-content: center;
  align-self: center;
  gap: 0;
}

.sidebar-collapsed .nav-icon {
  margin: 0;
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

/* Collapsed tooltips */
.sidebar-collapsed .nav-item::after {
  content: attr(data-tooltip);
  position: absolute;
  left: 100%;
  margin-left: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--gray-900);
  color: white;
  font-size: 0.75rem;
  border-radius: 6px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all 0.2s ease;
  z-index: 100;
}

.sidebar-collapsed .nav-item:hover::after {
  opacity: 1;
  visibility: visible;
}

/* ==========================================================================
   Sidebar Toggle
   ========================================================================== */

.sidebar-header-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  height: 64px;
}

.menu-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  padding: 0;
  transition: background 0.2s ease;
  flex-shrink: 0;
}

.menu-btn:hover {
  background: var(--rose-50);
}

.menu-btn .toggle {
  position: relative;
  width: 24px;
  height: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition-duration: 0.3s;
}

.menu-btn .bars {
  width: 24px;
  height: 3px;
  background-color: var(--rose-500);
  border-radius: 4px;
  transition-duration: 0.3s;
}

.menu-btn--open .bars {
  margin-left: 8px;
}

.menu-btn--open .bar2 {
  transform: rotate(135deg);
  margin-left: 0;
  transform-origin: center;
}

.menu-btn--open .bar1 {
  transform: rotate(45deg);
  transform-origin: left center;
}

.menu-btn--open .bar3 {
  transform: rotate(-45deg);
  transform-origin: left center;
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
  background: linear-gradient(135deg, var(--rose-50) 0%, var(--rose-75) 100%);
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
