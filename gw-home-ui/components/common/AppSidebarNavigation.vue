<script setup lang="ts">
import { adminNavigationItems, maxFavoriteMenuCount, personalNavigationItems, primaryNavigationItems } from '~/constants/navigation-menu'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{ close: [] }>()
const route = useRoute()
const authStore = useAuthStore()
const navigationFavoriteStore = useNavigationFavoriteStore()
const { saveNavigationFavorites } = useNavigationFavoriteApi()
const { showToast } = useToast()
const panelElement = ref<HTMLElement | null>(null)
const isMobile = ref(false)
const isDrawerOpen = computed(() => isMobile.value && props.visible)
let mediaQuery: MediaQueryList | undefined
const sections = computed(() => [
  { label: '워크스페이스', items: primaryNavigationItems },
  { label: '개인', items: personalNavigationItems },
  ...(authStore.currentUser?.role === 'ADMIN' ? [{ label: '관리자', items: adminNavigationItems }] : [])
])
const allItems = computed(() => sections.value.flatMap(section => section.items))

// 중첩 경로에서는 가장 구체적인 메뉴 하나만 활성화한다.
function isActive(path: string): boolean {
  return (route.path === path || route.path.startsWith(`${path}/`))
    && !allItems.value.some(item => item.to.length > path.length && (route.path === item.to || route.path.startsWith(`${item.to}/`)))
}

// 현재 즐겨찾기 포함 여부를 반환한다.
function isFavorite(path: string): boolean {
  return navigationFavoriteStore.favoriteMenus.includes(path)
}

// 기존 API와 순서를 유지하며 즐겨찾기를 저장한다.
async function toggleFavorite(path: string) {
  if (navigationFavoriteStore.isSaving) return
  if (!isFavorite(path) && navigationFavoriteStore.favoriteMenus.length >= maxFavoriteMenuCount) {
    showToast(`즐겨찾기 메뉴는 ${maxFavoriteMenuCount}개까지 설정할 수 있습니다.`, { variant: 'error' })
    return
  }
  const nextFavoriteMenus = isFavorite(path)
    ? navigationFavoriteStore.favoriteMenus.filter(favoriteMenu => favoriteMenu !== path)
    : [...navigationFavoriteStore.favoriteMenus, path]
  navigationFavoriteStore.setSaving(true)
  try {
    await saveNavigationFavorites(nextFavoriteMenus)
    navigationFavoriteStore.setFavoriteMenus(nextFavoriteMenus)
  } catch (error) {
    const saveError = error as { data?: { message?: string }; message?: string }
    showToast(saveError.data?.message ?? saveError.message ?? '즐겨찾기 메뉴를 저장하지 못했습니다.', { variant: 'error' })
  } finally {
    navigationFavoriteStore.setSaving(false)
  }
}

// 화면이 넓어지면 모바일 메뉴의 스크롤 잠금도 해제한다.
function updateViewport() {
  isMobile.value = mediaQuery?.matches ?? false
  if (!isMobile.value) emit('close')
}
useOverlayLayer(isDrawerOpen, panelElement, () => emit('close'), 40)
onMounted(() => {
  mediaQuery = window.matchMedia('(max-width: 1099px)')
  updateViewport()
  mediaQuery.addEventListener('change', updateViewport)
})
onBeforeUnmount(() => mediaQuery?.removeEventListener('change', updateViewport))
</script>

