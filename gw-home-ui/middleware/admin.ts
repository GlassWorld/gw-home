export default defineNuxtRouteMiddleware(async () => {
  const { ensureAuthenticated } = useAuth()
  const authStore = useAuthStore()
  const isAuthenticated = await ensureAuthenticated()

  if (!isAuthenticated) {
    return navigateTo('/login')
  }

  if (authStore.currentUser?.role !== 'ADMIN') {
    return navigateTo('/dashboard')
  }
})
