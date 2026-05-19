<script setup lang="ts">
import { useVaultPage } from '~/features/vault/composables/use-vault-page'

definePageMeta({
  middleware: 'auth'
})

type CredentialViewMode = 'card' | 'list'

const {
  authStore,
  credentialList,
  categoryList,
  selectedCredential,
  isLoading,
  isFormVisible,
  isDetailVisible,
  keyword,
  selectedCategoryUuids,
  loadCategoryList,
  openDetail,
  openCreateModal,
  openEditModal,
  handleSearch,
  handleCategorySelect,
  clearCategoryFilter,
  handleSaved,
  handleDeleted,
  copyCredential
} = useVaultPage()

const credentialViewMode = ref<CredentialViewMode>('card')

function getCategoryStyle(categoryColor: string | null) {
  if (!categoryColor) {
    return {
      background: 'rgba(110, 193, 255, 0.12)',
      color: 'var(--color-accent)'
    }
  }

  return {
    background: `${categoryColor}22`,
    color: categoryColor
  }
}

function getMemoPreview(memo: string | null) {
  return memo?.trim() || '메모 없음'
}

function toggleCredentialViewMode() {
  credentialViewMode.value = credentialViewMode.value === 'card' ? 'list' : 'card'
}

await loadCategoryList()
</script>

<template>
  <main class="page-container vault-page">
    <section class="content-panel vault-page__hero page-panel-padding-md">
      <div class="vault-page__hero-header">
        <div>
          <p class="vault-page__eyebrow">Vault</p>
          <h1 class="section-title">개인 자격증명 관리</h1>
          <p class="section-description">
            자주 쓰는 로그인 정보와 메모를 모아두고, 필요할 때 빠르게 복사할 수 있습니다.
          </p>
        </div>

        <div class="vault-page__hero-actions page-actions">
          <CommonBaseButton @click="openCreateModal">
            신규 등록
          </CommonBaseButton>
          <CommonBaseButton
            v-if="authStore.currentUser?.role === 'ADMIN'"
            to="/admin/vault-categories"
            variant="secondary"
          >
            카테고리 관리
          </CommonBaseButton>
        </div>
      </div>

      <form class="vault-page__search-form" @submit.prevent="handleSearch">
        <input
          v-model="keyword"
          class="input-field"
          type="search"
          placeholder="제목, 아이디, 메모를 여러 단어로 검색"
        >
        <CommonBaseButton variant="secondary" type="submit">
          검색
        </CommonBaseButton>
      </form>

      <div v-if="categoryList.length" class="vault-page__category-filter">
        <CommonBaseButton
          :variant="selectedCategoryUuids.length ? 'secondary' : 'primary'"
          size="small"
          @click="clearCategoryFilter"
        >
          전체
        </CommonBaseButton>
        <CommonBaseButton
          v-for="category in categoryList"
          :key="category.categoryUuid"
          :variant="selectedCategoryUuids.includes(category.categoryUuid) ? 'primary' : 'secondary'"
          size="small"
          @click="handleCategorySelect(category.categoryUuid)"
        >
          {{ category.name }}
        </CommonBaseButton>
      </div>
    </section>

    <section class="vault-page__content">
      <p v-if="isLoading" class="message-muted">
        자격증명 정보를 불러오는 중입니다.
      </p>
      <template v-else-if="credentialList.length">
        <div class="vault-page__result-toolbar">
          <p class="vault-page__result-count">총 {{ credentialList.length }}개</p>
          <CommonBaseButton
            variant="secondary"
            size="small"
            :aria-label="credentialViewMode === 'card' ? '목록형 보기로 전환' : '카드형 보기로 전환'"
            @click="toggleCredentialViewMode"
          >
            {{ credentialViewMode === 'card' ? '목록으로 보기' : '카드로 보기' }}
          </CommonBaseButton>
        </div>

        <div v-if="credentialViewMode === 'card'" class="vault-page__grid">
          <VaultCredentialCard
            v-for="credential in credentialList"
            :key="credential.credentialUuid"
            :credential="credential"
            @copy="copyCredential"
            @detail="openDetail"
          />
        </div>

        <div v-else class="vault-page__list content-panel">
          <article
            v-for="credential in credentialList"
            :key="credential.credentialUuid"
            class="vault-page__list-row"
          >
            <div class="vault-page__list-main">
              <h2>{{ credential.title }}</h2>
              <p>{{ getMemoPreview(credential.memo) }}</p>
            </div>
            <p class="vault-page__list-login">
              {{ credential.loginId || '아이디 없음' }}
            </p>
            <div class="vault-page__list-category-list">
              <span
                v-for="category in credential.categories"
                :key="category.categoryUuid"
                class="vault-page__list-category"
                :style="getCategoryStyle(category.color)"
              >
                {{ category.name }}
              </span>
              <span v-if="credential.categories.length === 0" class="vault-page__list-empty-category">
                카테고리 없음
              </span>
            </div>
            <div class="vault-page__list-actions">
              <CommonBaseButton variant="secondary" size="small" @click="copyCredential(credential)">
                복사
              </CommonBaseButton>
              <CommonBaseButton size="small" @click="openDetail(credential)">
                상세
              </CommonBaseButton>
            </div>
          </article>
        </div>
      </template>
      <section v-else class="content-panel vault-page__empty-state">
        <h2>아직 저장된 자격증명이 없습니다.</h2>
        <p class="section-description">첫 자격증명을 등록하면 여기서 바로 관리할 수 있습니다.</p>
        <CommonBaseButton @click="openCreateModal">
          첫 자격증명 등록
        </CommonBaseButton>
      </section>
    </section>

    <VaultCredentialFormModal
      :visible="isFormVisible"
      :credential="selectedCredential"
      @saved="handleSaved"
      @close="isFormVisible = false"
    />

    <VaultCredentialDetailModal
      :visible="isDetailVisible"
      :credential="selectedCredential"
      @edit="openEditModal"
      @deleted="handleDeleted"
      @close="isDetailVisible = false"
    />
  </main>
