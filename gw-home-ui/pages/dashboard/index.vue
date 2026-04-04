<script setup lang="ts">
import DashboardBoardSection from '~/features/dashboard/components/DashboardBoardSection.vue'
import DashboardNoticeSection from '~/features/dashboard/components/DashboardNoticeSection.vue'
import DashboardStatsSection from '~/features/dashboard/components/DashboardStatsSection.vue'
import DashboardWeeklyReportSection from '~/features/dashboard/components/DashboardWeeklyReportSection.vue'
import DashboardWorkSection from '~/features/dashboard/components/DashboardWorkSection.vue'
import NoticeDetailModal from '~/features/notice/components/NoticeDetailModal.vue'
import type { BoardSummary } from '~/types/api/board'
import type { NoticeDetail, NoticeSummary } from '~/types/api/notice'
import type { DailyReport, WeeklyReport, WorkUnit } from '~/features/work/types/work.types'
import { getWeekRange, isDateInCurrentWeek, sortByDateDesc, formatDate } from '~/utils/date'

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
      <DashboardNoticeSection
        :notices="recentNotices"
        :error-message="noticeErrorMessage"
        @open="openNoticeDetail"
      />
      <DashboardBoardSection :boards="recentBoards" />
    </section>

    <section class="dashboard-page__content-grid">
      <DashboardWeeklyReportSection
        :reports="recentWeeklyReports"
        :error-message="reportErrorMessage"
        :get-report-excerpt="getReportExcerpt"
      />
      <DashboardStatsSection :current-week-label="currentWeekLabel" :stats="reportStats" />
      <DashboardWorkSection
        :work-units="activeWorkUnits"
        :error-message="workErrorMessage"
        :get-work-status-label="getWorkStatusLabel"
      />
    </section>

    <NoticeDetailModal
      :visible="isDetailModalVisible"
      :notice="selectedNotice"
      :is-loading="isDetailLoading"
      :error-message="detailErrorMessage"
      @close="closeNoticeDetail"
    />
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

@media (max-width: 1080px) {
  .dashboard-page__content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-page__summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
