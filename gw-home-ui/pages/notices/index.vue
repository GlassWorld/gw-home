<script setup lang="ts">
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import type { NoticeDetail, NoticeSummary } from '~/types/api/notice'

definePageMeta({
  middleware: 'auth'
})

const route = useRoute()
const router = useRouter()
const { fetchNotice, fetchNoticeList } = useNoticeApi()

const noticeList = ref<NoticeSummary[]>([])
const totalPages = ref(0)
const totalCount = ref(0)
const isLoading = ref(false)
const errorMessage = ref('')
const selectedNotice = ref<NoticeDetail | null>(null)
const isDetailLoading = ref(false)
const detailErrorMessage = ref('')
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const page = ref(Number(route.query.page ?? 1) || 1)
const selectedNoticeUuid = computed(() =>
  typeof route.query.noticeUuid === 'string' ? route.query.noticeUuid : ''
)
const isDetailModalVisible = computed(() => Boolean(selectedNoticeUuid.value))

function buildQuery(nextPage: number) {
  return {
    ...route.query,
    page: String(nextPage),
    keyword: keyword.value || undefined,
    noticeUuid: undefined
  }
}

function isSameQuery(nextQuery: ReturnType<typeof buildQuery>) {
  const currentKeyword = typeof route.query.keyword === 'string' ? route.query.keyword : undefined
  const currentPage = typeof route.query.page === 'string' ? route.query.page : '1'

  return currentPage === nextQuery.page && currentKeyword === nextQuery.keyword
}

async function loadNoticeList() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await fetchNoticeList({
      keyword: keyword.value || undefined,
      page: page.value,
      size: 10,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    })

    noticeList.value = response.content
    totalPages.value = response.totalPages
    totalCount.value = response.totalCount
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '공지사항 목록을 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

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

const renderedContent = computed(() => renderMarkdown(selectedNotice.value?.content))

async function loadSelectedNotice(noticeUuid: string) {
  isDetailLoading.value = true
  detailErrorMessage.value = ''
  selectedNotice.value = null

  try {
    selectedNotice.value = await fetchNotice(noticeUuid)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    detailErrorMessage.value = fetchError.data?.message ?? '공지사항 상세를 불러오지 못했습니다.'
  } finally {
    isDetailLoading.value = false
  }
}

async function openNoticeDetail(noticeUuid: string) {
  await router.push({
    query: {
      ...route.query,
      noticeUuid
    }
  })
}

async function closeNoticeDetail() {
  await router.push({
    query: {
      ...route.query,
      noticeUuid: undefined
    }
  })
}

async function handleSearch() {
  page.value = 1
  const nextQuery = buildQuery(1)

  if (isSameQuery(nextQuery)) {
    await loadNoticeList()
    return
  }

  await router.push({ query: nextQuery })
}

async function movePage(nextPage: number) {
  page.value = nextPage
  const nextQuery = buildQuery(nextPage)

  if (isSameQuery(nextQuery)) {
    await loadNoticeList()
    return
  }

  await router.push({ query: nextQuery })
}

watch(
  () => [route.query.keyword, route.query.page],
  async ([nextKeyword, nextPage]) => {
    keyword.value = typeof nextKeyword === 'string' ? nextKeyword : ''
    page.value = Number(nextPage ?? 1) || 1
    await loadNoticeList()
  },
  { immediate: true }
)

watch(
  selectedNoticeUuid,
  async noticeUuid => {
    if (!noticeUuid) {
      selectedNotice.value = null
      detailErrorMessage.value = ''
      isDetailLoading.value = false
      return
    }

    await loadSelectedNotice(noticeUuid)
  },
  { immediate: true }
)
</script>

