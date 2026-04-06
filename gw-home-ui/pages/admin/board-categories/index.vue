<script setup lang="ts">
import { useAdminBoardCategoryManagement } from '~/features/admin/composables/use-admin-board-category-management'

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
} = useAdminBoardCategoryManagement()

await loadCategoryList()
</script>

<template>
  <main class="page-container board-category-admin-page">
    <section class="content-panel board-category-admin-page__panel page-panel-padding-md">
      <div class="board-category-admin-page__header">
        <div>
          <p class="board-category-admin-page__eyebrow">Admin</p>
          <h1 class="section-title">게시글 카테고리 관리</h1>
        </div>
      </div>

      <form class="board-category-admin-page__form" @submit.prevent="handleSubmit">
        <label class="board-category-admin-page__field board-category-admin-page__field--name">
          <span>이름</span>
          <input v-model="formState.name" class="input-field" type="text" maxlength="50" required>
        </label>
        <label class="board-category-admin-page__field board-category-admin-page__field--sort">
          <span>정렬 순서</span>
          <input v-model.number="formState.sortOrder" class="input-field" type="number" min="0" required>
        </label>

        <div class="board-category-admin-page__actions">
          <CommonBaseButton v-if="editingCategoryUuid" variant="secondary" @click="resetForm">
            취소
          </CommonBaseButton>
          <CommonBaseButton type="submit" :disabled="isSubmitting">
            {{ editingCategoryUuid ? '카테고리 수정' : '카테고리 등록' }}
          </CommonBaseButton>
        </div>
      </form>
    </section>

    <section class="content-panel board-category-admin-page__panel page-panel-padding-md">
      <div class="board-category-admin-page__list">
        <article
          v-for="category in categoryList"
          :key="category.categoryUuid"
          class="board-category-admin-page__item"
        >
          <div class="board-category-admin-page__item-summary">
            <strong>{{ category.categoryName }}</strong>
            <span class="board-category-admin-page__item-sort">정렬순서 {{ category.sortOrder }}</span>
          </div>

          <div class="board-category-admin-page__item-actions">
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
.board-category-admin-page {
  display: grid;
  gap: 24px;
}

.board-category-admin-page__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.board-category-admin-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.board-category-admin-page__form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px auto;
  gap: 14px 16px;
  margin-top: 18px;
  align-items: end;
}

.board-category-admin-page__field {
  display: grid;
  gap: 8px;
}

.board-category-admin-page__actions,
.board-category-admin-page__item,
.board-category-admin-page__item-actions,
.board-category-admin-page__item-summary {
  display: flex;
  gap: 12px;
}

.board-category-admin-page__actions {
  justify-content: flex-end;
}

.board-category-admin-page__list {
  display: grid;
  gap: 14px;
}

.board-category-admin-page__item {
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
}

.board-category-admin-page__item-summary {
  align-items: center;
}

.board-category-admin-page__item-sort {
  color: var(--color-accent);
  font-size: 0.88rem;
  font-weight: 700;
}

@media (max-width: 768px) {
  .board-category-admin-page__form {
    grid-template-columns: 1fr;
  }

  .board-category-admin-page__item,
  .board-category-admin-page__actions,
  .board-category-admin-page__item-actions,
  .board-category-admin-page__item-summary {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
