<script setup lang="ts">
import { computed, ref, type Component } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import type { RouteLocationRaw } from 'vue-router'
import { useUIStore } from '../stores/ui'
import { useSidebarShortcut } from '../composables/useSidebarShortcut'
import type { ProjectDetail } from '../types/api/projects'
import Badge from '../components/common/Badge.vue'
import Button from '../components/common/Button.vue'
import ShareButton from '../components/common/ShareButton.vue'
import ShareProjectModal from '../components/project/ShareProjectModal.vue'
import ProjectInfoDrawer from '../components/project/ProjectInfoDrawer.vue'
import PresencePanel from '../components/collab/PresencePanel.vue'
import SidebarHoverMenu from '../components/collab/SidebarHoverMenu.vue'
import {
  BookOpen,
  Clapperboard,
  User,
  Layers,
  Settings,
  Play,
  ArrowLeft,
  Pencil,
} from 'lucide-vue-next'


interface Props {
  project: ProjectDetail | null
  activeTab: 'story' | 'scenes' | 'objects' | 'timeline' | 'settings'
  sceneCount?: number
  progress?: { completed: number; total: number }
  hideScenes?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  sceneCount: 0,
  progress: () => ({ completed: 0, total: 0 }),
  hideScenes: false,
})

const emit = defineEmits<{
  (e: 'tab-change', tab: string): void
}>()

const router = useRouter()
const route = useRoute()
const uiStore = useUIStore()

// Keyboard shortcut (Ctrl+B)
useSidebarShortcut()

const projectId = computed(() => props.project?.projectId || Number(route.params.id))
const isProjectInfoOpen = ref(false)

interface NavItem {
  key: string
  icon: Component
  label: string
  badge?: number
  to: RouteLocationRaw | null
}

const navItems = computed<NavItem[]>(() => {
  const items: NavItem[] = [
    { key: 'story', icon: BookOpen, label: '스토리', to: null },
    { key: 'scenes', icon: Clapperboard, label: '씬', badge: props.sceneCount, to: null },
    { key: 'objects', icon: User, label: '오브젝트', to: null },
    { key: 'timeline', icon: Layers, label: '전체 타임라인', to: { name: 'timeline', params: { id: projectId.value } } },
    { key: 'settings', icon: Settings, label: '설정', to: null },
  ]
  return props.hideScenes ? items.filter((item) => item.key !== 'scenes') : items
})

const memberBadges = computed(() =>
  (props.project?.members || []).slice(0, 3).map((member) => ({
    initials: member.name?.[0]?.toUpperCase() || '?',
  }))
)
const totalMembers = computed(() => props.project?.members?.length ?? props.project?.memberCount ?? 0)
const extraCount = computed(() => Math.max(totalMembers.value - memberBadges.value.length, 0))


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
      <!-- Header with Toggle (Back Link Removed) -->
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
          <button class="btn-icon-back" @click="router.push('/dashboard')" title="Go to Dashboard">
            <ArrowLeft class="icon-md" />
          </button>
          
          <div class="breadcrumb">
            <RouterLink to="/dashboard">내 프로젝트</RouterLink>
            <span class="separator">/</span>
            <span class="current">{{ project?.title || 'Project' }}</span>
            <button
              type="button"
              class="current-edit-trigger"
              :disabled="!project"
              @click="isProjectInfoOpen = true"
            >
              <Pencil class="icon-sm" />
            </button>
          </div>
        </div>

        <div class="header-actions">
          <div class="progress-section">
            <span class="progress-label">제작 진행도</span>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: `${progressPercentage}%` }"></div>
            </div>
            <span class="progress-text">{{ progress.completed }}/{{ progress.total }}</span>
          </div>

          <div class="member-pill" v-if="totalMembers > 0">
            <span
              v-for="(member, index) in memberBadges"
              :key="`${member.initials}-${index}`"
              class="member-initial"
            >
              {{ member.initials }}
            </span>
            <span v-if="extraCount > 0" class="member-more">+{{ extraCount }}</span>
          </div>
          <ShareButton @click="uiStore.openModal('share-project')" />

          <Button variant="primary" class="btn-preview">
            <Play class="icon-sm" />
            미리보기
          </Button>
        </div>
      </header>

      <!-- Content -->
      <div class="main-content">
        <slot />
      </div>
    </main>
  </div>

  <ShareProjectModal :project-id="projectId" />
  <ProjectInfoDrawer
    :project="project"
    :is-open="isProjectInfoOpen"
    @close="isProjectInfoOpen = false"
  />
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
  background: rgba(255, 255, 255, 0.92);
  border-right: 1px solid var(--rose-100);
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  position: relative;
  z-index: 40;
  backdrop-filter: blur(12px);
}

.sidebar-collapsed {
  width: 72px;
}

/* Mobile Sidebar Behavior */
@media (max-width: 768px) {
  .app-container {
    position: relative;
  }

  .sidebar {
    position: fixed;
    height: 100%;
    /* On mobile, if expanded, it's an overlay. If collapsed, it's hidden or icon bar?
       Let's keep icon bar (collapsed) by default. */
  }
  
  .sidebar-collapsed {
     /* Optional: width: 0 if we want to hide it completely, but navigation is needed. 
        Let's keep 72px for icons, but ensure main content isn't squished? 
        If sidebar is fixed, it doesn't take flex space. 
        So main content will be full width behind it. 
        We need to add margin to main content OR padding-left. 
     */
     /* Actually, if sidebar is fixed, main content starts at edge.
        We need padding-left on main-wrapper equal to sidebar width. 
     */
  }

  .sidebar:not(.sidebar-collapsed) {
    box-shadow: 4px 0 24px rgba(0,0,0,0.15);
  }
}

