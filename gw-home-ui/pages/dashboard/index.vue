<script setup lang="ts">
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import type { BoardSummary } from '~/types/api/board'
import type { NoticeDetail, NoticeSummary } from '~/types/api/notice'
import type { DailyReport, WeeklyReport, WorkUnit } from '~/features/work/types/work.types'

definePageMeta({
  middleware: 'auth'
})

const route = useRoute()
const router = useRouter()
const { fetchBoardList } = useBoard()
const { fetchDashboardNotices, fetchNotice } = useNoticeApi()
const { fetchWeeklyReports } = useWeeklyReportApi()
const { fetchDailyReports } = useDailyReportApi()
const { fetchWorkUnitList } = useWorkUnitApi()

const recentBoards = ref<BoardSummary[]>([])
const recentNotices = ref<NoticeSummary[]>([])
const recentWeeklyReports = ref<WeeklyReport[]>([])
const recentDailyReports = ref<DailyReport[]>([])
const activeWorkUnits = ref<WorkUnit[]>([])
const selectedNotice = ref<NoticeDetail | null>(null)

const errorMessage = ref('')
const noticeErrorMessage = ref('')
const reportErrorMessage = ref('')
const workErrorMessage = ref('')
const detailErrorMessage = ref('')
const isDetailLoading = ref(false)

const selectedNoticeUuid = computed(() =>
  typeof route.query.noticeUuid === 'string' ? route.query.noticeUuid : ''
)
const isDetailModalVisible = computed(() => Boolean(selectedNoticeUuid.value))

function sortByDateDesc<T>(items: T[], selector: (item: T) => string): T[] {
  return [...items].sort((left, right) => {
    return new Date(selector(right)).getTime() - new Date(selector(left)).getTime()
  })
}

function getWeekRange(date = new Date()) {
  const current = new Date(date)
  const day = current.getDay()
  const diffToMonday = day === 0 ? -6 : 1 - day
  const start = new Date(current)
  start.setDate(current.getDate() + diffToMonday)
  start.setHours(0, 0, 0, 0)

  const end = new Date(start)
  end.setDate(start.getDate() + 6)
  end.setHours(23, 59, 59, 999)

  return { start, end }
}

function isDateInCurrentWeek(value: string): boolean {
  if (!value) {
    return false
  }

  const target = new Date(value)

  if (Number.isNaN(target.getTime())) {
    return false
  }

  const { start, end } = getWeekRange()
  return target >= start && target <= end
}

function formatDate(value: string | null | undefined, options?: Intl.DateTimeFormatOptions): string {
  if (!value) {
    return '-'
  }

  const parsedDate = new Date(value)

  if (Number.isNaN(parsedDate.getTime())) {
    return '-'
  }

  return parsedDate.toLocaleDateString('ko-KR', options)
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '-'
  }

  const parsedDate = new Date(value)

  if (Number.isNaN(parsedDate.getTime())) {
    return '-'
  }

  return parsedDate.toLocaleString('ko-KR')
}

function getReportExcerpt(content: string | null | undefined): string {
  const normalized = content?.replace(/\s+/g, ' ').trim()
  return normalized ? normalized.slice(0, 84) : '작성된 요약이 아직 없습니다.'
}

function getWorkStatusLabel(status: WorkUnit['status']): string {
  if (status === 'DONE') {
    return '완료'
  }

  if (status === 'ON_HOLD') {
    return '보류'
  }

  return '진행'
}

const [boardResult, noticeResult, weeklyReportResult, dailyReportResult, workUnitResult] = await Promise.allSettled([
  fetchBoardList({
    page: 1,
    size: 4,
    sortBy: 'createdAt',
    sortDirection: 'DESC'
  }),
  fetchDashboardNotices(4),
  fetchWeeklyReports(),
  fetchDailyReports({
    page: 1,
    size: 12
  }),
  fetchWorkUnitList({
    useYn: 'Y',
    sort: 'recent'
  })
])

if (boardResult.status === 'fulfilled') {
  recentBoards.value = boardResult.value.content
} else {
  errorMessage.value = '게시글 정보를 불러오지 못했습니다.'
}

if (noticeResult.status === 'fulfilled') {
  recentNotices.value = noticeResult.value
} else {
  noticeErrorMessage.value = '공지사항 정보를 불러오지 못했습니다.'
}

if (weeklyReportResult.status === 'fulfilled') {
  recentWeeklyReports.value = sortByDateDesc(weeklyReportResult.value, item => item.weekEndDate).slice(0, 3)
} else {
  reportErrorMessage.value = '주간보고 정보를 불러오지 못했습니다.'
}

