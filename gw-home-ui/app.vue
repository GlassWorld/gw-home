<script setup lang="ts">
const authStore = useAuthStore()
const route = useRoute()

const shouldShowHeader = computed(() => {
  return authStore.isAuthenticated && route.path !== '/login'
})

const shouldShowFooter = computed(() => {
  return route.path !== '/login'
})
</script>

<template>
  <div class="app-shell">
    <CommonAppHeader v-if="shouldShowHeader" />
    <div class="app-shell__content">
      <NuxtPage />
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
  display: flex;
  flex-direction: column;
}

.app-shell__content {
  display: flex;
  flex: 1;
  min-height: 0;
  align-items: flex-start;
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
