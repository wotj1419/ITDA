import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest, SignupRequest } from '../types'

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))

  // Getters
  const isAuthenticated = computed(() => !!accessToken.value)

  // Actions
  async function login(credentials: LoginRequest): Promise<void> {
    // Mock login - replace with actual API call
    await new Promise((resolve) => setTimeout(resolve, 500))

    // Mock response
    const mockUser: User = {
      userId: 1,
      email: credentials.email,
      name: 'Minjun Kim',
      profileImage: 'https://i.pravatar.cc/150?u=user123',
    }

    const mockToken = 'mock_access_token_' + Date.now()

    user.value = mockUser
    accessToken.value = mockToken
    localStorage.setItem('accessToken', mockToken)
  }

  async function signup(data: SignupRequest): Promise<void> {
    // Mock signup - replace with actual API call
    await new Promise((resolve) => setTimeout(resolve, 500))

    // Auto-login after signup
    await login({ email: data.email, password: data.password })
  }

  async function fetchMe(): Promise<void> {
    if (!accessToken.value) return

    // Mock fetch - replace with actual API call
    await new Promise((resolve) => setTimeout(resolve, 300))

    user.value = {
      userId: 1,
      email: 'minjun@example.com',
      name: 'Minjun Kim',
      profileImage: 'https://i.pravatar.cc/150?u=user123',
    }
  }

  function logout(): void {
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
