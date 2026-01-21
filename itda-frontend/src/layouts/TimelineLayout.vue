<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useUIStore } from '../stores/ui'
import { useSidebarShortcut } from '../composables/useSidebarShortcut'
import {
  ArrowLeft,
  Layers,
  Menu,
  Film,
  Clock,
} from 'lucide-vue-next'

interface Props {
  projectTitle: string
  clipCount: number
  totalDuration: number
}

const props = withDefaults(defineProps<Props>(), {
  projectTitle: 'Project',
  clipCount: 0,
  totalDuration: 0,
})

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
          @click="uiStore.toggleSidebar"
          :title="uiStore.sidebarExpanded ? 'Collapse' : 'Expand'"
        >
          <Menu class="icon-md" />
        </button>
      </div>

      <!-- Timeline Info -->
      <div class="sidebar-section border-bottom sidebar-text">
        <h2 class="project-title">{{ projectTitle }}</h2>
        <div class="timeline-badge">
          <Layers class="badge-icon" />
          <span>Full Timeline</span>
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
            <RouterLink to="/dashboard">AI Movie Studio</RouterLink>
            <span class="separator">/</span>
            <RouterLink :to="{ name: 'project-detail', params: { id: projectId } }">
              {{ projectTitle }}
            </RouterLink>
            <span class="separator">/</span>
            <span class="current">Full Timeline</span>
          </div>
        </div>

        <div class="header-actions">
          <slot name="actions" />
          <div class="duration-badge">
            <Clock class="badge-icon" />
            <span>{{ formatDuration(totalDuration) }} / 1:00</span>
          </div>
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
}

.menu-btn:hover {
  background: var(--rose-50);
  color: var(--rose-600);
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
  padding: 0 2rem; /* Reduced padding top/bottom to match fixed height */
  height: 64px;
  background: white;
  border-bottom: 1px solid var(--rose-100);
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
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--gray-500);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.btn-icon-back:hover {
  background: var(--rose-50);
  color: var(--rose-600);
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
</style>