<template>
  <Teleport to="body" :disabled="!isMobile">
    <Transition name="drawer" :css="isMobile" @before-leave="disableLeavingElement">
      <div v-if="!isMobile || visible" class="app-sidebar" :class="{ 'app-sidebar--mobile': isMobile }">
        <div v-if="isMobile" class="app-sidebar__backdrop" aria-hidden="true" @click="emit('close')" />
        <aside
          id="workspace-navigation"
          ref="panelElement"
          class="app-sidebar__panel"
          :role="isMobile ? 'dialog' : undefined"
          :aria-modal="isMobile ? true : undefined"
          aria-label="워크스페이스 메뉴"
          tabindex="-1"
        >
          <header class="app-sidebar__header">
            <span class="app-sidebar__eyebrow">WORKSPACE</span>
            <CommonBaseButton v-if="isMobile" class="app-sidebar__close" variant="secondary" aria-label="사이드메뉴 닫기" @click="emit('close')">×</CommonBaseButton>
          </header>
          <div class="app-sidebar__body">
            <nav v-for="section in sections" :key="section.label" class="app-sidebar__section" :aria-label="section.label">
              <p class="app-sidebar__section-title">{{ section.label }}</p>
              <div v-for="item in section.items" :key="item.to" class="app-sidebar__item">
                <NuxtLink :to="item.to" class="app-sidebar__link" :class="{ 'app-sidebar__link--active': isActive(item.to) }" :aria-current="isActive(item.to) ? 'page' : undefined" @click="emit('close')">{{ item.label }}</NuxtLink>
                <button type="button" class="app-sidebar__favorite-button" :class="{ 'app-sidebar__favorite-button--active': isFavorite(item.to) }" :disabled="navigationFavoriteStore.isSaving" :aria-label="isFavorite(item.to) ? `${item.label} 즐겨찾기 해제` : `${item.label} 즐겨찾기 설정`" :aria-pressed="isFavorite(item.to)" @click="toggleFavorite(item.to)">{{ isFavorite(item.to) ? '★' : '☆' }}</button>
              </div>
            </nav>
          </div>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.app-sidebar {
  position: sticky;
  top: var(--header-height);
  flex: 0 0 var(--sidebar-width);
  align-self: flex-start;
}
.app-sidebar__panel {
  display: flex;
  flex-direction: column;
  height: calc(100dvh - var(--header-height));
  padding: var(--panel-padding) 12px;
  border-right: 1px solid var(--color-border);
  background: var(--color-surface);
}
.app-sidebar__header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 0 12px 16px; }
.app-sidebar__eyebrow { color: var(--color-text-muted); font-size: 0.6875rem; font-weight: 700; letter-spacing: 0.12em; }
.app-sidebar__body { min-height: 0; overflow-y: auto; overscroll-behavior: contain; scrollbar-width: thin; }
.app-sidebar__section { display: grid; gap: 4px; margin-bottom: 16px; }
.app-sidebar__section-title { margin: 0; padding: 0 12px 8px; color: var(--color-text-muted); font-size: 0.75rem; font-weight: 600; }
.app-sidebar__item { display: grid; grid-template-columns: minmax(0, 1fr) var(--control-height); align-items: center; gap: 2px; }
.app-sidebar__link { display: flex; align-items: center; min-height: var(--control-height); padding: 6px 10px; border-radius: var(--radius-small); font-size: 0.875rem; overflow-wrap: anywhere; }
.app-sidebar__link:hover { background: var(--color-surface-muted); }
.app-sidebar__link--active { background: var(--color-primary-soft); color: var(--color-primary); font-weight: 700; }
.app-sidebar__favorite-button { width: var(--control-height); height: var(--control-height); padding: 0; border: 0; border-radius: var(--radius-small); background: transparent; color: var(--color-text-muted); font-size: 1.25rem; }
.app-sidebar__favorite-button:hover { background: var(--color-surface-muted); }
.app-sidebar__favorite-button--active { color: var(--color-warning); }
.app-sidebar__favorite-button:disabled { opacity: 0.5; }
.app-sidebar__close { width: var(--button-height); padding: 0; font-size: 1.5rem; }
.app-sidebar--mobile { position: fixed; inset: 0; height: 100vh; height: 100dvh; z-index: 40; }
.app-sidebar__backdrop { position: absolute; inset: 0; background: var(--color-overlay); }
.app-sidebar--mobile .app-sidebar__panel {
  position: absolute;
  inset: 0 auto 0 0;
  width: min(340px, calc(100% - 32px));
  height: 100%;
  height: 100dvh;
  padding-top: max(16px, env(safe-area-inset-top));
  padding-bottom: max(16px, env(safe-area-inset-bottom));
  box-shadow: var(--shadow-raised);
}
@media (max-width: 1099px) {
  .app-sidebar:not(.app-sidebar--mobile) { display: none; }
}
</style>
