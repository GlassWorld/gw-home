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

// 서버 렌더링과 첫 화면의 메뉴 상태를 일치시키고 계정 변경 후 늦은 응답을 버린다.
onMounted(() => {
  watch(
    () => [authStore.isAuthenticated, authStore.currentUser?.role, authStore.currentUser?.loginId],
    async ([isAuthenticated], _previous, onCleanup) => {
      let isCurrent = true
      onCleanup(() => { isCurrent = false })
      if (!isAuthenticated) {
        navigationFavoriteStore.clearFavoriteMenus()
        return
      }
      try {
        const favorites = await fetchNavigationFavorites()
        if (isCurrent) navigationFavoriteStore.setFavoriteMenus(favorites)
      } catch {
        if (isCurrent) navigationFavoriteStore.clearFavoriteMenus()
      }
    },
    { immediate: true }
  )
})
</script>

<template>
  <div class="app-shell">
    <a class="skip-link" href="#main-content">본문으로 이동</a>
    <CommonAppHeader v-if="shouldShowHeader" :navigation-visible="isNavigationVisible" @open-navigation="isNavigationVisible = true" />
    <div class="app-shell__workspace">
      <CommonAppSidebarNavigation
        v-if="shouldShowHeader"
        :visible="isNavigationVisible"
        @close="isNavigationVisible = false"
      />
      <div id="main-content" class="app-shell__content" tabindex="-1">
        <NuxtPage :transition="{ name: 'page', mode: 'out-in', onBeforeLeave: disableLeavingElement }" />
      </div>
    </div>
    <CommonToastViewport />
    <CommonDialogProvider />
    <footer v-if="shouldShowFooter" class="app-footer">
      <p>Copyright 2026 chjsa11</p>
    </footer>
  </div>
</template>
