export default defineNuxtRouteMiddleware(async () => {
  const { ensureAuthenticated } = useAuth()
  const isAuthenticated = await ensureAuthenticated()

  if (!isAuthenticated) {
    return navigateTo('/login')
  }
})
