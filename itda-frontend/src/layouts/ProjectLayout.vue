<script setup lang="ts">
import { computed, type Component } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import type { RouteLocationRaw } from 'vue-router'
import { useUIStore } from '../stores/ui'
import type { ProjectDetail } from '../types'
import Badge from '../components/common/Badge.vue'
import AvatarGroup from '../components/common/AvatarGroup.vue'
import Button from '../components/common/Button.vue'
import {
  ArrowLeft,
  BookOpen,
  Clapperboard,
  User,
  Layers,
  Settings,
  Phone,
  ChevronLeft,
  ChevronRight,
  Users,
  Play,
} from 'lucide-vue-next'

interface Props {
  project: ProjectDetail | null
  activeTab: 'story' | 'scenes' | 'characters' | 'timeline' | 'settings'
  sceneCount?: number
  progress?: { completed: number; total: number }
}

const props = withDefaults(defineProps<Props>(), {
  sceneCount: 0,
  progress: () => ({ completed: 0, total: 0 }),
})

const emit = defineEmits<{
  (e: 'tab-change', tab: string): void
}>()

const route = useRoute()
const uiStore = useUIStore()

const projectId = computed(() => props.project?.projectId || Number(route.params.id))

interface NavItem {
  key: string
  icon: Component
  label: string
  badge?: number
  to: RouteLocationRaw | null
}

const navItems = computed<NavItem[]>(() => [
  { key: 'story', icon: BookOpen, label: 'Story', to: null },
  { key: 'scenes', icon: Clapperboard, label: 'Scenes', badge: props.sceneCount, to: null },
  { key: 'characters', icon: User, label: 'Characters', to: null },
  { key: 'timeline', icon: Layers, label: 'Timeline', to: { name: 'timeline', params: { id: projectId.value } } },
  { key: 'settings', icon: Settings, label: 'Settings', to: null },
])

const avatarItems = computed(() =>
  (props.project?.members || []).slice(0, 3).map((member) => ({
    src: member.profileImage,
    alt: member.name,
    fallback: member.name?.[0]?.toUpperCase() || '?',
  }))
)

const sidebarClasses = computed(() => [
  'sidebar',
  { 'sidebar-collapsed': !uiStore.sidebarExpanded },
])

const handleNavClick = (item: typeof navItems.value[0]) => {
  if (item.to) {
    return // RouterLink will handle it
  }
  emit('tab-change', item.key)
}

const progressPercentage = computed(() => {
  if (props.progress.total === 0) return 0
  return Math.round((props.progress.completed / props.progress.total) * 100)
})
</script>

<template>
  <div class="app-container">
    <!-- Project Sidebar -->
    <aside :class="sidebarClasses">
      <!-- Back Link -->
      <div class="sidebar-section border-bottom">
        <RouterLink to="/dashboard" class="nav-item" data-tooltip="Back to Projects">
          <ArrowLeft class="nav-icon" />
          <span class="nav-label">Back to Projects</span>
        </RouterLink>
      </div>

      <!-- Project Info -->
      <div v-if="project" class="sidebar-section border-bottom sidebar-text">
        <h2 class="project-title">{{ project.title }}</h2>
        <Badge v-if="project.genre" variant="rose">{{ project.genre }}</Badge>
      </div>

      <!-- Navigation -->
      <nav class="sidebar-nav">
        <template v-for="item in navItems" :key="item.key">
          <RouterLink
            v-if="item.to"
            :to="item.to"
            :class="['nav-item', { active: activeTab === item.key }]"
            :data-tooltip="item.label"
          >
            <component :is="item.icon" class="nav-icon" />
            <span class="nav-label">{{ item.label }}</span>
            <span v-if="item.badge" class="nav-badge nav-label">{{ item.badge }}</span>
          </RouterLink>
          <button
            v-else
            :class="['nav-item', { active: activeTab === item.key }]"
            :data-tooltip="item.label"
            @click="handleNavClick(item)"
          >
            <component :is="item.icon" class="nav-icon" />
            <span class="nav-label">{{ item.label }}</span>
            <span v-if="item.badge" class="nav-badge nav-label">{{ item.badge }}</span>
          </button>
        </template>
      </nav>

      <!-- Online Now -->
      <div class="sidebar-section border-top">
        <div class="sidebar-text">
          <div class="section-label">ONLINE NOW</div>
          <div class="online-users">
            <AvatarGroup :avatars="avatarItems" :max="3" size="sm" />
          </div>
          <Button variant="secondary" class="start-call-btn">
            <Phone class="btn-icon" />
            <span class="nav-label">Start Call</span>
          </Button>
        </div>
        <!-- Sidebar Toggle -->
        <button class="sidebar-toggle" @click="uiStore.toggleSidebar">
          <ChevronLeft v-if="uiStore.sidebarExpanded" class="toggle-icon" />
          <ChevronRight v-else class="toggle-icon" />
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="main-wrapper">
      <!-- Header -->
      <header class="header">
        <div class="breadcrumb">
          <RouterLink to="/dashboard">AI Movie Studio</RouterLink>
          <span class="separator">/</span>
          <span class="current">{{ project?.title || 'Project' }}</span>
        </div>

        <div class="header-actions">
          <div class="progress-section">
            <span class="progress-label">Progress</span>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: `${progressPercentage}%` }"></div>
            </div>
            <span class="progress-text">{{ progress.completed }}/{{ progress.total }}</span>
          </div>

          <Button variant="secondary">
            <Users class="btn-icon" />
            협업 시작
          </Button>

          <Button variant="primary">
            <Play class="btn-icon" />
            Preview
          </Button>
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
  transition: width 0.3s ease;
  flex-shrink: 0;
}

.sidebar-collapsed {
  width: 72px;
}

.sidebar-collapsed .sidebar-text,
.sidebar-collapsed .nav-label,
.sidebar-collapsed .nav-badge {
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
  color: var(--gray-900);
  margin: 0 0 0.25rem;
}

/* Navigation */
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
  position: relative;
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

.nav-label {
  font-size: 0.875rem;
  font-weight: 500;
}

.nav-badge {
  margin-left: auto;
  padding: 0.125rem 0.5rem;
  background: var(--rose-100);
  color: var(--rose-600);
  font-size: 0.625rem;
  font-weight: 600;
  border-radius: 9999px;
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

/* Online Now Section */
.section-label {
  font-size: 0.625rem;
  font-weight: 600;
  color: var(--gray-500);
  margin-bottom: 0.75rem;
}

.online-users {
  margin-bottom: 0.75rem;
}

.start-call-btn {
  width: 100%;
  font-size: 0.75rem;
}

.btn-icon {
  width: 16px;
  height: 16px;
}

/* Sidebar Toggle */
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
  transition: all 0.2s ease;
}

.sidebar-toggle:hover {
  background: var(--rose-50);
  color: var(--rose-500);
}

.toggle-icon {
  width: 14px;
  height: 14px;
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
  padding: 1rem 2rem;
  background: white;
  border-bottom: 1px solid var(--rose-100);
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

.progress-section {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.progress-label {
  font-size: 0.75rem;
  color: var(--gray-500);
}

.progress-bar {
  width: 120px;
  height: 6px;
  background: var(--rose-100);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--rose-400), var(--rose-500));
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--rose-500);
}

/* Content */
.main-content {
  flex: 1;
  padding: 2rem;
  overflow-y: auto;
}
</style>
