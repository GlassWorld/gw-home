<script setup lang="ts">
import type { SaveVaultCategoryPayload, VaultCategory } from '~/types/vault'

definePageMeta({
  middleware: 'admin'
})

const { fetchAdminCategoryList, createCategory, updateCategory, removeCategory } = useVaultCategoryApi()
const { showToast } = useToast()
const { confirm } = useDialog()

const categoryList = ref<VaultCategory[]>([])
const editingCategoryUuid = ref('')
const isSubmitting = ref(false)
const formState = reactive<SaveVaultCategoryPayload>({
  name: '',
  description: '',
  sortOrder: 0
})

async function loadCategoryList() {
  try {
    categoryList.value = await fetchAdminCategoryList()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    categoryList.value = []
    showToast(fetchError.data?.message ?? '카테고리 목록을 불러오지 못했습니다.', { variant: 'error' })
  }
}

function resetForm() {
  editingCategoryUuid.value = ''
  formState.name = ''
  formState.description = ''
  formState.sortOrder = 0
}

function startEdit(category: VaultCategory) {
  editingCategoryUuid.value = category.categoryUuid
  formState.name = category.name
  formState.description = category.description ?? ''
  formState.sortOrder = category.sortOrder
}

async function handleSubmit() {
  if (isSubmitting.value) {
    return
  }

  isSubmitting.value = true

  const payload: SaveVaultCategoryPayload = {
    name: formState.name.trim(),
    description: formState.description?.trim() || '',
    sortOrder: Number(formState.sortOrder || 0)
  }

  try {
    if (editingCategoryUuid.value) {
      await updateCategory(editingCategoryUuid.value, payload)
      showToast('카테고리를 수정했습니다.', { variant: 'success' })
    } else {
      await createCategory(payload)
      showToast('카테고리를 등록했습니다.', { variant: 'success' })
    }

    resetForm()
    await loadCategoryList()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '카테고리 저장에 실패했습니다.', { variant: 'error' })
  } finally {
    isSubmitting.value = false
  }
}

async function handleDelete(categoryUuid: string) {
  const shouldDelete = await confirm('카테고리를 삭제할까요?', {
    title: '카테고리 삭제',
    confirmText: '삭제',
    cancelText: '취소'
  })

  if (!shouldDelete) {
    return
  }

  try {
    await removeCategory(categoryUuid)
    showToast('카테고리를 삭제했습니다.', { variant: 'success' })
    await loadCategoryList()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '카테고리 삭제에 실패했습니다.', { variant: 'error' })
  }
}

await loadCategoryList()
</script>

<template>
  <main class="page-container vault-category-admin-page">
    <section class="content-panel vault-category-admin-page__panel">
      <div class="vault-category-admin-page__header">
        <div>
          <p class="vault-category-admin-page__eyebrow">Admin</p>
          <h1 class="section-title">Vault 카테고리 관리</h1>
        </div>
      </div>

      <form class="vault-category-admin-page__form" @submit.prevent="handleSubmit">
        <label>
          <span>이름</span>
          <input v-model="formState.name" class="input-field" type="text" maxlength="100" required>
        </label>
        <label>
          <span>정렬 순서</span>
          <input v-model.number="formState.sortOrder" class="input-field" type="number" min="0">
        </label>
        <label class="vault-category-admin-page__field--wide">
          <span>설명</span>
          <input v-model="formState.description" class="input-field" type="text" maxlength="200">
        </label>

        <div class="vault-category-admin-page__actions">
          <CommonBaseButton v-if="editingCategoryUuid" variant="secondary" @click="resetForm">
            취소
          </CommonBaseButton>
          <CommonBaseButton type="submit" :disabled="isSubmitting">
            {{ editingCategoryUuid ? '카테고리 수정' : '카테고리 등록' }}
          </CommonBaseButton>
        </div>
      </form>
    </section>

    <section class="content-panel vault-category-admin-page__panel">
      <div class="vault-category-admin-page__list">
        <article
          v-for="category in categoryList"
          :key="category.categoryUuid"
          class="vault-category-admin-page__item"
        >
          <div class="vault-category-admin-page__item-summary">
            <strong>{{ category.name }}</strong>
            <span class="vault-category-admin-page__item-sort">정렬순서 {{ category.sortOrder }}</span>
            <p>{{ category.description || '-' }}</p>
          </div>

          <div class="vault-category-admin-page__item-actions">
            <CommonBaseButton variant="secondary" size="small" @click="startEdit(category)">
              수정
            </CommonBaseButton>
            <CommonBaseButton variant="danger" size="small" @click="handleDelete(category.categoryUuid)">
              삭제
            </CommonBaseButton>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.vault-category-admin-page {
  display: grid;
  gap: 24px;
}

.vault-category-admin-page__panel {
  padding: 22px;
}

.vault-category-admin-page__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.vault-category-admin-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.vault-category-admin-page__form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.vault-category-admin-page__form label {
  display: grid;
  gap: 8px;
}

.vault-category-admin-page__field--wide {
  grid-column: 1 / -1;
}

.vault-category-admin-page__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  grid-column: 1 / -1;
}

.vault-category-admin-page__list {
  display: grid;
  gap: 14px;
}

.vault-category-admin-page__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
}

.vault-category-admin-page__item-summary {
  display: grid;
  grid-template-columns: minmax(120px, 180px) auto minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  flex: 1;
}

.vault-category-admin-page__item-summary strong,
.vault-category-admin-page__item-summary p {
  margin: 0;
}

.vault-category-admin-page__item-summary p {
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.vault-category-admin-page__item-summary span {
  color: var(--color-accent);
  font-size: 0.88rem;
  font-weight: 700;
  white-space: nowrap;
}

.vault-category-admin-page__item-sort {
  display: inline-flex;
  align-items: center;
  min-width: 92px;
}

.vault-category-admin-page__item-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 768px) {
  .vault-category-admin-page__form {
    grid-template-columns: 1fr;
  }

  .vault-category-admin-page__item {
    flex-direction: column;
    align-items: stretch;
  }

  .vault-category-admin-page__item-summary {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .vault-category-admin-page__item-actions,
  .vault-category-admin-page__actions {
    flex-wrap: wrap;
  }
}
</style>
