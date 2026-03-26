<script setup lang="ts">
const authStore = useAuthStore()
const { logout } = useAuth()
const isSubmitting = ref(false)
const isMemoVisible = ref(false)

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

function openMemo() {
  isMemoVisible.value = true
}

function closeMemo() {
  isMemoVisible.value = false
}
</script>

<template>
  <header class="app-header">
    <div class="page-container app-header__inner">
      <NuxtLink class="app-header__brand" to="/dashboard">
        Glass World
      </NuxtLink>

      <nav class="app-header__nav">
        <NuxtLink to="/board">게시글</NuxtLink>
        <NuxtLink to="/vault">Vault</NuxtLink>
      </nav>

      <div class="app-header__actions">
        <NuxtLink to="/settings" class="app-header__profile-link">
          <div class="app-header__profile">
            <strong>{{ authStore.currentUser?.nickname }}</strong>
            <span>{{ authStore.currentUser?.loginId }}</span>
          </div>
        </NuxtLink>

        <button
          type="button"
          class="app-header__icon-button"
          aria-label="메모 열기"
          @click="openMemo"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M7 4.5h10A2.5 2.5 0 0 1 19.5 7v10a2.5 2.5 0 0 1-2.5 2.5H7A2.5 2.5 0 0 1 4.5 17V7A2.5 2.5 0 0 1 7 4.5Z"
              fill="none"
              stroke="currentColor"
              stroke-linejoin="round"
              stroke-width="1.7"
            />
            <path d="M8 9h8" fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.7" />
            <path d="M8 12h8" fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.7" />
            <path d="M8 15h5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.7" />
          </svg>
        </button>

        <button
          type="button"
          class="app-header__icon-button"
          aria-label="로그아웃"
          :disabled="isSubmitting"
          @click="handleLogout"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M10 4.5H7A2.5 2.5 0 0 0 4.5 7v10A2.5 2.5 0 0 0 7 19.5h3"
              fill="none"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.7"
            />
            <path d="M13 8.5 17.5 12 13 15.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" />
            <path d="M9 12h8.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.7" />
          </svg>
        </button>
      </div>
    </div>

    <CommonHeaderMemoModal
      :visible="isMemoVisible"
      @close="closeMemo"
    />
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
  gap: 16px;
  min-height: 48px;
}

.app-header__brand {
  font-size: 1.05rem;
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

.app-header__profile-link {
  padding: 6px 10px;
  border-radius: 14px;
  transition:
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.app-header__profile-link:hover {
  background: rgba(95, 186, 255, 0.08);
  box-shadow: inset 0 -1px 0 rgba(147, 210, 255, 0.32);
}

.app-header__profile span {
  font-size: 0.84rem;
  color: var(--color-text-muted);
}

.app-header__icon-button {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.06);
  color: #d8f1ff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

.app-header__icon-button:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: rgba(147, 210, 255, 0.34);
  background: rgba(95, 186, 255, 0.14);
}

.app-header__icon-button:disabled {
  cursor: wait;
  opacity: 0.6;
}

.app-header__icon-button svg {
  width: 20px;
  height: 20px;
}

@media (max-width: 768px) {
  .app-header__inner {
    flex-wrap: wrap;
    justify-content: center;
    padding-top: 8px;
    padding-bottom: 8px;
  }

  .app-header__profile {
    align-items: center;
  }
}
</style>
