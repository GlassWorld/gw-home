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
    <NuxtPage class="app-shell__content" />
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
  flex: 1;
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
