<script setup lang="ts">
import { adminNavigationItems, personalNavigationItems, primaryNavigationItems } from '~/constants/navigation-menu'

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
      <button type="button" class="app-sidebar__backdrop" aria-label="사이드메뉴 닫기" @click="emit('close')" />

      <aside class="app-sidebar__panel">
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
            <button
              type="button"
              class="app-sidebar__favorite-button"
              :class="{ 'app-sidebar__favorite-button--active': isFavorite(item.to) }"
              :disabled="navigationFavoriteStore.isSaving"
              :aria-label="isFavorite(item.to) ? `${item.label} 즐겨찾기 해제` : `${item.label} 즐겨찾기 설정`"
              @click.stop="toggleFavorite(item.to)"
            >
              {{ isFavorite(item.to) ? '★' : '☆' }}
            </button>
            <NuxtLink
              :to="item.to"
              class="app-sidebar__link"
              :class="{ 'app-sidebar__link--active': isActive(item.to) }"
              @click="emit('close')"
            >
              {{ item.label }}
            </NuxtLink>
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
              <button
                type="button"
                class="app-sidebar__favorite-button"
                :class="{ 'app-sidebar__favorite-button--active': isFavorite(item.to) }"
                :disabled="navigationFavoriteStore.isSaving"
                :aria-label="isFavorite(item.to) ? `${item.label} 즐겨찾기 해제` : `${item.label} 즐겨찾기 설정`"
                @click.stop="toggleFavorite(item.to)"
              >
                {{ isFavorite(item.to) ? '★' : '☆' }}
              </button>
              <NuxtLink
                :to="item.to"
                class="app-sidebar__link"
                :class="{ 'app-sidebar__link--active': isActive(item.to) }"
                @click="emit('close')"
              >
                {{ item.label }}
              </NuxtLink>
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
              <button
                type="button"
                class="app-sidebar__favorite-button"
                :class="{ 'app-sidebar__favorite-button--active': isFavorite(item.to) }"
                :disabled="navigationFavoriteStore.isSaving"
                :aria-label="isFavorite(item.to) ? `${item.label} 즐겨찾기 해제` : `${item.label} 즐겨찾기 설정`"
                @click.stop="toggleFavorite(item.to)"
              >
                {{ isFavorite(item.to) ? '★' : '☆' }}
              </button>
              <NuxtLink
                :to="item.to"
                class="app-sidebar__link"
                :class="{ 'app-sidebar__link--active': isActive(item.to) }"
                @click="emit('close')"
              >
                {{ item.label }}
              </NuxtLink>
            </div>
          </nav>
        </section>
      </aside>
    </div>
  </Teleport>
</template>

<style scoped>
.app-sidebar {
  position: fixed;
  inset: 0;
  z-index: 40;
}

.app-sidebar__backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgba(4, 14, 26, 0.52);
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
  background:
    linear-gradient(180deg, rgba(5, 21, 38, 0.98), rgba(8, 29, 51, 0.96)),
    rgba(5, 21, 38, 0.96);
  border-right: 1px solid rgba(147, 210, 255, 0.16);
  box-shadow: 20px 0 48px rgba(0, 0, 0, 0.24);
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
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  align-items: stretch;
}

.app-sidebar__admin {
  display: grid;
  gap: 10px;
}

.app-sidebar__link {
  padding: 12px 14px;
  border-radius: 14px;
  color: var(--color-text-muted);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid transparent;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    transform 0.18s ease;
}

.app-sidebar__link:hover,
.app-sidebar__link--active {
  color: #f7fbff;
  border-color: rgba(147, 210, 255, 0.24);
  background: rgba(95, 186, 255, 0.12);
  transform: translateX(2px);
}

.app-sidebar__close {
  width: 38px;
  height: 38px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  color: #d8f1ff;
}

.app-sidebar__favorite-button {
  width: 42px;
  border: 1px solid rgba(147, 210, 255, 0.16);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(216, 241, 255, 0.72);
  font-size: 1.1rem;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease,
    color 0.18s ease;
}

.app-sidebar__favorite-button:hover:not(:disabled),
.app-sidebar__favorite-button--active {
  transform: translateY(-1px);
  border-color: rgba(255, 210, 120, 0.3);
  background: rgba(255, 210, 120, 0.12);
  color: #ffd56a;
}

.app-sidebar__favorite-button:disabled {
  cursor: wait;
  opacity: 0.6;
}
</style>
