<script setup lang="ts">
const authStore = useAuthStore()
const route = useRoute()

const shouldShowHeader = computed(() => {
  return authStore.isAuthenticated && route.path !== '/login'
})
</script>

<template>
  <div class="app-shell">
    <CommonAppHeader v-if="shouldShowHeader" />
    <NuxtPage />
  </div>
</template>