</template>

<style scoped>
.vault-page {
  display: grid;
  gap: 24px;
}

.vault-page__empty-state {
  padding: 22px;
}

.vault-page__hero {
  display: grid;
  gap: 14px;
}

.vault-page__hero-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.vault-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-weight: 700;
  font-size: 0.82rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.vault-page__hero .section-title {
  font-size: clamp(1rem, 1.3vw, 1.3rem);
}

.vault-page__hero .section-description {
  max-width: 640px;
  font-size: 0.96rem;
}

.vault-page__search-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}

.vault-page__content {
  display: grid;
  gap: 20px;
}

.vault-page__result-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.vault-page__result-count {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 700;
}

.vault-page__category-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.vault-page__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  align-items: start;
}

.vault-page__list {
  display: grid;
  padding: 8px 0;
}

.vault-page__list-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) minmax(120px, 0.8fr) minmax(180px, 1fr) auto;
  align-items: center;
  gap: 16px;
  min-height: 76px;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(176, 195, 255, 0.12);
}

.vault-page__list-row:last-child {
  border-bottom: 0;
}

.vault-page__list-main {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.vault-page__list-main h2,
.vault-page__list-main p,
.vault-page__list-login {
  margin: 0;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.vault-page__list-main h2 {
  font-size: 1rem;
}

.vault-page__list-main p,
.vault-page__list-login {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.vault-page__list-category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.vault-page__list-category,
.vault-page__list-empty-category {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 24px;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.vault-page__list-empty-category {
  color: var(--color-text-muted);
  background: rgba(176, 195, 255, 0.1);
}

.vault-page__list-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.vault-page__empty-state {
  display: grid;
  gap: 14px;
  justify-items: start;
}

.vault-page__empty-state h2 {
  margin: 0;
}

@media (max-width: 960px) {
  .vault-page__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .vault-page__list-row {
    grid-template-columns: minmax(180px, 1.4fr) minmax(120px, 0.8fr) auto;
  }

  .vault-page__list-category-list {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .vault-page__hero,
  .vault-page__empty-state {
    padding: 22px;
  }

  .vault-page__hero-header {
    flex-direction: column;
  }

  .vault-page__hero-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .vault-page__grid {
    grid-template-columns: 1fr;
  }

  .vault-page__result-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .vault-page__list-row {
    grid-template-columns: 1fr;
    align-items: start;
    gap: 10px;
    padding: 16px;
  }

  .vault-page__list-actions {
    justify-content: flex-start;
  }

  .vault-page__search-form {
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
  }

  .vault-page__search-form :deep(.base-button:last-child) {
    grid-column: 1 / -1;
  }
}
</style>
