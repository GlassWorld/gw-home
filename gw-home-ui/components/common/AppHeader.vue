<script setup lang="ts">
const authStore = useAuthStore()
const { logout } = useAuth()
const isSubmitting = ref(false)

async function handleLogout() {
  if (isSubmitting.value) {
    return
  }

  isSubmitting.value = true

  try {
    await logout()
    await navigateTo('/login')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <header class="app-header">
    <div class="page-container app-header__inner">
      <NuxtLink class="app-header__brand" to="/dashboard">
        Glass World
      </NuxtLink>

      <nav class="app-header__nav">
        <NuxtLink to="/dashboard">대시보드</NuxtLink>
        <NuxtLink to="/board">게시글</NuxtLink>
        <NuxtLink to="/vault">Vault</NuxtLink>
      </nav>

      <div class="app-header__actions">
        <div class="app-header__profile">
          <strong>{{ authStore.currentUser?.nickname }}</strong>
          <span>{{ authStore.currentUser?.loginId }}</span>
        </div>

        <CommonBaseButton
          variant="secondary"
          :disabled="isSubmitting"
          @click="handleLogout"
        >
          로그아웃
        </CommonBaseButton>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 10;
  backdrop-filter: blur(14px);
  background: rgba(10, 22, 40, 0.85);
  border-bottom: 1px solid rgba(147, 210, 255, 0.12);
}

.app-header__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 64px;
}

.app-header__brand {
  font-size: 1.2rem;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.app-header__nav,
.app-header__actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.app-header__nav a {
  color: var(--color-text-muted);
}

.app-header__profile {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
}

.app-header__profile span {
  font-size: 0.84rem;
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .app-header__inner {
    flex-wrap: wrap;
    justify-content: center;
    padding-top: 10px;
    padding-bottom: 10px;
  }

  .app-header__profile {
    align-items: center;
  }
}
</style>
