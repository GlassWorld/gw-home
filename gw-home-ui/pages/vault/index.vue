<script setup lang="ts">
import type { Credential, VaultCategory } from '~/types/vault'

definePageMeta({
  middleware: 'auth'
})

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const { fetchCredentialList } = useVaultApi()
const { fetchCategoryList } = useVaultCategoryApi()
const { showToast } = useToast()

const credentialList = ref<Credential[]>([])
const categoryList = ref<VaultCategory[]>([])
const selectedCredential = ref<Credential | null>(null)
const isLoading = ref(false)
const isFormVisible = ref(false)
const isDetailVisible = ref(false)
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const selectedCategoryUuid = ref(typeof route.query.categoryUuid === 'string' ? route.query.categoryUuid : '')

async function loadCredentialList() {
  isLoading.value = true

  try {
    credentialList.value = await fetchCredentialList({
      keyword: keyword.value || undefined,
      categoryUuid: selectedCategoryUuid.value || undefined
    })
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    credentialList.value = []
    showToast(fetchError.data?.message ?? '자격증명 목록을 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isLoading.value = false
  }
}

async function loadCategoryList() {
  categoryList.value = await fetchCategoryList()
}

async function openDetail(credential: Credential) {
  selectedCredential.value = credential
  isDetailVisible.value = true
}

function openCreateModal() {
  selectedCredential.value = null
  isFormVisible.value = true
}

function openEditModal(credential: Credential) {
  selectedCredential.value = credential
  isDetailVisible.value = false
  isFormVisible.value = true
}

function handleSearch() {
  router.push({
    query: {
      ...route.query,
      keyword: keyword.value || undefined,
      categoryUuid: selectedCategoryUuid.value || undefined
    }
  })
}

function handleCategorySelect(categoryUuid: string) {
  selectedCategoryUuid.value = categoryUuid
  router.push({
    query: {
      ...route.query,
      keyword: keyword.value || undefined,
      categoryUuid: categoryUuid || undefined
    }
  })
}

async function handleSaved() {
  isFormVisible.value = false
  await loadCredentialList()
  showToast('저장되었습니다.', { variant: 'success' })
}

async function handleDeleted() {
  isDetailVisible.value = false
  selectedCredential.value = null
  await loadCredentialList()
  showToast('삭제되었습니다.', { variant: 'success' })
}

async function copyCredential(credential: Credential) {
  try {
    await navigator.clipboard.writeText(credential.password)
    showToast('복사되었습니다.', { variant: 'success' })
  } catch {
    showToast('클립보드 복사에 실패했습니다.', { variant: 'error' })
  }
}

watch(
  () => [route.query.keyword, route.query.categoryUuid],
  async ([queryKeyword, queryCategoryUuid]) => {
    keyword.value = typeof queryKeyword === 'string' ? queryKeyword : ''
    selectedCategoryUuid.value = typeof queryCategoryUuid === 'string' ? queryCategoryUuid : ''
    await loadCredentialList()
  },
  { immediate: true }
)

await loadCategoryList()
</script>

<template>
  <main class="page-container vault-page">
    <section class="content-panel vault-page__hero">
      <div class="vault-page__hero-header">
        <div>
          <p class="vault-page__eyebrow">Vault</p>
          <h1 class="section-title">개인 자격증명 관리</h1>
          <p class="section-description">
            자주 쓰는 로그인 정보와 메모를 카드로 모아두고, 필요할 때 빠르게 복사할 수 있습니다.
          </p>
        </div>

        <div class="vault-page__hero-actions">
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
          placeholder="제목 또는 설명 검색"
        >
        <CommonBaseButton variant="secondary" type="submit">
          검색
        </CommonBaseButton>
      </form>

      <div v-if="categoryList.length" class="vault-page__category-filter">
        <CommonBaseButton
          :variant="selectedCategoryUuid ? 'secondary' : 'primary'"
          size="small"
          @click="handleCategorySelect('')"
        >
          전체
        </CommonBaseButton>
        <CommonBaseButton
          v-for="category in categoryList"
          :key="category.categoryUuid"
          :variant="selectedCategoryUuid === category.categoryUuid ? 'primary' : 'secondary'"
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
      <div v-else-if="credentialList.length" class="vault-page__grid">
        <VaultCredentialCard
          v-for="credential in credentialList"
          :key="credential.credentialUuid"
          :credential="credential"
          @copy="copyCredential"
          @detail="openDetail"
        />
      </div>
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

.vault-page__hero,
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

.vault-page__hero-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
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
  font-size: clamp(1.5rem, 2.6vw, 2rem);
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

.vault-page__category-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.vault-page__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
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

  .vault-page__search-form {
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
  }

  .vault-page__search-form :deep(.base-button:last-child) {
    grid-column: 1 / -1;
  }
}
</style>
