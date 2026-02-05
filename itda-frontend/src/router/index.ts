import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'landing',
    component: () => import('../pages/LandingPage.vue'),
  },
  {
    path: '/auth',
    name: 'auth',
    component: () => import('../pages/AuthPage.vue'),
  },
  {
    path: '/auth/forgot',
    name: 'forgot-password',
    component: () => import('../pages/ForgotPasswordPage.vue'),
  },
  {
    path: '/auth/reset',
    name: 'reset-password',
    component: () => import('../pages/ResetPasswordPage.vue'),
  },
  {
    path: '/access-denied',
    name: 'access-denied',
    component: () => import('../pages/AccessDeniedPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('../pages/DashboardPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/favorites',
    name: 'favorites',
    component: () => import('../pages/FavoritesPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/projects/:id',
    name: 'project-detail',
    component: () => import('../pages/ProjectDetailPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/projects/:projectId/scenes/:sceneId',
    name: 'scene-edit',
    component: () => import('../pages/SceneEditPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/projects/:id/scenes/:sceneId/timeline',
    name: 'scene-timeline',
    component: () => import('../pages/TimelinePage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/projects/:id/timeline',
    name: 'timeline',
    component: () => import('../pages/TimelinePage.vue'),
    meta: { requiresAuth: true },
  },
  // Catch-all 404
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../pages/ProfilePage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/profile/edit',
    name: 'profile-edit',
    component: () => import('../pages/ProfileEditPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/shared',
    name: 'shared',
    component: () => import('../pages/SharedPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/trash',
    name: 'trash',
    component: () => import('../pages/TrashPage.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const getAccessToken = (): string | null => {
  const token = localStorage.getItem('accessToken')
  if (!token || token === 'null' || token === 'undefined') {
    return null
  }
  const parts = token.split('.')
  if (parts.length !== 3) {
    return null
  }
  return token
}

// Navigation guard for auth
router.beforeEach((to, _from, next) => {
  const isAuthenticated = !!getAccessToken()

  if (to.meta.requiresAuth && !isAuthenticated) {
    next({ name: 'auth', query: { redirect: to.fullPath } })
    return
  }

  if (to.name === 'auth' && isAuthenticated) {
    next({ name: 'dashboard' })
    return
  }

  next()
})

export default router