if (dailyReportResult.status === 'fulfilled') {
  recentDailyReports.value = sortByDateDesc(dailyReportResult.value.content, item => item.reportDate)
} else if (!reportErrorMessage.value) {
  reportErrorMessage.value = '일일보고 정보를 불러오지 못했습니다.'
}

if (workUnitResult.status === 'fulfilled') {
  activeWorkUnits.value = workUnitResult.value.filter(item => item.useYn === 'Y').slice(0, 4)
} else {
  workErrorMessage.value = '업무 정보를 불러오지 못했습니다.'
}

const currentWeekLabel = computed(() => {
  const { start, end } = getWeekRange()
  return `${formatDate(start.toISOString(), { month: 'numeric', day: 'numeric' })} - ${formatDate(end.toISOString(), { month: 'numeric', day: 'numeric' })}`
})

const reportStats = computed(() => {
  const thisWeekDailyCount = recentDailyReports.value.filter(report => isDateInCurrentWeek(report.reportDate)).length
  const thisWeekWorkUnitCount = recentDailyReports.value
    .filter(report => isDateInCurrentWeek(report.reportDate))
    .reduce((sum, report) => sum + report.workUnits.length, 0)

  return [
    {
      label: '이번 주 일일보고',
      value: `${thisWeekDailyCount}건`
    },
    {
      label: '주간보고',
      value: `${recentWeeklyReports.value.length}건`
    },
    {
      label: '활성 업무',
      value: `${activeWorkUnits.value.length}건`
    },
    {
      label: '보고된 업무',
      value: `${thisWeekWorkUnitCount}건`
    }
  ]
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
    <p v-if="errorMessage && !recentBoards.length && !recentNotices.length" class="message-error">
      {{ errorMessage }}
    </p>

    <section class="dashboard-page__summary-grid">
      <article class="dashboard-page__section content-panel">
        <div class="dashboard-page__section-header">
          <div>
            <h2>공지사항</h2>
            <p class="message-muted">최신 안내 4건</p>
          </div>
          <CommonBaseButton variant="secondary" to="/notices">
            전체
          </CommonBaseButton>
        </div>

        <p v-if="noticeErrorMessage" class="message-error">
          {{ noticeErrorMessage }}
        </p>
        <div v-else-if="recentNotices.length" class="dashboard-page__compact-list">
          <button
            v-for="notice in recentNotices"
            :key="notice.noticeUuid"
            class="dashboard-page__compact-item"
            type="button"
            @click="openNoticeDetail(notice.noticeUuid)"
          >
            <strong>{{ notice.title }}</strong>
            <span>{{ formatDate(notice.createdAt, { month: 'numeric', day: 'numeric' }) }}</span>
          </button>
        </div>
        <p v-else class="message-muted">
          공지사항이 없습니다.
        </p>
      </article>

      <article class="dashboard-page__section content-panel">
        <div class="dashboard-page__section-header">
          <div>
            <h2>게시글</h2>
            <p class="message-muted">최근 등록 4건</p>
          </div>
          <CommonBaseButton variant="secondary" to="/board">
            전체
          </CommonBaseButton>
        </div>

        <div v-if="recentBoards.length" class="dashboard-page__compact-list">
          <NuxtLink
            v-for="board in recentBoards"
            :key="board.boardPostUuid"
            class="dashboard-page__compact-item dashboard-page__compact-item--link"
            :to="`/board/${board.boardPostUuid}`"
          >
            <strong>{{ board.title }}</strong>
            <span>{{ board.author }} · 댓글 {{ board.commentCount }}</span>
          </NuxtLink>
        </div>
        <p v-else class="message-muted">
          게시글이 없습니다.
        </p>
      </article>
    </section>

    <section class="dashboard-page__content-grid">
      <article class="dashboard-page__section content-panel">
        <div class="dashboard-page__section-header">
          <div>
            <h2>주간보고</h2>
            <p class="message-muted">최근 작성 내역</p>
          </div>
          <CommonBaseButton variant="secondary" to="/work/weekly-reports">
            이동
          </CommonBaseButton>
        </div>

        <p v-if="reportErrorMessage" class="message-error">
          {{ reportErrorMessage }}
        </p>
        <div v-else-if="recentWeeklyReports.length" class="dashboard-page__report-list">
          <article
            v-for="weeklyReport in recentWeeklyReports"
            :key="weeklyReport.uuid"
            class="dashboard-page__report-card"
          >
            <div class="dashboard-page__report-card-header">
              <strong>{{ weeklyReport.title }}</strong>
              <span>{{ formatDate(weeklyReport.weekEndDate, { month: 'numeric', day: 'numeric' }) }}</span>
            </div>
            <p>{{ getReportExcerpt(weeklyReport.content) }}</p>
          </article>
        </div>
        <p v-else class="message-muted">
          주간보고가 없습니다.
        </p>
      </article>

      <article class="dashboard-page__section content-panel">
        <div class="dashboard-page__section-header">
          <div>
            <h2>보고통계</h2>
            <p class="message-muted">{{ currentWeekLabel }}</p>
          </div>
        </div>

        <div class="dashboard-page__stat-grid">
          <div
            v-for="stat in reportStats"
            :key="stat.label"
            class="dashboard-page__stat-card"
          >
            <span>{{ stat.label }}</span>
            <strong>{{ stat.value }}</strong>
          </div>
        </div>
      </article>

      <article class="dashboard-page__section content-panel">
        <div class="dashboard-page__section-header">
          <div>
            <h2>활성화된 업무내역</h2>
            <p class="message-muted">최근 사용 기준 4건</p>
          </div>
          <CommonBaseButton variant="secondary" to="/work">
            전체
          </CommonBaseButton>
        </div>

        <p v-if="workErrorMessage" class="message-error">
          {{ workErrorMessage }}
        </p>
        <div v-else-if="activeWorkUnits.length" class="dashboard-page__work-list">
          <article
            v-for="workUnit in activeWorkUnits"
            :key="workUnit.workUnitUuid"
            class="dashboard-page__work-item"
          >
            <div class="dashboard-page__work-item-header">
              <strong>{{ workUnit.title }}</strong>
              <span>{{ getWorkStatusLabel(workUnit.status) }}</span>
            </div>
            <p>{{ workUnit.category || '카테고리 없음' }}</p>
            <small>사용 {{ workUnit.useCount }}회 · 최근 {{ formatDate(workUnit.lastUsedAt) }}</small>
          </article>
        </div>
        <p v-else class="message-muted">
          활성화된 업무가 없습니다.
        </p>
      </article>
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
            <span>등록일 {{ formatDateTime(selectedNotice.createdAt) }}</span>
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

.dashboard-page__summary-grid,
.dashboard-page__content-grid {
  display: grid;
  gap: 24px;
}

.dashboard-page__summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.dashboard-page__content-grid {
  grid-template-columns: 1.15fr 0.85fr 0.9fr;
  align-items: start;
}

.dashboard-page__section {
  padding: 24px;
}

.dashboard-page__section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.dashboard-page__section-header h2 {
  margin: 0;
  font-size: 1.05rem;
}

.dashboard-page__section-header p {
  margin: 6px 0 0;
}

.dashboard-page__compact-list,
.dashboard-page__report-list,
.dashboard-page__work-list {
  display: grid;
  gap: 12px;
}

.dashboard-page__compact-item,
.dashboard-page__report-card,
.dashboard-page__work-item {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.dashboard-page__compact-item {
  width: 100%;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.dashboard-page__compact-item--link {
  text-decoration: none;
}

.dashboard-page__compact-item strong,
.dashboard-page__report-card strong,
.dashboard-page__work-item strong {
  font-size: 0.96rem;
  line-height: 1.4;
}

.dashboard-page__compact-item span,
.dashboard-page__report-card span,
.dashboard-page__work-item p,
.dashboard-page__work-item small {
  color: var(--color-text-muted);
}

.dashboard-page__compact-item:hover,
.dashboard-page__compact-item--link:hover {
  border-color: rgba(147, 210, 255, 0.24);
}

.dashboard-page__compact-item:focus-visible {
  outline: 2px solid rgba(110, 193, 255, 0.24);
  outline-offset: 2px;
}

.dashboard-page__report-card-header,
.dashboard-page__work-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dashboard-page__report-card p,
.dashboard-page__work-item p,
.dashboard-page__work-item small {
  margin: 0;
  line-height: 1.5;
}

.dashboard-page__stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.dashboard-page__stat-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(147, 210, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.dashboard-page__stat-card span {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.dashboard-page__stat-card strong {
  font-size: 1.32rem;
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

@media (max-width: 1080px) {
  .dashboard-page__content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-page__summary-grid,
  .dashboard-page__stat-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-page__section-header {
    flex-direction: column;
    align-items: stretch;
  }

  .dashboard-page__compact-item,
  .dashboard-page__report-card-header,
  .dashboard-page__work-item-header {
    gap: 8px;
  }

  .dashboard-page__report-card-header,
  .dashboard-page__work-item-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
