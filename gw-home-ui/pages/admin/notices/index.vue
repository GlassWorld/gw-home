<script setup lang="ts">
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import type { NoticeDetail, NoticeSummary, SaveNoticeForm } from '~/types/api/notice'

definePageMeta({
  middleware: 'admin'
})

const { fetchAdminNoticeList, createNotice, updateNotice, deleteNotice, fetchAdminNotice } = useNoticeApi()
const { showToast } = useToast()
const { confirm } = useDialog()

const noticeList = ref<NoticeSummary[]>([])
const keyword = ref('')
const page = ref(1)
const totalPages = ref(0)
const isSubmitting = ref(false)
const isLoading = ref(false)
const isPreviewLoading = ref(false)
const isEditorVisible = ref(false)
const editingNoticeUuid = ref('')
const previewNotice = ref<NoticeDetail | null>(null)
const previewNoticeUuid = ref('')
const formState = reactive<SaveNoticeForm>({
  title: '',
  content: ''
})

function renderMarkdown(rawValue: string | null | undefined): string {
  const normalizedValue = rawValue?.trim()

  if (!normalizedValue) {
    return '<p>내용이 없습니다.</p>'
  }

  const parsedHtml = marked.parse(normalizedValue, { async: false })

  if (!import.meta.client) {
    return parsedHtml
  }

  return DOMPurify.sanitize(parsedHtml)
}

const renderedPreviewContent = computed(() => renderMarkdown(previewNotice.value?.content))

async function loadNoticeList() {
  isLoading.value = true

  try {
    const response = await fetchAdminNoticeList({
      keyword: keyword.value || undefined,
      page: page.value,
      size: 10,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    })

    noticeList.value = response.content
    totalPages.value = response.totalPages
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    noticeList.value = []
    showToast(fetchError.data?.message ?? '공지 목록을 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isLoading.value = false
  }
}

function resetForm() {
  editingNoticeUuid.value = ''
  formState.title = ''
  formState.content = ''
}

function openCreateModal() {
  resetForm()
  isEditorVisible.value = true
}

function closeEditorModal() {
  isEditorVisible.value = false
  resetForm()
}

async function startEdit(noticeUuid: string) {
  try {
    const notice = await fetchAdminNotice(noticeUuid)
    editingNoticeUuid.value = notice.noticeUuid
    formState.title = notice.title
    formState.content = notice.content
    isEditorVisible.value = true
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '공지 상세를 불러오지 못했습니다.', { variant: 'error' })
  }
}

async function openPreview(noticeUuid: string) {
  isPreviewLoading.value = true
  previewNoticeUuid.value = noticeUuid
  previewNotice.value = null

  try {
    previewNotice.value = await fetchAdminNotice(noticeUuid)
  } catch (error) {
    previewNoticeUuid.value = ''
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '공지 상세를 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isPreviewLoading.value = false
  }
}

function closePreview() {
  previewNoticeUuid.value = ''
  previewNotice.value = null
  isPreviewLoading.value = false
}

async function handleSubmit() {
  if (isSubmitting.value) {
    return
  }

  isSubmitting.value = true

  try {
    const payload: SaveNoticeForm = {
      title: formState.title.trim(),
      content: formState.content.trim()
    }

    if (editingNoticeUuid.value) {
      await updateNotice(editingNoticeUuid.value, payload)
      showToast('공지사항을 수정했습니다.', { variant: 'success' })
    } else {
      await createNotice(payload)
      showToast('공지사항을 등록했습니다.', { variant: 'success' })
    }

    closeEditorModal()
    await loadNoticeList()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '공지사항 저장에 실패했습니다.', { variant: 'error' })
  } finally {
    isSubmitting.value = false
  }
}

async function handleDelete(noticeUuid: string) {
  const shouldDelete = await confirm('공지사항을 삭제할까요?', {
    title: '공지사항 삭제',
    confirmText: '삭제',
    cancelText: '취소'
  })

  if (!shouldDelete) {
    return
  }

  try {
    await deleteNotice(noticeUuid)
    showToast('공지사항을 삭제했습니다.', { variant: 'success' })

    if (editingNoticeUuid.value === noticeUuid) {
      resetForm()
    }

    await loadNoticeList()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '공지사항 삭제에 실패했습니다.', { variant: 'error' })
  }
}

async function handleSearch() {
  page.value = 1
  await loadNoticeList()
}

async function movePage(nextPage: number) {
  page.value = nextPage
  await loadNoticeList()
}

await loadNoticeList()
</script>

