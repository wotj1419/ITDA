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
import SidebarHoverMenu from '../components/collab/SidebarHoverMenu.vue';
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
  sceneTitle: '씬',
  sceneBadge: '씬 편집',
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
    label: '씬 편집',
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
      <div class="sidebar-section border-top collab-section">
        <div class="sidebar-text presence-block">
          <PresencePanel />
        </div>
        <SidebarHoverMenu :project-id="projectId" :expanded="uiStore.sidebarExpanded" />
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
  border-right: 1px solid var(--rose-100, #FFF0F5);
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
  border-bottom: 1px solid var(--rose-100, #FFF0F5);
}

.border-top {
  border-top: 1px solid var(--rose-100, #FFF0F5);
  margin-top: auto;
}

.collab-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  transition: border-color 0.2s ease;
}

.sidebar-collapsed .collab-section {
  justify-content: flex-end;
  padding-bottom: 0.5rem;
  border-top-color: transparent;
}

.presence-block {
  max-height: 320px;
  opacity: 1;
  overflow: hidden;
  transform: translateY(0);
  transition:
    max-height 0.25s ease,
    opacity 0.15s ease 0.2s,
    transform 0.2s ease;
}

.sidebar-collapsed .presence-block {
  max-height: 0;
  opacity: 0;
  transform: translateY(6px);
  pointer-events: none;
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
  width: 100%;
  height: 44px;
  padding: 0.75rem 0.625rem;
  justify-content: flex-start;
  align-self: stretch;
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
  background: var(--rose-100, #FFF0F5);
  color: var(--rose-600, #FF6B8A);
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.nav-label {
  font-size: 0.875rem;
  font-weight: 500;
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

/* Collapsed tooltips (match dashboard) */
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
  min-height: 0;
  overflow: hidden;
}

.editor-content {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: visible;
}

.editor-canvas-area {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: var(--rose-canvas);
  z-index: 0;

  /* Scene Editor canvas-only rose tone */
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

.start-call-btn {
  width: 100%;
  font-size: 0.75rem;
  white-space: nowrap;
  position: relative;
  overflow: hidden;
  transition:
    width 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    height 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    padding 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    border-radius 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

.start-call-btn :deep(.btn-label) {
  display: inline-flex;
  align-items: center;
  gap: 0.9rem;
}

.call-label {
  transition: opacity 0.2s ease, transform 0.25s ease;
  display: inline-block;
}

.start-call-btn .icon-sm {
  transition: transform 0.25s ease;
}

.call-cta {
  margin-top: 1rem;
  padding: 0.5rem;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--rose-50), white);
  border: 1px solid var(--rose-100);
  box-shadow: 0 8px 18px rgba(255, 133, 161, 0.08);
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  overflow: hidden;
  max-height: 88px;
  transition:
    margin 0.25s ease,
    padding 0.25s ease,
    background 0.25s ease,
    border-color 0.25s ease,
    box-shadow 0.25s ease,
    max-height 0.25s ease;
}

.call-hint {
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--gray-500);
  margin-bottom: 0.375rem;
  transition: opacity 0.2s ease, max-height 0.2s ease, margin 0.2s ease;
  max-height: 20px;
}

.sidebar-collapsed .call-cta {
  margin-top: 0.5rem;
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
  display: flex;
  justify-content: center;
  max-height: 48px;
}

.sidebar-collapsed .call-hint {
  opacity: 0;
  max-height: 0;
  margin: 0;
}

.sidebar-collapsed .start-call-btn {
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: 999px;
  gap: 0;
  box-shadow: 0 6px 12px rgba(255, 133, 161, 0.18);
  transition: width 0.25s ease, height 0.25s ease, padding 0.25s ease, box-shadow 0.25s ease;
}

.sidebar-collapsed .call-label {
  opacity: 0;
  transform: translateX(6px) scale(0.9);
}

.sidebar-collapsed .start-call-btn .icon-sm {
  transform: scale(1.05);
}
</style>
