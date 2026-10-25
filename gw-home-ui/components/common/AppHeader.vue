<script setup lang="ts">
import { allNavigationItems, maxHeaderFavoriteMenuCount } from '~/constants/navigation-menu'

const props = withDefaults(defineProps<{ navigationVisible?: boolean }>(), { navigationVisible: false })
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

// 기존 로그아웃 흐름을 유지하고 중복 요청을 막는다.
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

// 개인 메모 창을 연다.
function openMemo() {
  isMemoVisible.value = true
}

// 개인 메모 창을 닫는다.
function closeMemo() {
  isMemoVisible.value = false
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__inner">
      <div class="app-header__start">
        <button type="button" class="app-header__menu-button" aria-label="사이드메뉴 열기" aria-controls="workspace-navigation" :aria-expanded="props.navigationVisible" @click="emit('openNavigation')">
          <span /><span /><span />
        </button>
        <NuxtLink class="app-header__brand" to="/dashboard">
          <span class="app-header__brand-mark" aria-hidden="true">G</span>
          Glass World
        </NuxtLink>
      </div>
      <nav v-if="favoriteNavigationItems.length" class="app-header__favorite-navigation" aria-label="즐겨찾기 메뉴">
        <NuxtLink v-for="item in favoriteNavigationItems" :key="item.to" :to="item.to" class="app-header__favorite-link">{{ item.label }}</NuxtLink>
      </nav>
      <div class="app-header__actions">
        <NuxtLink to="/settings" class="app-header__profile-link">
          <strong>{{ authStore.currentUser?.nickname || authStore.currentUser?.loginId }}</strong>
          <span>{{ authStore.currentUser?.loginId }}</span>
        </NuxtLink>
        <button type="button" class="app-header__icon-button" aria-label="메모 열기" @click="openMemo"><IconsIconMemo /></button>
        <button type="button" class="app-header__icon-button" aria-label="로그아웃" :disabled="isSubmitting" @click="handleLogout"><IconsIconLogout /></button>
      </div>
    </div>
    <CommonHeaderMemoModal :visible="isMemoVisible" @close="closeMemo" />
  </header>
</template>

<style scoped>
.app-header { position: sticky; top: 0; z-index: 20; border-bottom: 1px solid var(--color-border); background: var(--color-surface); }
.app-header__inner { display: flex; align-items: center; gap: var(--section-gap); min-height: var(--header-height); padding-inline: var(--page-gutter); }
.app-header__start, .app-header__actions { display: flex; align-items: center; gap: 12px; min-width: 0; }
.app-header__actions { margin-left: auto; }
.app-header__brand { display: inline-flex; align-items: center; gap: 10px; min-height: 44px; font-size: 1.0625rem; font-weight: 750; letter-spacing: -0.04em; white-space: nowrap; }
.app-header__brand-mark { display: grid; place-items: center; width: 32px; height: 32px; border-radius: 10px; background: var(--color-primary); color: var(--color-on-primary); font-size: 1.125rem; }
.app-header__menu-button, .app-header__icon-button {
  display: inline-flex; flex: none; align-items: center; justify-content: center;
  width: var(--button-height); height: var(--button-height); padding: 0; border: 1px solid var(--color-border);
  border-radius: var(--radius-small); color: var(--color-text-muted); background: var(--color-surface);
}
.app-header__menu-button { flex-direction: column; gap: 4px; }
.app-header__menu-button span { width: 16px; height: 2px; border-radius: 2px; background: currentColor; }
.app-header__icon-button svg { width: 20px; height: 20px; }
.app-header__menu-button:hover, .app-header__icon-button:hover { background: var(--color-surface-muted); color: var(--color-primary); }
.app-header__icon-button:disabled { opacity: 0.5; }
.app-header__profile-link { display: grid; min-width: 0; max-width: 170px; min-height: 44px; align-content: center; padding-inline: 8px; line-height: 1.4; }
.app-header__profile-link strong, .app-header__profile-link span { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.app-header__profile-link strong { font-size: 0.8125rem; }
.app-header__profile-link span { color: var(--color-text-muted); font-size: 0.75rem; }
.app-header__favorite-navigation { display: flex; flex: 1; gap: 4px; min-width: 0; overflow-x: auto; scrollbar-width: thin; }
.app-header__favorite-link { flex: none; display: flex; align-items: center; min-height: 44px; padding: 8px 12px; border-radius: var(--radius-small); color: var(--color-text-muted); font-size: 0.8125rem; }
.app-header__favorite-link:hover, .app-header__favorite-link.router-link-exact-active { background: var(--color-primary-soft); color: var(--color-primary); }
@media (min-width: 1100px) { .app-header__menu-button { display: none; } }
@media (max-width: 1099px) { .app-header__favorite-navigation { display: none; } }
@media (max-width: 767px) {
  .app-header__inner { gap: 12px; }
  .app-header__start, .app-header__actions { gap: 6px; }
  .app-header__brand { font-size: 0.9375rem; }
  .app-header__brand-mark, .app-header__profile-link { display: none; }
}
</style>
