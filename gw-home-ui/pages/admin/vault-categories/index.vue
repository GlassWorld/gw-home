<script setup lang="ts">
import { useAdminVaultCategoryManagement } from '~/features/admin/composables/use-admin-vault-category-management'

definePageMeta({
  middleware: 'admin'
})

const {
  categoryList,
  editingCategoryUuid,
  isSubmitting,
  formState,
  resetForm,
  startEdit,
  handleSubmit,
  handleDelete,
  loadCategoryList
} = useAdminVaultCategoryManagement()

await loadCategoryList()
</script>

<template>
  <main class="page-container vault-category-admin-page">
    <section class="content-panel vault-category-admin-page__panel page-panel-padding-md">
      <div class="vault-category-admin-page__header">
        <div>
          <p class="vault-category-admin-page__eyebrow">Admin</p>
          <h1 class="section-title">Vault 카테고리 관리</h1>
        </div>
      </div>

      <form class="vault-category-admin-page__form" @submit.prevent="handleSubmit">
        <label class="vault-category-admin-page__field vault-category-admin-page__field--name">
          <span>이름</span>
          <input v-model="formState.name" class="input-field" type="text" maxlength="100" required>
        </label>
        <label class="vault-category-admin-page__field vault-category-admin-page__field--color">
          <span>색상</span>
          <div class="vault-category-admin-page__color-field">
            <input v-model="formState.color" class="vault-category-admin-page__color-input" type="color">
            <span class="vault-category-admin-page__color-value">{{ formState.color }}</span>
          </div>
        </label>
        <label class="vault-category-admin-page__field vault-category-admin-page__field--sort">
          <span>정렬 순서</span>
          <input v-model.number="formState.sortOrder" class="input-field" type="number" min="0">
        </label>
        <label class="vault-category-admin-page__field vault-category-admin-page__field--wide">
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

    <section class="content-panel vault-category-admin-page__panel page-panel-padding-md">
      <div class="vault-category-admin-page__list">
        <article
          v-for="category in categoryList"
          :key="category.categoryUuid"
          class="vault-category-admin-page__item"
        >
          <div class="vault-category-admin-page__item-summary">
            <strong class="vault-category-admin-page__item-name">
              <span
                class="vault-category-admin-page__item-badge"
                :style="{
                  background: category.color ? `${category.color}22` : 'rgba(110, 193, 255, 0.12)',
                  color: category.color ?? 'var(--color-accent)'
                }"
              >
                {{ category.name }}
              </span>
            </strong>
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
  grid-template-columns: minmax(0, 1fr) auto 140px;
  gap: 14px 16px;
  margin-top: 18px;
  align-items: end;
}

.vault-category-admin-page__form label {
  display: grid;
  gap: 8px;
}

.vault-category-admin-page__field {
  min-width: 0;
}

.vault-category-admin-page__field--name {
  min-width: 0;
}

.vault-category-admin-page__field--color {
  min-width: 170px;
}

.vault-category-admin-page__field--sort {
  min-width: 140px;
}

.vault-category-admin-page__field--wide {
  grid-column: 1 / -1;
}

.vault-category-admin-page__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  grid-column: 2 / 4;
}

.vault-category-admin-page__color-field {
  display: flex;
  align-items: center;
  gap: 12px;
}

.vault-category-admin-page__color-input {
  width: 52px;
  height: 40px;
  padding: 4px;
  border: 1px solid rgba(147, 210, 255, 0.28);
  border-radius: var(--radius-small);
  background: rgba(8, 23, 42, 0.72);
  cursor: pointer;
}

.vault-category-admin-page__color-value {
  color: var(--color-text-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
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

.vault-category-admin-page__item-name {
  display: flex;
  align-items: center;
}

.vault-category-admin-page__item-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
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

  .vault-category-admin-page__actions {
    grid-column: 1 / -1;
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
