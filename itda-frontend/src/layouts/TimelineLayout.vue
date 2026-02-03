<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useUIStore } from '../stores/ui'
import { useSidebarShortcut } from '../composables/useSidebarShortcut'
import PresencePanel from '../components/collab/PresencePanel.vue'
import SidebarHoverMenu from '../components/collab/SidebarHoverMenu.vue'
import {
  ArrowLeft,
  Layers,
  Film,
  Clock,
} from 'lucide-vue-next'

interface Props {
  projectTitle: string
  clipCount: number
  totalDuration: number
  isSceneTimeline?: boolean
  sceneTitle?: string
  sceneId?: number | null
}

const props = withDefaults(defineProps<Props>(), {
  projectTitle: 'Project',
  clipCount: 0,
  totalDuration: 0,
  isSceneTimeline: false,
  sceneTitle: '',
  sceneId: null,
})

const timelineLabel = computed(() => props.isSceneTimeline ? '씬 타임라인' : '전체 타임라인')

const route = useRoute()
const router = useRouter()
const uiStore = useUIStore()

// Keyboard shortcut (Ctrl+B)
useSidebarShortcut()

// Hover logic removed as per user request for manual toggle only
const projectId = computed(() => Number(route.params.id))

const sidebarClasses = computed(() => [
  'sidebar',
  { 'sidebar-collapsed': !uiStore.sidebarExpanded },
])

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = Math.round(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

</script>

<template>
  <div class="app-container">
    <!-- Sidebar -->
    <aside :class="sidebarClasses">
      <!-- Header with Toggle -->
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

      <!-- Timeline Info -->
      <div class="sidebar-section border-bottom sidebar-text">
        <h2 class="project-title">{{ projectTitle }}</h2>
        <div class="timeline-badge">
          <Layers class="badge-icon" />
          <span>{{ timelineLabel }}</span>
        </div>
      </div>

      <!-- Stats -->
      <div class="sidebar-section sidebar-text">
        <div class="stat-item">
          <Film class="stat-icon" />
          <div class="stat-content">
            <span class="stat-value">{{ clipCount }}</span>
            <span class="stat-label nav-label">클립</span>
          </div>
        </div>
        <div class="stat-item">
          <Clock class="stat-icon" />
          <div class="stat-content">
            <span class="stat-value">{{ formatDuration(totalDuration) }}</span>
            <span class="stat-label nav-label">총 길이</span>
          </div>
        </div>
      </div>

      <!-- Presence -->
      <div class="sidebar-section border-top collab-section">
        <div class="sidebar-text presence-block">
          <PresencePanel />
        </div>
        <SidebarHoverMenu :project-id="projectId" :expanded="uiStore.sidebarExpanded" />
      </div>
    </aside>

    <!-- Main Content -->
    <main class="main-wrapper">
      <!-- Header -->
      <header class="header">
        <div class="header-left">
          <button class="btn-icon-back" @click="router.back()" title="Go Back">
            <ArrowLeft class="icon-md" />
          </button>
          
          <div class="breadcrumb">
            <RouterLink to="/dashboard">홈</RouterLink>
            <span class="separator">/</span>
            <RouterLink :to="{ name: 'project-detail', params: { id: projectId } }">
              {{ projectTitle }}
            </RouterLink>
            <span class="separator">/</span>
            <template v-if="isSceneTimeline && sceneTitle && sceneId">
              <RouterLink 
                :to="{ name: 'scene-edit', params: { projectId: projectId, sceneId: sceneId } }"
                class="breadcrumb-link"
              >
                {{ sceneTitle }}
              </RouterLink>
              <span class="separator">/</span>
            </template>
            <span class="current">{{ timelineLabel }}</span>
          </div>
        </div>

        <div class="header-actions">
          <slot name="actions" />
        </div>
      </header>

      <!-- Content -->
      <div class="main-content">
        <slot />
      </div>
    </main>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  min-height: 100vh;
  background: var(--rose-canvas);
}

/* Sidebar */
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
  border-bottom: 1px solid var(--gray-100);
}

.border-top {
  border-top: 1px solid var(--rose-100);
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
  color: var(--gray-900);
  margin: 0 0 0.5rem;
}

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

.icon-md {
  width: 24px;
  height: 24px;
}

.timeline-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.5rem;
  background: var(--rose-100);
  color: var(--rose-600);
  font-size: 0.75rem;
  font-weight: 500;
  border-radius: 4px;
}

.badge-icon {
  width: 14px;
  height: 14px;
}

/* Navigation */
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
}

.nav-item:hover {
  background: var(--rose-50);
  color: var(--gray-900);
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

/* Stats */
.stat-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
}

.stat-icon {
  width: 20px;
  height: 20px;
  color: var(--rose-400);
  flex-shrink: 0;
}

.stat-content {
  display: flex;
  align-items: baseline;
  gap: 0.375rem;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--gray-900);
}

.stat-label {
  font-size: 0.75rem;
  color: var(--gray-500);
}



/* Collapsed tooltips */
.sidebar-collapsed .nav-item::after {
  content: attr(data-tooltip);
  position: absolute;
  left: 100%;
  margin-left: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--rose-50);
  color: var(--gray-900);
  border: 1px solid var(--rose-100);
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

/* Main */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Header */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 20;
  padding: 0.75rem 1.5rem; /* Reduced padding top/bottom to match fixed height */
  height: 64px;
  background: white;
  border-bottom: 1px solid var(--gray-100);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.btn-icon-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--gray-600);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.btn-icon-back:hover {
  background: var(--gray-50);
  color: var(--gray-900);
}

.btn-icon-back .icon-md {
  width: 20px;
  height: 20px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.breadcrumb a {
  color: var(--gray-500);
  text-decoration: none;
}

.breadcrumb a:hover {
  color: var(--rose-500);
}

.separator {
  color: var(--gray-300);
}

.current {
  color: var(--gray-900);
  font-weight: 500;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.duration-badge {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.75rem;
  background: var(--rose-50);
  color: var(--rose-600);
  font-size: 0.875rem;
  font-weight: 600;
  border-radius: 8px;
}

/* Content */
.main-content {
  flex: 1;
  padding: 2rem;
  overflow-y: auto;
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
