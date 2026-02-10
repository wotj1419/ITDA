<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useUIStore } from '../../stores/ui'
import { useAuthStore } from '../../stores/auth'
import { useSidebarShortcut } from '../../composables/useSidebarShortcut'
import { resolveApiUrl } from '../../services/api/urls'
import {
  Folder,
  Star,
  Users,
  Trash2,
  LogOut,
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const uiStore = useUIStore()
const authStore = useAuthStore()

// Keyboard shortcut (Ctrl+B)
useSidebarShortcut()

const navItems = [
  { to: '/dashboard', icon: Folder, label: '모든 프로젝트', tooltip: '모든 프로젝트' },
  { to: '/favorites', icon: Star, label: '즐겨찾기', tooltip: '즐겨찾기' },
  { to: '/shared', icon: Users, label: '공유받은 프로젝트', tooltip: '공유받은 프로젝트' },
  { to: '/trash', icon: Trash2, label: '휴지통', tooltip: '휴지통' },
]

const showProfileMenu = ref(false)
const profileMenuRef = ref<HTMLElement | null>(null)

// 귀여운 동물 이모지 목록
const AVATAR_EMOJIS = [
  '🐱', '🐶', '🐰', '🦊', '🐻', '🐼', '🐨', '🦁',
  '🐯', '🐮', '🐷', '🐸', '🐵', '🐔', '🐧', '🦄',
  '🐹', '🐝', '🦋', '🐢', '🐙', '🦀', '🐳', '🦩',
]

// userId 기반으로 이모지 선택
function getEmoji(userId?: number, name?: string): string {
  if (userId !== undefined && userId > 0) {
    const index = (userId - 1) % AVATAR_EMOJIS.length
    return AVATAR_EMOJIS[index] ?? '🐱'
  }
  if (!name) return AVATAR_EMOJIS[0] ?? '🐱'
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = ((hash << 5) - hash) + name.charCodeAt(i)
    hash = hash & hash
  }
  const index = Math.abs(hash) % AVATAR_EMOJIS.length
  return AVATAR_EMOJIS[index] ?? '🐱'
}

const userEmoji = computed(() => getEmoji(authStore.user?.id, authStore.user?.name))
const resolvedProfileImageUrl = computed(() => resolveApiUrl(authStore.user?.profileImageUrl))

const toggleProfileMenu = () => {
  showProfileMenu.value = !showProfileMenu.value
}

const closeProfileMenu = (e: MouseEvent) => {
  if (profileMenuRef.value && !profileMenuRef.value.contains(e.target as Node)) {
    showProfileMenu.value = false
  }
}

const handleLogout = () => {
  authStore.logout()
  router.push('/')
}

onMounted(() => {
  document.addEventListener('click', closeProfileMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeProfileMenu)
})

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
        :class="{ 'menu-btn--open': uiStore.sidebarExpanded }"
        @click="uiStore.toggleSidebar"
      >
        <span class="toggle" aria-hidden="true">
          <span class="bars bar1"></span>
          <span class="bars bar2"></span>
          <span class="bars bar3"></span>
        </span>
      </button>
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

    <!-- User Info & Dropdown -->
    <div class="sidebar-user" ref="profileMenuRef">
      <div class="user-trigger" @click.stop="toggleProfileMenu">
        <div
          class="user-avatar"
          :style="{
            backgroundImage: resolvedProfileImageUrl
              ? `url(${resolvedProfileImageUrl})`
              : undefined,
          }"
        >
          <span v-if="!resolvedProfileImageUrl" class="avatar-emoji">{{ userEmoji }}</span>
        </div>
        <div class="user-info-text">
          <div class="user-name">{{ authStore.user?.name || 'Guest' }}</div>
          <div class="user-email">{{ authStore.user?.email || '' }}</div>
        </div>
      </div>

      <!-- Dropdown Menu -->
      <transition name="fade">
        <div v-if="showProfileMenu" class="profile-menu">
           <!-- Profile Header -->
           <RouterLink to="/profile" class="menu-header" @click="showProfileMenu = false">
             <div
                class="user-avatar header-avatar"
                :style="{
                  backgroundImage: resolvedProfileImageUrl
                    ? `url(${resolvedProfileImageUrl})`
                    : undefined,
                }"
              >
                <span v-if="!resolvedProfileImageUrl" class="avatar-emoji">{{ userEmoji }}</span>
              </div>
              <div class="user-info-text">
                <div class="user-name">{{ authStore.user?.name || 'Guest' }}</div>
                <div class="user-email">{{ authStore.user?.email || '' }}</div>
              </div>
           </RouterLink>
           
           <div class="menu-divider"></div>
           
           <div class="menu-group">
             <button class="menu-item text-danger" @click="handleLogout">
               <LogOut class="icon-sm" />
                <span>로그아웃</span>
             </button>
           </div>
        </div>
      </transition>
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
  z-index: 50;
}

.sidebar-collapsed {
  width: 72px;
}

/* Text elements - smooth fade transition */
.sidebar-text,
.nav-label,
.user-info-text {
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

/* Hide user info in the sidebar trigger, BUT keep it visible in the popup menu */
.sidebar-collapsed .user-trigger .user-info-text {
  opacity: 0;
  pointer-events: none;
  display: none; /* remove from flow to center avatar */
}

/* Ensure text inside the popup menu remains visible */
.sidebar-collapsed .profile-menu .user-info-text {
  opacity: 1;
  pointer-events: auto;
  display: block;
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

/* User Info */
.sidebar-user {
  padding: 1rem;
  border-top: 1px solid var(--rose-100);
  position: relative;
  /* Keep original layout context, but removed fixed height so menu can overflow if needed */
}

/* New: Trigger area styles */
.user-trigger {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  padding: 0.5rem;
  margin: -0.5rem;
  border-radius: 8px;
  transition: background 0.2s;
  width: 100%; /* Take full width */
}

.user-trigger:hover {
  background: var(--gray-50);
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

.avatar-emoji {
  font-size: 1.2rem;
  line-height: 1;
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

.icon-sm {
  width: 20px;
  height: 20px;
}

.icon-md {
  width: 24px;
  height: 24px;
}

/* Dropdown Menu Styles */
.profile-menu {
  position: absolute;
  bottom: 100%;
  left: 1rem;
  right: 1rem;
  margin-bottom: 0.5rem;
  background: white;
  border: 1px solid var(--gray-200);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 0.5rem;
  z-index: 100;
  min-width: 220px;
}

.sidebar-collapsed .profile-menu {
  left: 100%;
  bottom: 0;
  margin-left: 0.5rem;
  margin-bottom: 0;
}

.menu-group {
  display: flex;
  flex-direction: column;
}

.menu-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background-color: var(--gray-50);
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
  margin-bottom: 0.5rem;
  transition: background-color 0.2s;
}

.menu-header:hover {
  background-color: var(--gray-100);
}

.header-avatar {
  width: 32px; /* Slightly smaller in menu */
  height: 32px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border: none;
  background: transparent;
  width: 100%;
  text-align: left;
  font-size: 0.875rem;
  color: var(--gray-700);
  cursor: pointer;
  border-radius: 6px;
  text-decoration: none;
  transition: all 0.2s;
}

.menu-item:hover {
  background-color: var(--gray-100);
  color: var(--gray-900);
}

.menu-divider {
  height: 1px;
  background-color: var(--gray-100);
  margin: 0.5rem 0;
}

.text-danger {
  color: #ef4444;
}

.text-danger:hover {
  background-color: #fef2f2;
  color: #dc2626;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
