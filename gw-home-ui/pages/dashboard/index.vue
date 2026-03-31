<script setup lang="ts">
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import type { BoardSummary } from '~/types/api/board'
import type { NoticeDetail, NoticeSummary } from '~/types/api/notice'

definePageMeta({
  middleware: 'auth'
})

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const { fetchBoardList } = useBoard()
const { fetchDashboardNotices, fetchNotice } = useNoticeApi()
const recentBoards = ref<BoardSummary[]>([])
const recentNotices = ref<NoticeSummary[]>([])
const selectedNotice = ref<NoticeDetail | null>(null)
const errorMessage = ref('')
const noticeErrorMessage = ref('')
const detailErrorMessage = ref('')
const isDetailLoading = ref(false)
const selectedNoticeUuid = computed(() =>
  typeof route.query.noticeUuid === 'string' ? route.query.noticeUuid : ''
)
const isDetailModalVisible = computed(() => Boolean(selectedNoticeUuid.value))

try {
  const [boardResponse, noticeResponse] = await Promise.all([
    fetchBoardList({
      page: 1,
      size: 5,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    }),
    fetchDashboardNotices(5)
  ])

  recentBoards.value = boardResponse.content
  recentNotices.value = noticeResponse
} catch (error) {
  const fetchError = error as { data?: { message?: string } }
  errorMessage.value = fetchError.data?.message ?? '대시보드 정보를 불러오지 못했습니다.'
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
  <main class="page-container dashboard-page">
    <section class="dashboard-page__hero content-panel">
      <div>
        <p class="dashboard-page__eyebrow">Dashboard</p>
        <h1 class="section-title">{{ authStore.currentUser?.nickname }}님, 다시 오셨네요.</h1>
        <p class="section-description">
          {{ authStore.currentUser?.email }} 계정으로 로그인 중이며, 최근 글 흐름을 바로 확인할 수 있습니다.
        </p>
      </div>

      <div class="dashboard-page__profile-card">
        <span>Login ID</span>
        <strong>{{ authStore.currentUser?.loginId }}</strong>
        <span>권한 {{ authStore.currentUser?.role }}</span>
      </div>
    </section>

    <section class="dashboard-page__section content-panel">
      <div class="dashboard-page__section-header">
        <div>
          <h2>최근 공지</h2>
          <p class="message-muted">운영 안내와 변경 사항을 먼저 확인하세요.</p>
        </div>
        <CommonBaseButton variant="secondary" to="/notices">
          전체 공지
        </CommonBaseButton>
      </div>

      <p v-if="noticeErrorMessage" class="message-error">
        {{ noticeErrorMessage }}
      </p>
      <div v-else-if="recentNotices.length" class="dashboard-page__notice-list">
        <button
          v-for="notice in recentNotices"
          :key="notice.noticeUuid"
          class="dashboard-page__notice-item"
          type="button"
          @click="openNoticeDetail(notice.noticeUuid)"
        >
          <strong>{{ notice.title }}</strong>
          <span>{{ new Date(notice.createdAt).toLocaleDateString() }}</span>
        </button>
      </div>
      <p v-else class="message-muted">
        표시할 공지사항이 아직 없습니다.
      </p>
    </section>

    <section class="dashboard-page__section content-panel">
      <div class="dashboard-page__section-header">
        <div>
          <h2>최근 게시글</h2>
          <p class="message-muted">최신 5건을 먼저 보여줍니다.</p>
        </div>

        <div class="dashboard-page__actions">
          <CommonBaseButton variant="secondary" to="/board">
            전체 게시글
          </CommonBaseButton>
          <CommonBaseButton to="/board/create">
            게시글 작성
          </CommonBaseButton>
        </div>
      </div>

      <p v-if="errorMessage" class="message-error">
        {{ errorMessage }}
      </p>

      <div v-else-if="recentBoards.length" class="dashboard-page__board-list">
        <BoardBoardListItem
          v-for="board in recentBoards"
          :key="board.boardPostUuid"
          :board="board"
        />
      </div>

      <p v-else class="message-muted">
        표시할 게시글이 아직 없습니다.
      </p>
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
        <header class="dashboard-page__notice-detail-header">
          <div class="meta-row">
            <span>작성 {{ selectedNotice.createdBy || '관리자' }}</span>
            <span>등록일 {{ new Date(selectedNotice.createdAt).toLocaleString() }}</span>
            <span>조회 {{ selectedNotice.viewCount }}</span>
          </div>
        </header>

        <article class="dashboard-page__notice-detail-content" v-html="renderedContent" />
      </template>
    </CommonBaseModal>
  </main>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 24px;
}

.dashboard-page__hero,
.dashboard-page__section {
  padding: 28px;
}

.dashboard-page__hero {
  display: grid;
  grid-template-columns: 1.3fr 0.7fr;
  gap: 20px;
  align-items: center;
}

.dashboard-page__eyebrow {
  margin: 0 0 10px;
  color: var(--color-accent);
  font-weight: 700;
}

.dashboard-page__profile-card {
  display: grid;
  gap: 8px;
  padding: 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.06);
}

.dashboard-page__profile-card span {
  color: var(--color-text-muted);
}

.dashboard-page__profile-card strong {
  font-size: 1.4rem;
}

.dashboard-page__section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.dashboard-page__section-header h2 {
  margin: 0;
}

.dashboard-page__actions,
.dashboard-page__board-list,
.dashboard-page__notice-list {
  display: grid;
  gap: 14px;
}

.dashboard-page__notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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

.dashboard-page__notice-item span {
  color: var(--color-text-muted);
  white-space: nowrap;
}

.dashboard-page__notice-item:hover {
  transform: translateY(-1px);
  border-color: rgba(147, 210, 255, 0.26);
}

.dashboard-page__notice-item:focus-visible {
  outline: 2px solid rgba(110, 193, 255, 0.24);
  outline-offset: 2px;
}

.dashboard-page__notice-detail-header {
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.14);
}

.dashboard-page__notice-detail-content {
  line-height: 1.7;
  word-break: break-word;
}

.dashboard-page__notice-detail-content:deep(p:first-child) {
  margin-top: 0;
}

.dashboard-page__notice-detail-content:deep(p:last-child) {
  margin-bottom: 0;
}

.dashboard-page__notice-detail-content:deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(147, 210, 255, 0.12);
  color: #a9dcff;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.dashboard-page__notice-detail-content:deep(pre) {
  margin: 12px 0 0;
  padding: 14px;
  overflow-x: auto;
  border-radius: 12px;
  background: rgba(3, 12, 24, 0.82);
  border: 1px solid rgba(147, 210, 255, 0.14);
}

.dashboard-page__notice-detail-content:deep(pre code) {
  padding: 0;
  background: transparent;
}

.dashboard-page__notice-detail-content:deep(a) {
  color: #8bd0ff;
  text-decoration: underline;
}

.dashboard-page__notice-detail-content:deep(ul),
.dashboard-page__notice-detail-content:deep(ol) {
  padding-left: 20px;
}

@media (max-width: 768px) {
  .dashboard-page__hero {
    grid-template-columns: 1fr;
  }

  .dashboard-page__section-header {
    flex-direction: column;
    align-items: stretch;
  }

  .dashboard-page__notice-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
