const envBase = import.meta.env.VITE_API_BASE_URL as string | undefined

export const API_BASE_URL =
  envBase && envBase.trim()
    ? envBase.replace(/\/$/, '')
    : import.meta.env.DEV
      ? 'http://localhost:8080/api'
      : `${window.location.origin}/api`