<template>
  <main class="page-container notice-page">
    <section class="content-panel notice-page__hero">
      <div>
        <p class="notice-page__eyebrow">Notice</p>
        <h1 class="section-title">공지사항</h1>
        <p class="section-description">중요한 운영 안내를 한곳에서 확인할 수 있습니다.</p>
      </div>

      <form class="notice-page__search-form" @submit.prevent="handleSearch">
        <input
          v-model="keyword"
          class="input-field"
          type="search"
          placeholder="공지 제목 검색"
        >
        <CommonBaseButton type="submit">
          검색
        </CommonBaseButton>
      </form>
    </section>

    <section class="content-panel notice-page__list-panel">
      <div class="notice-page__list-header">
        <p class="message-muted">총 {{ totalCount }}건</p>
      </div>

      <p v-if="errorMessage" class="message-error">
        {{ errorMessage }}
      </p>
      <p v-else-if="isLoading" class="message-muted">
        공지사항을 불러오는 중입니다.
      </p>
      <div v-else-if="noticeList.length" class="notice-page__list">
        <button
          v-for="notice in noticeList"
          :key="notice.noticeUuid"
          class="notice-page__item"
          type="button"
          @click="openNoticeDetail(notice.noticeUuid)"
        >
          <div class="notice-page__item-main">
            <strong>{{ notice.title }}</strong>
            <span>작성 {{ notice.createdBy || '관리자' }}</span>
          </div>
          <div class="notice-page__item-meta">
            <span>{{ new Date(notice.createdAt).toLocaleDateString() }}</span>
            <span>조회 {{ notice.viewCount }}</span>
          </div>
        </button>
      </div>
      <p v-else class="message-muted">
        등록된 공지사항이 없습니다.
      </p>

      <div class="notice-page__pagination">
        <CommonBaseButton
          variant="secondary"
          :disabled="page <= 1"
          @click="movePage(page - 1)"
        >
          이전
        </CommonBaseButton>
        <span>{{ page }} / {{ totalPages || 1 }}</span>
        <CommonBaseButton
          variant="secondary"
          :disabled="page >= totalPages"
          @click="movePage(page + 1)"
        >
          다음
        </CommonBaseButton>
      </div>
    </section>

    <CommonBaseModal
      :visible="isDetailModalVisible"
      :title="selectedNotice?.title ?? '공지사항 상세'"
      eyebrow="Notice"
      width="min(920px, 100%)"
      @close="closeNoticeDetail"
    >
      <p v-if="detailErrorMessage" class="message-error">
        {{ detailErrorMessage }}
      </p>

      <p v-else-if="isDetailLoading" class="message-muted">
        공지사항 상세를 불러오는 중입니다.
      </p>

      <template v-else-if="selectedNotice">
        <header class="notice-detail-modal__header">
          <div class="meta-row">
            <span>작성 {{ selectedNotice.createdBy || '관리자' }}</span>
            <span>등록일 {{ new Date(selectedNotice.createdAt).toLocaleString() }}</span>
            <span>조회 {{ selectedNotice.viewCount }}</span>
          </div>
        </header>

        <article class="notice-detail-modal__content" v-html="renderedContent" />
      </template>
    </CommonBaseModal>
  </main>
</template>

<style scoped>
.notice-page {
  display: grid;
  gap: 24px;
}

.notice-page__hero,
.notice-page__list-panel {
  padding: 28px;
}

.notice-page__hero {
  display: grid;
  gap: 18px;
}

.notice-page__eyebrow {
  margin: 0 0 10px;
  color: var(--color-accent);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.notice-page__search-form {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}

.notice-page__list,
.notice-page__pagination {
  display: grid;
  gap: 14px;
}

.notice-page__list {
  margin: 20px 0;
}

.notice-page__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  width: 100%;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(147, 210, 255, 0.12);
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.notice-page__item-main,
.notice-page__item-meta {
  display: grid;
  gap: 6px;
}

.notice-page__item-main strong {
  font-size: 1.02rem;
}

.notice-page__item-main span,
.notice-page__item-meta {
  color: var(--color-text-muted);
}

.notice-page__item-meta {
  justify-items: end;
  white-space: nowrap;
}

.notice-page__item:hover {
  transform: translateY(-1px);
  border-color: rgba(147, 210, 255, 0.26);
}

.notice-page__item:focus-visible {
  outline: 2px solid rgba(110, 193, 255, 0.24);
  outline-offset: 2px;
}

.notice-page__pagination {
  grid-template-columns: repeat(3, auto);
  justify-content: center;
  align-items: center;
}

.notice-detail-modal__header {
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.14);
}

.notice-detail-modal__content {
  line-height: 1.7;
  word-break: break-word;
}

.notice-detail-modal__content:deep(p:first-child) {
  margin-top: 0;
}

.notice-detail-modal__content:deep(p:last-child) {
  margin-bottom: 0;
}

.notice-detail-modal__content:deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(147, 210, 255, 0.12);
  color: #a9dcff;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.notice-detail-modal__content:deep(pre) {
  margin: 12px 0 0;
  padding: 14px;
  overflow-x: auto;
  border-radius: 12px;
  background: rgba(3, 12, 24, 0.82);
  border: 1px solid rgba(147, 210, 255, 0.14);
}

.notice-detail-modal__content:deep(pre code) {
  padding: 0;
  background: transparent;
}

.notice-detail-modal__content:deep(a) {
  color: #8bd0ff;
  text-decoration: underline;
}

.notice-detail-modal__content:deep(ul),
.notice-detail-modal__content:deep(ol) {
  padding-left: 20px;
}

@media (max-width: 768px) {
  .notice-page__search-form {
    grid-template-columns: 1fr;
  }

  .notice-page__item {
    flex-direction: column;
    align-items: flex-start;
  }

  .notice-page__item-meta {
    justify-items: start;
  }
}
</style>