<template>
  <main class="page-container notice-admin-page">
    <section class="content-panel notice-admin-page__panel">
      <div class="notice-admin-page__header">
        <div>
          <p class="notice-admin-page__eyebrow">Admin</p>
          <h1 class="section-title">공지관리</h1>
        </div>
        <CommonBaseButton @click="openCreateModal">
          공지 등록
        </CommonBaseButton>
      </div>
    </section>

    <section class="content-panel notice-admin-page__panel">
      <div class="notice-admin-page__toolbar">
        <form class="notice-admin-page__search" @submit.prevent="handleSearch">
          <input v-model="keyword" class="input-field" type="search" placeholder="공지 제목 검색">
          <CommonBaseButton type="submit" variant="secondary">
            검색
          </CommonBaseButton>
        </form>
      </div>

      <p v-if="isLoading" class="message-muted">
        공지 목록을 불러오는 중입니다.
      </p>

      <div v-else class="notice-admin-page__list">
        <article
          v-for="notice in noticeList"
          :key="notice.noticeUuid"
          class="notice-admin-page__item"
        >
          <div class="notice-admin-page__item-main">
            <strong>{{ notice.title }}</strong>
            <div class="meta-row">
              <span>{{ new Date(notice.createdAt).toLocaleString() }}</span>
              <span>조회 {{ notice.viewCount }}</span>
            </div>
          </div>

          <div class="notice-admin-page__item-actions">
            <CommonBaseButton variant="secondary" size="small" @click="startEdit(notice.noticeUuid)">
              수정
            </CommonBaseButton>
            <button
              type="button"
              class="notice-admin-page__detail-link"
              @click="openPreview(notice.noticeUuid)"
            >
              미리보기
            </button>
            <CommonBaseButton variant="danger" size="small" @click="handleDelete(notice.noticeUuid)">
              삭제
            </CommonBaseButton>
          </div>
        </article>

        <p v-if="noticeList.length === 0" class="message-muted">
          등록된 공지사항이 없습니다.
        </p>
      </div>

      <div class="notice-admin-page__pagination">
        <CommonBaseButton variant="secondary" :disabled="page <= 1" @click="movePage(page - 1)">
          이전
        </CommonBaseButton>
        <span>{{ page }} / {{ totalPages || 1 }}</span>
        <CommonBaseButton variant="secondary" :disabled="page >= totalPages" @click="movePage(page + 1)">
          다음
        </CommonBaseButton>
      </div>
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

    <CommonBaseModal
      :visible="Boolean(previewNoticeUuid)"
      :title="previewNotice?.title ?? '공지사항 미리보기'"
      eyebrow="Notice Preview"
      width="min(920px, 100%)"
      @close="closePreview"
    >
      <p v-if="isPreviewLoading" class="message-muted">
        공지사항 상세를 불러오는 중입니다.
      </p>

      <template v-else-if="previewNotice">
        <header class="notice-admin-page__preview-header">
          <div class="meta-row">
            <span>작성 {{ previewNotice.createdBy || '관리자' }}</span>
            <span>등록일 {{ new Date(previewNotice.createdAt).toLocaleString() }}</span>
            <span>조회 {{ previewNotice.viewCount }}</span>
          </div>
        </header>

        <article class="notice-admin-page__preview-content" v-html="renderedPreviewContent" />
      </template>
    </CommonBaseModal>
  </main>
</template>

<style scoped>
.notice-admin-page {
  display: grid;
  gap: 24px;
}

.notice-admin-page__panel {
  padding: 22px;
}

.notice-admin-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.notice-admin-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.notice-admin-page__form,
.notice-admin-page__list {
  display: grid;
  gap: 16px;
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
.notice-admin-page__pagination,
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
  gap: 16px;
  padding: 16px;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
}

.notice-admin-page__item-main {
  display: grid;
  gap: 8px;
}

.notice-admin-page__detail-link {
  padding: 8px 12px;
  border-radius: 12px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  color: var(--color-text);
  background: rgba(255, 255, 255, 0.04);
  font: inherit;
  cursor: pointer;
}

.notice-admin-page__detail-link:hover {
  border-color: rgba(147, 210, 255, 0.3);
}

.notice-admin-page__preview-header {
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.14);
}

.notice-admin-page__preview-content {
  line-height: 1.7;
  word-break: break-word;
}

.notice-admin-page__preview-content:deep(p:first-child) {
  margin-top: 0;
}

.notice-admin-page__preview-content:deep(p:last-child) {
  margin-bottom: 0;
}

.notice-admin-page__preview-content:deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(147, 210, 255, 0.12);
  color: #a9dcff;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.notice-admin-page__preview-content:deep(pre) {
  margin: 12px 0 0;
  padding: 14px;
  overflow-x: auto;
  border-radius: 12px;
  background: rgba(3, 12, 24, 0.82);
  border: 1px solid rgba(147, 210, 255, 0.14);
}

.notice-admin-page__preview-content:deep(pre code) {
  padding: 0;
  background: transparent;
}

.notice-admin-page__preview-content:deep(a) {
  color: #8bd0ff;
  text-decoration: underline;
}

.notice-admin-page__preview-content:deep(ul),
.notice-admin-page__preview-content:deep(ol) {
  padding-left: 20px;
}

.notice-admin-page__pagination {
  justify-content: center;
  margin-top: 18px;
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
