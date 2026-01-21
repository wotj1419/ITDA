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

// Navigation guard for auth
router.beforeEach((to, _from, next) => {
  const isAuthenticated = localStorage.getItem('accessToken')

  if (to.meta.requiresAuth && !isAuthenticated) {
    next({ name: 'auth' })
  } else {
    next()
  }
})

export default router
