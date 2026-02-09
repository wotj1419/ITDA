import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest, SignupRequest, UpdateProfileRequest } from '../types/api/auth'
import { authService } from '../services'

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))

  // Getters
  const isAuthenticated = computed(() => !!accessToken.value)

  // Actions
  async function login(credentials: LoginRequest): Promise<void> {
    const { accessToken: access, refreshToken: refresh } = await authService.login(credentials);

    accessToken.value = access;
    refreshToken.value = refresh;
    localStorage.setItem('accessToken', access);
    localStorage.setItem('refreshToken', refresh);
    await fetchMe();
  }

  async function signup(data: SignupRequest): Promise<void> {
    await authService.signup(data);
    // Auto-login removed: User must login manually
  }

  async function fetchMe(): Promise<void> {
    if (!accessToken.value) return;

    try {
      const userData = await authService.fetchMe();
      user.value = userData;
    } catch (error) {
      console.error('Failed to fetch user', error);
      logout();
    }
  }

  async function updateProfile(data: UpdateProfileRequest): Promise<User> {
    if (!accessToken.value) {
      throw new Error('Not authenticated');
    }
    const updated = await authService.updateProfile(data);
    user.value = updated;
    return updated;
  }

  function logout(): void {
    user.value = null
    accessToken.value = null
    refreshToken.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  // Initialize: fetch user if token exists
  if (accessToken.value) {
    fetchMe()
  }

  return {
    user,
    accessToken,
    refreshToken,
    isAuthenticated,
    login,
    signup,
    fetchMe,
    updateProfile,
    logout,
  }
})
