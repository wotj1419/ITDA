import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest, SignupRequest } from '../types'
import { authService } from '../services'

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))

  // Getters
  const isAuthenticated = computed(() => !!accessToken.value)

  // Actions
  async function login(credentials: LoginRequest): Promise<void> {
    const { user: userData, token } = await authService.login(credentials)

    user.value = userData
    accessToken.value = token
    localStorage.setItem('accessToken', token)
  }

  async function signup(data: SignupRequest): Promise<void> {
    await authService.signup(data)
    // Auto-login removed: User must login manually
  }

  async function fetchMe(): Promise<void> {
    if (!accessToken.value) return

    try {
      const userData = await authService.fetchMe()
      user.value = userData
    } catch (error) {
      console.error('Failed to fetch user', error)
      logout()
    }
  }

  function logout(): void {
    authService.logout()
    user.value = null
    accessToken.value = null
    localStorage.removeItem('accessToken')
  }

  // Initialize: fetch user if token exists
  if (accessToken.value) {
    fetchMe()
  }

  return {
    user,
    accessToken,
    isAuthenticated,
    login,
    signup,
    fetchMe,
    logout,
  }
})
