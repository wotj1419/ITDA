<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useUIStore } from '../../stores/ui'
import { useAuthStore } from '../../stores/auth'
import {
  Folder,
  Star,
  Users,
  Trash2,
  Settings,
  ChevronRight,
  ChevronLeft,
} from 'lucide-vue-next'

const route = useRoute()
const uiStore = useUIStore()
const authStore = useAuthStore()

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
    <!-- Logo -->
    <div class="sidebar-header">
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
      <div
        class="user-avatar"
        :style="{ backgroundImage: authStore.user?.profileImage ? `url(${authStore.user.profileImage})` : undefined }"
      >
        <span v-if="!authStore.user?.profileImage">{{ authStore.user?.name?.[0] || 'U' }}</span>
      </div>
      <div class="user-info-text">
        <div class="user-name truncate">{{ authStore.user?.name || 'Guest' }}</div>
        <div class="user-email truncate">{{ authStore.user?.email || '' }}</div>
      </div>
      <button class="btn-icon user-info-text">
        <Settings class="icon-sm" />
      </button>
      <!-- Sidebar Toggle -->
      <button class="sidebar-toggle" @click="uiStore.toggleSidebar">
        <ChevronLeft v-if="uiStore.sidebarExpanded" class="icon-xs" />
        <ChevronRight v-else class="icon-xs" />
      </button>
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
  transition: width 0.3s ease;
  flex-shrink: 0;
}

.sidebar-collapsed {
  width: 72px;
}

.sidebar-collapsed .sidebar-text,
.sidebar-collapsed .nav-label,
.sidebar-collapsed .user-info-text,
.sidebar-collapsed .credit-info {
  display: none;
}

/* Header / Logo */
.sidebar-header {
  padding: 1rem;
  border-bottom: 1px solid var(--rose-100);
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
  color: var(--gray-900);
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--rose-400), var(--rose-500));
  border-radius: 8px;
  flex-shrink: 0;
}

.logo-text {
  font-weight: 700;
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

.icon-xs {
  width: 14px;
  height: 14px;
}

.icon-sm {
  width: 20px;
  height: 20px;
}
</style>
