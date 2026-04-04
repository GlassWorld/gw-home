<script setup lang="ts">
import { allNavigationItems, maxHeaderFavoriteMenuCount } from '~/constants/navigation-menu'

const authStore = useAuthStore()
const navigationFavoriteStore = useNavigationFavoriteStore()
const { logout } = useAuth()
const isSubmitting = ref(false)
const isMemoVisible = ref(false)

const favoriteNavigationItems = computed(() => {
  const isAdmin = authStore.currentUser?.role === 'ADMIN'

  return allNavigationItems
    .filter(item => navigationFavoriteStore.favoriteMenus.includes(item.to))
    .filter(item => !item.adminOnly || isAdmin)
    .slice(0, maxHeaderFavoriteMenuCount)
})

const emit = defineEmits<{
  openNavigation: []
}>()

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
      <div class="app-header__start">
        <button
          type="button"
          class="app-header__menu-button"
          aria-label="사이드메뉴 열기"
          @click="emit('openNavigation')"
        >
          <span />
          <span />
          <span />
        </button>

        <NuxtLink class="app-header__brand" to="/dashboard">
          Glass World
        </NuxtLink>
      </div>

      <nav v-if="favoriteNavigationItems.length" class="app-header__favorite-navigation" aria-label="즐겨찾기 메뉴">
        <NuxtLink
          v-for="item in favoriteNavigationItems"
          :key="item.to"
          :to="item.to"
          class="app-header__favorite-link"
        >
          {{ item.label }}
        </NuxtLink>
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
          <IconsIconMemo />
        </button>

        <button
          type="button"
          class="app-header__icon-button"
          aria-label="로그아웃"
          :disabled="isSubmitting"
          @click="handleLogout"
        >
          <IconsIconLogout />
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
  backdrop-filter: blur(18px) saturate(120%);
  -webkit-backdrop-filter: blur(18px) saturate(120%);
  background: linear-gradient(180deg, rgba(8, 20, 36, 0.58) 0%, rgba(8, 20, 36, 0.42) 100%);
  border-bottom: 1px solid rgba(147, 210, 255, 0.08);
  box-shadow: 0 8px 24px rgba(5, 14, 28, 0.12);
}

.app-header__inner {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
  min-height: 48px;
}

.app-header__brand {
  font-size: 1.05rem;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.app-header__start,
.app-header__actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.app-header__menu-button {
  width: 40px;
  height: 40px;
  padding: 0;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px solid rgba(176, 195, 255, 0.26);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(42, 54, 98, 0.92) 0%, rgba(25, 35, 68, 0.9) 100%);
  color: rgba(236, 246, 255, 0.94);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.14),
    inset 0 -8px 14px rgba(56, 79, 162, 0.2),
    0 14px 28px rgba(6, 20, 54, 0.28);
  transition: box-shadow 0.18s ease, background-color 0.18s ease, border-color 0.18s ease;
}

.app-header__menu-button:hover {
  border-color: rgba(196, 212, 255, 0.34);
  background: linear-gradient(180deg, rgba(56, 70, 120, 0.96) 0%, rgba(34, 47, 86, 0.94) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    inset 0 -8px 14px rgba(66, 91, 180, 0.24),
    0 16px 30px rgba(8, 22, 58, 0.32);
}

.app-header__menu-button span {
  width: 16px;
  height: 2px;
  border-radius: 999px;
  background: currentColor;
}

.app-header__profile {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
}

.app-header__profile-link {
  padding: 6px 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  transition:
    background 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.app-header__profile-link:hover {
  border-color: rgba(176, 195, 255, 0.18);
  background: linear-gradient(180deg, rgba(30, 41, 78, 0.78) 0%, rgba(20, 29, 56, 0.76) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.app-header__favorite-navigation {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  min-width: 0;
}

.app-header__favorite-link {
  color: var(--color-text-muted);
  transition:
    transform 0.18s ease,
    color 0.18s ease;
}

.app-header__favorite-link:hover {
  transform: translateY(-1px);
  color: #f7fbff;
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
  border: 1px solid rgba(176, 195, 255, 0.26);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(42, 54, 98, 0.92) 0%, rgba(25, 35, 68, 0.9) 100%);
  color: rgba(236, 246, 255, 0.94);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.14),
    inset 0 -8px 14px rgba(56, 79, 162, 0.2),
    0 14px 28px rgba(6, 20, 54, 0.28);
  transition:
    box-shadow 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

.app-header__icon-button:hover:not(:disabled) {
  border-color: rgba(196, 212, 255, 0.34);
  background: linear-gradient(180deg, rgba(56, 70, 120, 0.96) 0%, rgba(34, 47, 86, 0.94) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    inset 0 -8px 14px rgba(66, 91, 180, 0.24),
    0 16px 30px rgba(8, 22, 58, 0.32);
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
    gap: 12px;
    padding-top: 8px;
    padding-bottom: 8px;
    grid-template-columns: auto auto;
  }

  .app-header__start,
  .app-header__actions {
    min-width: 0;
  }

  .app-header__profile {
    align-items: center;
  }

  .app-header__favorite-navigation {
    display: none;
  }
}
</style>
