<script setup lang="ts">
import { adminNavigationItems, maxFavoriteMenuCount, personalNavigationItems, primaryNavigationItems } from '~/constants/navigation-menu'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const route = useRoute()
const authStore = useAuthStore()
const navigationFavoriteStore = useNavigationFavoriteStore()
const { saveNavigationFavorites } = useNavigationFavoriteApi()
const { showToast } = useToast()

function isActive(path: string): boolean {
  return route.path === path || route.path.startsWith(`${path}/`)
}

function isFavorite(path: string): boolean {
  return navigationFavoriteStore.favoriteMenus.includes(path)
}

async function toggleFavorite(path: string) {
  if (navigationFavoriteStore.isSaving) {
    return
  }

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
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="app-sidebar" @keydown.esc="emit('close')">
      <Transition name="app-sidebar-backdrop" appear>
        <button
          v-if="visible"
          type="button"
          class="app-sidebar__backdrop"
          aria-label="사이드메뉴 닫기"
          @click="emit('close')"
        />
      </Transition>

      <Transition name="app-sidebar-slide" appear>
        <aside v-if="visible" class="app-sidebar__panel">
          <div class="app-sidebar__header">
            <div>
              <p class="app-sidebar__eyebrow">Navigation</p>
              <h2 class="app-sidebar__title">메뉴</h2>
            </div>
            <button type="button" class="app-sidebar__close" aria-label="사이드메뉴 닫기" @click="emit('close')">
              X
            </button>
          </div>

          <nav class="app-sidebar__section">
            <div
              v-for="item in primaryNavigationItems"
              :key="item.to"
              class="app-sidebar__item"
            >
              <NuxtLink
                :to="item.to"
                class="app-sidebar__link"
                :class="{ 'app-sidebar__link--active': isActive(item.to) }"
                @click="emit('close')"
              >
                {{ item.label }}
              </NuxtLink>
              <button
                type="button"
                class="app-sidebar__favorite-button"
                :class="{ 'app-sidebar__favorite-button--active': isFavorite(item.to) }"
                :disabled="navigationFavoriteStore.isSaving"
                :aria-label="isFavorite(item.to) ? `${item.label} 즐겨찾기 해제` : `${item.label} 즐겨찾기 설정`"
                :aria-pressed="isFavorite(item.to)"
                @click.stop="toggleFavorite(item.to)"
              >
                {{ isFavorite(item.to) ? '★' : '☆' }}
              </button>
            </div>
          </nav>

          <section class="app-sidebar__admin">
            <p class="app-sidebar__section-title">개인</p>
            <nav class="app-sidebar__section">
              <div
                v-for="item in personalNavigationItems"
                :key="item.to"
                class="app-sidebar__item"
              >
                <NuxtLink
                  :to="item.to"
                  class="app-sidebar__link"
                  :class="{ 'app-sidebar__link--active': isActive(item.to) }"
                  @click="emit('close')"
                >
                  {{ item.label }}
                </NuxtLink>
                <button
                  type="button"
                  class="app-sidebar__favorite-button"
                  :class="{ 'app-sidebar__favorite-button--active': isFavorite(item.to) }"
                  :disabled="navigationFavoriteStore.isSaving"
                  :aria-label="isFavorite(item.to) ? `${item.label} 즐겨찾기 해제` : `${item.label} 즐겨찾기 설정`"
                  :aria-pressed="isFavorite(item.to)"
                  @click.stop="toggleFavorite(item.to)"
                >
                  {{ isFavorite(item.to) ? '★' : '☆' }}
                </button>
              </div>
            </nav>
          </section>

          <section v-if="authStore.currentUser?.role === 'ADMIN'" class="app-sidebar__admin">
            <p class="app-sidebar__section-title">관리자</p>
            <nav class="app-sidebar__section">
              <div
                v-for="item in adminNavigationItems"
                :key="item.to"
                class="app-sidebar__item"
              >
                <NuxtLink
                  :to="item.to"
                  class="app-sidebar__link"
                  :class="{ 'app-sidebar__link--active': isActive(item.to) }"
                  @click="emit('close')"
                >
                  {{ item.label }}
                </NuxtLink>
                <button
                  type="button"
                  class="app-sidebar__favorite-button"
                  :class="{ 'app-sidebar__favorite-button--active': isFavorite(item.to) }"
                  :disabled="navigationFavoriteStore.isSaving"
                  :aria-label="isFavorite(item.to) ? `${item.label} 즐겨찾기 해제` : `${item.label} 즐겨찾기 설정`"
                  :aria-pressed="isFavorite(item.to)"
                  @click.stop="toggleFavorite(item.to)"
                >
                  {{ isFavorite(item.to) ? '★' : '☆' }}
                </button>
              </div>
            </nav>
          </section>
        </aside>
      </Transition>
    </div>
  </Teleport>
</template>

<style scoped>
.app-sidebar-backdrop-enter-active,
.app-sidebar-backdrop-leave-active {
  transition: opacity 0.2s ease;
}

.app-sidebar-backdrop-enter-from,
.app-sidebar-backdrop-leave-to {
  opacity: 0;
}

.app-sidebar-slide-enter-active,
.app-sidebar-slide-leave-active {
  transition:
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

.app-sidebar-slide-enter-from,
.app-sidebar-slide-leave-to {
  transform: translateX(calc(-100% - 24px));
}

.app-sidebar {
  position: fixed;
  inset: 0;
  z-index: 40;
}

.app-sidebar__backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgba(4, 14, 26, 0.34);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.app-sidebar__panel {
  position: absolute;
  top: 0;
  left: 0;
  width: min(360px, calc(100vw - 24px));
  height: 100%;
  padding: 22px 18px;
  display: grid;
  align-content: start;
  gap: 18px;
  background: linear-gradient(180deg, rgba(8, 18, 32, 0.42) 0%, rgba(11, 28, 48, 0.34) 100%);
  backdrop-filter: blur(26px) saturate(135%);
  -webkit-backdrop-filter: blur(26px) saturate(135%);
  border-right: 1px solid rgba(193, 223, 255, 0.12);
  box-shadow:
    20px 0 48px rgba(0, 0, 0, 0.1),
    inset -1px 0 0 rgba(255, 255, 255, 0.06);
  will-change: transform;
}

.app-sidebar__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.app-sidebar__eyebrow,
.app-sidebar__section-title {
  margin: 0;
  color: var(--color-accent);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.app-sidebar__title {
  margin: 6px 0 0;
  font-size: 1.24rem;
}

.app-sidebar__section {
  display: grid;
  gap: 8px;
}

.app-sidebar__item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.app-sidebar__admin {
  display: grid;
  gap: 10px;
}

.app-sidebar__link {
  padding: 12px 14px;
  border-radius: 8px;
  color: rgba(236, 246, 255, 0.94);
  background: rgba(255, 255, 255, 0.015);
  border: 1px solid rgba(176, 195, 255, 0.05);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.app-sidebar__link:hover,
.app-sidebar__link--active {
  color: #f7fbff;
  border-color: rgba(196, 212, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 6px 14px rgba(8, 22, 58, 0.04);
}

.app-sidebar__close {
  width: 38px;
  height: 38px;
  border: 1px solid rgba(176, 195, 255, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(236, 246, 255, 0.94);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.app-sidebar__favorite-button {
  width: 24px;
  height: 24px;
  padding: 0;
  align-self: center;
  border: 0;
  background: transparent;
  color: rgba(214, 230, 255, 0.42);
  font-size: 1rem;
  line-height: 1;
  transition:
    color 0.18s ease,
    opacity 0.18s ease,
    transform 0.18s ease;
}

.app-sidebar__favorite-button:hover:not(:disabled),
.app-sidebar__favorite-button--active {
  color: #ffd56a;
  opacity: 1;
  transform: scale(1.04);
}

.app-sidebar__favorite-button:disabled {
  cursor: wait;
  opacity: 0.6;
}
</style>
