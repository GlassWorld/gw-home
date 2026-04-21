<script setup lang="ts">
const authStore = useAuthStore()
const navigationFavoriteStore = useNavigationFavoriteStore()
const route = useRoute()
const { fetchNavigationFavorites } = useNavigationFavoriteApi()
const isNavigationVisible = ref(false)
const isPublicShareRoute = computed(() => route.path.startsWith('/share/'))

const shouldShowHeader = computed(() => {
  return authStore.isAuthenticated && route.path !== '/login' && !isPublicShareRoute.value
})

const shouldShowFooter = computed(() => {
  return route.path !== '/login' && !isPublicShareRoute.value
})

watch(
  () => route.path,
  () => {
    isNavigationVisible.value = false
  }
)

watch(
  () => [authStore.isAuthenticated, authStore.currentUser?.role],
  async ([isAuthenticated]) => {
    if (!isAuthenticated) {
      navigationFavoriteStore.clearFavoriteMenus()
      return
    }

    try {
      navigationFavoriteStore.setFavoriteMenus(await fetchNavigationFavorites())
    } catch {
      navigationFavoriteStore.clearFavoriteMenus()
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="app-shell">
    <CommonAppHeader v-if="shouldShowHeader" @open-navigation="isNavigationVisible = true" />
    <div class="app-shell__workspace">
      <CommonAppSidebarNavigation
        v-if="shouldShowHeader"
        :visible="isNavigationVisible"
        @close="isNavigationVisible = false"
      />
      <div class="app-shell__content">
        <NuxtPage />
      </div>
    </div>
    <CommonToastViewport />
    <CommonDialogProvider />
    <footer v-if="shouldShowFooter" class="app-footer">
      <div class="page-container app-footer__inner">
        <p>Copyright 2026 chjsa11</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-shell__workspace {
  display: flex;
  flex: 1;
  min-height: 0;
  align-items: flex-start;
}

.app-shell__content {
  display: flex;
  flex: 1;
  min-height: 0;
  align-items: flex-start;
  min-width: 0;
}

.app-footer__inner {
  padding-top: 0;
  padding-bottom: 28px;
}

.app-footer p {
  margin: 0;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 0.92rem;
}
</style>