/* Text elements - smooth fade transition */
.sidebar-text,
.nav-label,
.nav-badge {
  opacity: 1;
  transition: opacity 0.15s ease 0.2s; /* Fade in after sidebar expands */
  white-space: nowrap;
  overflow: hidden;
}

.sidebar-collapsed .sidebar-text,
.sidebar-collapsed .nav-label,
.sidebar-collapsed .nav-badge {
  opacity: 0;
  transition: opacity 0.1s ease; /* Fade out quickly when collapsing */
  pointer-events: none;
}

.sidebar-collapsed .nav-label,
.sidebar-collapsed .nav-badge {
  display: none;
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
  margin: 0 0 0.25rem;
  /* Handle long titles */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Navigation */
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
  flex-shrink: 0;
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


/* Main */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  width: 100%;
  transition: padding-left 0.3s ease;
  position: relative;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.7) 0%, var(--rose-canvas) 35%);
}

.main-wrapper::before {
  content: '';
  position: absolute;
  top: -8%;
  right: -6%;
  width: 36%;
  height: 36%;
  background: radial-gradient(circle, rgba(255, 133, 161, 0.12) 0%, transparent 70%);
  filter: blur(48px);
  z-index: 0;
  pointer-events: none;
}

/* Header */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.5rem;
  min-height: 64px;
  height: auto;
  flex-wrap: wrap;
  row-gap: 0.5rem;
  background: rgba(255, 255, 255, 0.78);
  border-bottom: 1px solid var(--rose-100);
  flex-shrink: 0;
  backdrop-filter: blur(14px);
  box-shadow: 0 8px 24px rgba(255, 133, 161, 0.08);
  position: relative;
  z-index: 2;
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.breadcrumb a {
  color: var(--gray-500);
  text-decoration: none;
  flex-shrink: 0;
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.current-edit-trigger {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--gray-400);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.current-edit-trigger:hover {
  border-color: var(--rose-100);
  background: var(--rose-50);
  color: var(--rose-500);
}

.current-edit-trigger:disabled {
  cursor: default;
  color: var(--gray-300);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
  row-gap: 0.5rem;
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
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.progress-text {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--rose-500);
}

.member-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  border: 1px solid var(--rose-100);
  border-radius: 999px;
  background: var(--rose-50);
  color: var(--rose-600);
  font-size: 0.75rem;
  font-weight: 600;
}

.member-initial {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 0.25rem;
  border-radius: 999px;
  background: white;
  border: 1px solid var(--rose-100);
}

.member-more {
  padding-left: 0.125rem;
}

/* Content */
.main-content {
  flex: 1;
  padding: 2rem;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  z-index: 1;
}

.btn-preview {
  background: linear-gradient(135deg, var(--rose-500), var(--rose-600));
  box-shadow: 0 10px 22px rgba(255, 133, 161, 0.28);
}

.btn-preview:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(255, 133, 161, 0.32);
}

/* Responsive Adjustments */
@media (max-width: 960px) {
    .current { max-width: 100px; }
    .progress-section { display: none; } /* Hide progress bar on smaller screens to save space */
}

@media (max-width: 768px) {
    /* Mobile Sidebar Handling: 
       Sidebar becomes fixed overlay 
       Main wrapper needs margin only if we want to show icons.
       Or we can push content.
       Let's push content for valid icon bar usage.
    */
    
    .sidebar {
        /* Default state is collapsed on mobile usually? */
    }
    .main-wrapper {
        padding-left: 72px; /* Assume collapsed sidebar width always visible */
    }
    
    .sidebar:not(.sidebar-collapsed) {
        width: 100%; /* Full screen overlay or partial? Partial 260px is fine */
        width: 260px;
    }
    
    /* When expanded, it covers content, so we don't increase padding-left */
    
    .header {
        padding: 0 1rem;
    }
    
    .main-content {
        padding: 1rem;
    }
    
    .breadcrumb {
        font-size: 0.8rem;
    }
    
    .header-actions {
        gap: 0.5rem;
    }
    
    .header-actions .btn-secondary, 
    .header-actions .btn-primary {
         /* Maybe hide text on buttons? handled by Button component? */
         padding: 0.5rem;
    }
}
@media (max-width: 480px) {
    .main-wrapper {
        padding-left: 0; /* Fully hide sidebar bar on very small? No, keep it. */
        padding-left: 0;
        padding-bottom: 60px; /* Bottom nav style? No, simpler */
    }
    
    .sidebar {
        position: fixed;
        left: -100%; /* Hide completely */
        transition: transform 0.3s ease;
        z-index: 100;
        width: 80%; /* Drawer style */
        top: 0; bottom: 0;
        left: 0;
        transform: translateX(-100%);
    }
    
    .sidebar.sidebar-collapsed {
        /* When collapsed on mobile, it's actually hidden via transform logic or we rely on explicit visibility state */
        /* If we reuse sidebar-collapsed class for "closed", then width is 72px. That's not what we want. */
        /* On mobile: expanded = drawer open, collapsed = hidden. */
        width: 260px;
        transform: translateX(-100%);
    }

    /* We need a trigger. But the trigger is IN the sidebar. If sidebar is hidden, we can't click trigger.
       So we need a mobile trigger in the Header? Or a bottom nav.
       Reverting to: Sidebar 72px is visible on mobile left. */
       
    .main-wrapper {
       padding-left: 72px; /* Sidebar strip always there */
    }
    .sidebar {
       left: 0;
       transform: none;
    }
    .sidebar.sidebar-collapsed {
       width: 72px;
    }
    .sidebar:not(.sidebar-collapsed) {
       width: 100%; /* Full screen menu on tiny screens? or just 260px shadow */
       width: 260px;
       box-shadow: 4px 0 20px rgba(0,0,0,0.2);
    }
}
</style>
