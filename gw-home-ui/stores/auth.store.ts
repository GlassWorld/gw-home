import type { UserProfile } from '~/types/api/user'

export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref<UserProfile | null>(null)
  const accessToken = ref<string | null>(null)
  const isAuthenticated = computed(() => {
    return currentUser.value !== null && accessToken.value !== null
  })

  function setUser(user: UserProfile | null) {
    currentUser.value = user
  }

  function setToken(token: string | null) {
    accessToken.value = token
  }

  function clearAuth() {
    currentUser.value = null
    accessToken.value = null
  }

  return {
    currentUser,
    accessToken,
    isAuthenticated,
    setUser,
    setToken,
    clearAuth
  }
})
