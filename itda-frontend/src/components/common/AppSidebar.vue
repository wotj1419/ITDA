<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useUIStore } from '../../stores/ui'
import { useAuthStore } from '../../stores/auth'
import { useSidebarShortcut } from '../../composables/useSidebarShortcut'
import {
  Folder,
  Star,
  Users,
  Trash2,
  Settings,
  Menu,
} from 'lucide-vue-next'

const route = useRoute()
const uiStore = useUIStore()
const authStore = useAuthStore()

// Keyboard shortcut (Ctrl+B)
useSidebarShortcut()

const navItems = [
  { to: '/dashboard', icon: Folder, label: 'All Projects', tooltip: 'All Projects' },
  { to: '/favorites', icon: Star, label: 'Favorites', tooltip: 'Favorites' },
  { to: '/shared', icon: Users, label: 'Shared with Me', tooltip: 'Shared with Me' },
  { to: '/trash', icon: Trash2, label: 'Trash', tooltip: 'Trash' },
]

const isActive = (path: string) => {
  if (path === '/dashboard') {
    return route.path === '/dashboard' || route.path.startsWith('/project')
  }
  return route.path === path
}

const sidebarClasses = computed(() => [
  'sidebar',
  { 'sidebar-collapsed': !uiStore.sidebarExpanded },
])
</script>

<template>
  <aside :class="sidebarClasses">
    <!-- Header with Toggle & Logo -->
    <div class="sidebar-header">
      <button
        class="menu-btn"
        @click="uiStore.toggleSidebar"
        :title="uiStore.sidebarExpanded ? 'Collapse' : 'Expand'"
      >
        <Menu class="icon-md" />
      </button>

      <RouterLink to="/" class="sidebar-logo">
        <div class="logo-icon"></div>
        <span class="sidebar-text logo-text">AI Movie Studio</span>
      </RouterLink>
    </div>

    <!-- Navigation -->
    <nav class="sidebar-nav">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        :class="['nav-item', { active: isActive(item.to) }]"
        :data-tooltip="item.tooltip"
      >
        <component :is="item.icon" class="nav-icon" />
        <span class="nav-label">{{ item.label }}</span>
      </RouterLink>
    </nav>

    <!-- Credit Info -->
    <div class="sidebar-credit credit-info">
      <div class="credit-card">
        <div class="credit-plan">PRO PLAN</div>
        <div class="credit-amount sidebar-text">Credits: 850 / 1000</div>
        <div class="progress-bar">
          <div class="progress-bar-fill" style="width: 85%"></div>
        </div>
      </div>
    </div>

    <!-- User Info -->
    <div class="sidebar-user">
      <RouterLink to="/profile" class="user-link">
        <div
          class="user-avatar"
          :style="{
            backgroundImage: authStore.user?.profileImage
              ? `url(${authStore.user.profileImage})`
              : undefined,
          }"
        >
          <span v-if="!authStore.user?.profileImage">{{
            authStore.user?.name?.[0] || 'U'
          }}</span>
        </div>
        <div class="user-info-text">
          <div class="user-name">{{ authStore.user?.name || 'Guest' }}</div>
          <div class="user-email">{{ authStore.user?.email || '' }}</div>
        </div>
      </RouterLink>
      <RouterLink to="/profile/edit" class="btn-icon user-info-text settings-btn">
        <Settings class="icon-sm" />
      </RouterLink>
    </div>
  </aside>
</template>

<style scoped>
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
.nav-label,
.user-info-text,
.credit-info {
  opacity: 1;
  transition: opacity 0.15s ease 0.2s; /* Fade in after sidebar expands */
  white-space: nowrap;
  overflow: hidden;
}

.sidebar-collapsed .sidebar-text,
.sidebar-collapsed .nav-label,
.sidebar-collapsed .user-info-text,
.sidebar-collapsed .credit-info {
  opacity: 0;
  transition: opacity 0.1s ease; /* Fade out quickly when collapsing */
  pointer-events: none;
}

/* Header / Logo */
.sidebar-header {
  padding: 1rem;
  border-bottom: 1px solid var(--rose-100);
  display: flex;
  align-items: center;
  gap: 0.75rem;
  height: 64px; /* Fixed height for consistency */
}

.menu-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px; /* Aligns with nav-item icon center */
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

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
  color: var(--gray-900);
  flex: 1;
  overflow: hidden; /* Hide logo when collapsed */
}

.logo-icon {
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, var(--rose-400), var(--rose-500));
  border-radius: 6px;
  flex-shrink: 0;
}

.logo-text {
  font-weight: 700;
  font-size: 1rem;
  white-space: nowrap;
}

/* Hide logo text/icon in collapsed mode if needed, or adjust alignment */
.sidebar-collapsed .logo-text,
.sidebar-collapsed .logo-icon {
  opacity: 0;
  pointer-events: none;
  width: 0;
  margin: 0;
}

.sidebar-collapsed .menu-btn {
  margin: 0;
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
  transition: all 0.2s ease;
  position: relative;
  height: 44px; /* Fixed height */
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
  /* Center icon in collapsed mode handled by flex & padding */
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

/* Credit Info */
.sidebar-credit {
  padding: 1rem;
  border-top: 1px solid var(--rose-100);
}

.credit-card {
  background: linear-gradient(135deg, var(--rose-50), var(--rose-100));
  border-radius: 12px;
  padding: 1rem;
}

.credit-plan {
  font-size: 0.625rem;
  font-weight: 600;
  color: var(--rose-500);
  margin-bottom: 0.25rem;
}

.credit-amount {
  font-size: 0.75rem;
  color: var(--gray-500);
  margin-bottom: 0.5rem;
}

.progress-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 2px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--rose-400), var(--rose-500));
  border-radius: 2px;
}

/* User Info */
.sidebar-user {
  padding: 1rem;
  border-top: 1px solid var(--rose-100);
  display: flex;
  align-items: center;
  gap: 0.75rem;
  position: relative;
  height: 72px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--rose-200);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: var(--rose-600);
  flex-shrink: 0;
}

.user-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0; /* Allow shrinking */
  text-decoration: none;
  color: inherit;
  cursor: pointer;
  z-index: 10;
  margin-right: 0.5rem;
  /* flex: 1 removed to prevent pushing settings button to the end */
}

/* Settings button takes only necessary space */
.settings-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  padding: 0;
  cursor: pointer;
  z-index: 20; /* Higher than user link */
}

.user-info-text {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 0.875rem;
  font-weight: 500;
}

.user-email {
  font-size: 0.75rem;
  color: var(--gray-500);
}

.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-icon {
  padding: 0.5rem;
  border: none;
  background: transparent;
  color: var(--gray-500);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-icon:hover {
  background: var(--gray-100);
}

.icon-xs {
  width: 16px;
  height: 16px;
}

.icon-sm {
  width: 20px;
  height: 20px;
}

.icon-md {
  width: 24px;
  height: 24px;
}
</style>
