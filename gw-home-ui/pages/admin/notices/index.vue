<script setup lang="ts">
import NoticeDetailModal from '~/features/notice/components/NoticeDetailModal.vue'
import { useAdminNoticeManagement } from '~/features/admin/composables/use-admin-notice-management'

definePageMeta({
  middleware: 'admin'
})

const {
  noticeList,
  keyword,
  page,
  totalPages,
  isSubmitting,
  isLoading,
  isPreviewLoading,
  isEditorVisible,
  editingNoticeUuid,
  previewNotice,
  previewNoticeUuid,
  formState,
  formatDateTime,
  loadNoticeList,
  openCreateModal,
  closeEditorModal,
  startEdit,
  openPreview,
  closePreview,
  handleSubmit,
  handleDelete,
  handleSearch,
  movePage
} = useAdminNoticeManagement()

await loadNoticeList()
</script>

<template>
  <main class="page-container page-stack notice-admin-page">
    <section class="content-panel notice-admin-page__panel page-panel-padding-md">
      <CommonBasePageHeader title="공지관리" eyebrow="Admin">
        <template #actions>
          <CommonBaseButton @click="openCreateModal">공지 등록</CommonBaseButton>
        </template>
      </CommonBasePageHeader>
    </section>

    <section class="content-panel notice-admin-page__panel page-panel-padding-md">
      <div class="notice-admin-page__toolbar">
        <form class="notice-admin-page__search" @submit.prevent="handleSearch">
          <input v-model="keyword" class="input-field" type="search" aria-label="공지 제목 검색" placeholder="공지 제목 검색">
          <CommonBaseButton type="submit" variant="secondary">
            검색
          </CommonBaseButton>
        </form>
      </div>

      <CommonBaseState v-if="isLoading" state="loading" message="공지 목록을 불러오는 중입니다." />

      <div v-else class="notice-admin-page__list">
        <article
          v-for="notice in noticeList"
          :key="notice.noticeUuid"
          class="notice-admin-page__item"
        >
          <div class="notice-admin-page__item-main">
            <strong>{{ notice.title }}</strong>
            <div class="meta-row">
              <span>{{ formatDateTime(notice.createdAt) }}</span>
              <span>조회 {{ notice.viewCount }}</span>
            </div>
          </div>

          <div class="notice-admin-page__item-actions">
            <CommonBaseButton variant="secondary" size="small" @click="startEdit(notice.noticeUuid)">
              수정
            </CommonBaseButton>
            <CommonBaseButton variant="secondary" size="small" @click="openPreview(notice.noticeUuid)">
              미리보기
            </CommonBaseButton>
            <CommonBaseButton variant="danger" size="small" @click="handleDelete(notice.noticeUuid)">
              삭제
            </CommonBaseButton>
          </div>
        </article>

        <CommonBaseState v-if="noticeList.length === 0" state="empty" message="등록된 공지사항이 없습니다." />
      </div>

      <CommonBasePagination :page="page" :total-pages="totalPages" :disabled="isLoading"
        @previous="movePage(page - 1)" @next="movePage(page + 1)" />
    </section>

    <CommonBaseModal
      :visible="isEditorVisible"
      :title="editingNoticeUuid ? '공지 수정' : '공지 등록'"
      eyebrow="Notice Editor"
      width="min(920px, 100%)"
      @close="closeEditorModal"
    >
      <form class="notice-admin-page__form" @submit.prevent="handleSubmit">
        <label class="notice-admin-page__field notice-admin-page__field--wide">
          <span>제목</span>
          <input v-model="formState.title" class="input-field" type="text" maxlength="300" required>
        </label>
        <label class="notice-admin-page__field notice-admin-page__field--wide">
          <span>본문</span>
          <textarea
            v-model="formState.content"
            class="input-field notice-admin-page__textarea"
            rows="14"
            required
          />
        </label>

        <div class="notice-admin-page__actions">
          <CommonBaseButton variant="secondary" type="button" @click="closeEditorModal">
            취소
          </CommonBaseButton>
          <CommonBaseButton type="submit" :disabled="isSubmitting">
            {{ editingNoticeUuid ? '공지 수정' : '공지 등록' }}
          </CommonBaseButton>
        </div>
      </form>
    </CommonBaseModal>

    <NoticeDetailModal
      :visible="Boolean(previewNoticeUuid)"
      :notice="previewNotice"
      :is-loading="isPreviewLoading"
      error-message=""
      @close="closePreview"
    />
  </main>
</template>

<style scoped>
.notice-admin-page__form,
.notice-admin-page__list {
  display: grid;
  gap: var(--section-gap);
}

.notice-admin-page__field {
  display: grid;
  gap: 8px;
}

.notice-admin-page__textarea {
  min-height: 320px;
  resize: vertical;
}

.notice-admin-page__actions,
.notice-admin-page__toolbar,
.notice-admin-page__item-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.notice-admin-page__actions {
  justify-content: flex-end;
}

.notice-admin-page__toolbar {
  margin-bottom: 18px;
}

.notice-admin-page__search {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  width: min(420px, 100%);
}

.notice-admin-page__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--section-gap);
  padding: var(--card-padding);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-medium);
  background: var(--color-surface-muted);
}

.notice-admin-page__item-main {
  display: grid;
  gap: 8px;
}

@media (max-width: 768px) {
  .notice-admin-page__search {
    grid-template-columns: 1fr;
  }

  .notice-admin-page__item {
    flex-direction: column;
    align-items: stretch;
  }

  .notice-admin-page__item-actions,
  .notice-admin-page__actions {
    flex-wrap: wrap;
  }
}
</style>
