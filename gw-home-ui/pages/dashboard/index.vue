<script setup lang="ts">
import DashboardWorkspaceSidePanel from '~/features/dashboard/components/DashboardWorkspaceSidePanel.vue'
import DashboardWorkspaceTaskTable from '~/features/dashboard/components/DashboardWorkspaceTaskTable.vue'
import NoticeDetailModal from '~/features/notice/components/NoticeDetailModal.vue'
import { useWorkTodoApi } from '~/features/work/api/work-todo.api'
import type { BoardSummary } from '~/types/api/board'
import type { NoticeDetail, NoticeSummary } from '~/types/api/notice'
import type { WorkTodo, WorkTodoTree, DailyReport, WeeklyReport, WorkUnit } from '~/features/work/types/work.types'
import type {
  DashboardIssueItem,
  DashboardMetricCard,
  DashboardRecentReportItem,
  DashboardScheduleItem,
  DashboardTaskRow,
  DashboardTone,
  DashboardUpdateItem
} from '~/features/dashboard/types/dashboard-workspace'
import { formatDate, getWeekRange, isDateInCurrentWeek, sortByDateDesc } from '~/utils/date'

definePageMeta({
  middleware: 'auth'
})

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const { fetchBoardList } = useBoard()
const { fetchDashboardNotices, fetchNotice } = useNoticeApi()
const { fetchWeeklyReports } = useWeeklyReportApi()
const { fetchDailyReports } = useDailyReportApi()
const { fetchWorkUnitList } = useWorkUnitApi()
const { fetchWorkTodoTree } = useWorkTodoApi()

const recentBoards = ref<BoardSummary[]>([])
const recentNotices = ref<NoticeSummary[]>([])
const weeklyReports = ref<WeeklyReport[]>([])
const dailyReports = ref<DailyReport[]>([])
const activeWorkUnits = ref<WorkUnit[]>([])
const todoTreeMap = ref<Record<string, WorkTodoTree>>({})
const selectedNotice = ref<NoticeDetail | null>(null)

const isWorkTreeLoading = ref(false)
const boardErrorMessage = ref('')
const noticeErrorMessage = ref('')
const reportErrorMessage = ref('')
const workErrorMessage = ref('')
const detailErrorMessage = ref('')
const isDetailLoading = ref(false)

const selectedNoticeUuid = computed(() =>
  typeof route.query.noticeUuid === 'string' ? route.query.noticeUuid : ''
)

const currentWeek = getWeekRange()
const currentWeekLabel = `${formatDate(currentWeek.start, { month: 'numeric', day: 'numeric' })} - ${formatDate(currentWeek.end, { month: 'numeric', day: 'numeric' })}`

const periodFilter = ref<'all' | 'this-week'>('this-week')
const statusFilter = ref<'all' | 'IN_PROGRESS' | 'DONE' | 'AT_RISK'>('all')
const ownerFilter = ref<'ME'>('ME')

function getReportExcerpt(content: string | null | undefined): string {
  const normalized = content?.replace(/\s+/g, ' ').trim()
  return normalized ? normalized.slice(0, 92) : '작성된 요약이 아직 없습니다.'
}

function getWorkStatusLabel(status: WorkUnit['status'], delayedCount: number): string {
  if (delayedCount > 0) {
    return '리스크'
  }

  if (status === 'DONE') {
    return '완료'
  }

  if (status === 'ON_HOLD') {
    return '보류'
  }

  return '진행중'
}

function getWorkStatusTone(status: WorkUnit['status'], delayedCount: number): DashboardTone {
  if (delayedCount > 0) {
    return 'danger'
  }

  if (status === 'DONE') {
    return 'success'
  }

  if (status === 'ON_HOLD') {
    return 'warning'
  }

  return 'primary'
}

function getTodoStatusLabel(status: WorkTodo['status']): string {
  if (status === 'DONE') {
    return '완료'
  }

  if (status === 'DELAYED') {
    return '지연'
  }

  if (status === 'IN_PROGRESS') {
    return '진행중'
  }

  return '대기'
}

function flattenTodoTree(todos: WorkTodo[]): WorkTodo[] {
  return todos.flatMap(todo => [todo, ...flattenTodoTree(todo.children)])
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
    size: 20
  }),
  fetchWorkUnitList({
    useYn: 'Y',
    sort: 'updated'
  })
])

if (boardResult.status === 'fulfilled') {
  recentBoards.value = boardResult.value.content
} else {
  boardErrorMessage.value = '게시글 정보를 불러오지 못했습니다.'
}

if (noticeResult.status === 'fulfilled') {
  recentNotices.value = noticeResult.value
} else {
  noticeErrorMessage.value = '공지사항 정보를 불러오지 못했습니다.'
}

if (weeklyReportResult.status === 'fulfilled') {
  weeklyReports.value = sortByDateDesc(weeklyReportResult.value, item => item.weekEndDate)
} else {
  reportErrorMessage.value = '주간보고 정보를 불러오지 못했습니다.'
}

if (dailyReportResult.status === 'fulfilled') {
  dailyReports.value = sortByDateDesc(dailyReportResult.value.content, item => item.reportDate)
} else if (!reportErrorMessage.value) {
  reportErrorMessage.value = '일일보고 정보를 불러오지 못했습니다.'
}

if (workUnitResult.status === 'fulfilled') {
  activeWorkUnits.value = sortByDateDesc(
    workUnitResult.value.filter(item => item.useYn === 'Y'),
    item => item.updatedAt
  )

  isWorkTreeLoading.value = true

  const todoTreeResults = await Promise.allSettled(
    activeWorkUnits.value.map(workUnit => fetchWorkTodoTree(workUnit.workUnitUuid))
  )

  todoTreeResults.forEach((result, index) => {
    const workUnit = activeWorkUnits.value[index]

    if (result.status === 'fulfilled' && workUnit) {
      todoTreeMap.value[workUnit.workUnitUuid] = result.value
    }
  })

  if (todoTreeResults.some(result => result.status === 'rejected')) {
    workErrorMessage.value = '일부 업무의 세부 작업 정보를 불러오지 못했습니다.'
  }

  isWorkTreeLoading.value = false
} else {
  workErrorMessage.value = '업무 정보를 불러오지 못했습니다.'
}

const latestDailyReportByWorkUnit = computed(() => {
  const reportEntries = new Map<string, DailyReport>()

  for (const report of dailyReports.value) {
    for (const workUnit of report.workUnits) {
      if (!reportEntries.has(workUnit.workUnitUuid)) {
        reportEntries.set(workUnit.workUnitUuid, report)
      }
    }
  }

  return reportEntries
})

const filteredWorkUnits = computed(() => {
  return activeWorkUnits.value.filter((workUnit) => {
    const summary = todoTreeMap.value[workUnit.workUnitUuid]?.summary
    const isRiskTask = workUnit.status === 'ON_HOLD' || (summary?.delayedCount ?? 0) > 0
    const matchesPeriod = periodFilter.value === 'all' || isDateInCurrentWeek(workUnit.updatedAt)
    const matchesStatus = statusFilter.value === 'all'
      || (statusFilter.value === 'AT_RISK' ? isRiskTask : workUnit.status === statusFilter.value)

    return matchesPeriod && matchesStatus
  })
})

const metricCards = computed<DashboardMetricCard[]>(() => {
  const delayedWorkCount = filteredWorkUnits.value.filter((workUnit) => {
    const summary = todoTreeMap.value[workUnit.workUnitUuid]?.summary
    return workUnit.status === 'ON_HOLD' || (summary?.delayedCount ?? 0) > 0
  }).length
  const thisWeekWeeklyReportCount = weeklyReports.value.filter(report => isDateInCurrentWeek(report.weekEndDate)).length
  const inProgressCount = filteredWorkUnits.value.filter(workUnit => workUnit.status === 'IN_PROGRESS').length
  const doneCount = filteredWorkUnits.value.filter(workUnit => workUnit.status === 'DONE').length

  return [
    {
      key: 'in-progress',
      label: '진행 중 업무',
      value: `${inProgressCount}건`,
      hint: '현재 활성 업무 기준',
      tone: 'primary'
    },
    {
      key: 'done',
      label: '완료 업무',
      value: `${doneCount}건`,
      hint: '현재 필터 범위 기준',
      tone: 'success'
    },
    {
      key: 'delayed',
      label: '지연 / 보류 업무',
      value: `${delayedWorkCount}건`,
      hint: '지연 TODO 또는 보류 업무',
      tone: delayedWorkCount > 0 ? 'danger' : 'warning'
    },
    {
      key: 'weekly-reports',
      label: '이번 주 보고',
      value: `${thisWeekWeeklyReportCount}건`,
      hint: currentWeekLabel,
      tone: 'neutral'
    }
  ]
})

const taskRows = computed<DashboardTaskRow[]>(() => {
  return filteredWorkUnits.value
    .slice(0, 8)
    .map((workUnit) => {
      const summary = todoTreeMap.value[workUnit.workUnitUuid]?.summary
      const delayedCount = summary?.delayedCount ?? 0

      return {
        workUnitUuid: workUnit.workUnitUuid,
        title: workUnit.title,
        category: workUnit.category,
        statusLabel: getWorkStatusLabel(workUnit.status, delayedCount),
        statusTone: getWorkStatusTone(workUnit.status, delayedCount),
        progressRate: summary?.progressRate ?? 0,
        delayedCount,
        latestReportDate: latestDailyReportByWorkUnit.value.get(workUnit.workUnitUuid)?.reportDate ?? null,
        updatedAt: workUnit.updatedAt
      }
    })
})

const recentReports = computed<DashboardRecentReportItem[]>(() => {
  return weeklyReports.value.slice(0, 4).map(report => ({
    uuid: report.uuid,
    title: report.title,
    weekLabel: `${formatDate(report.weekStartDate, { month: 'numeric', day: 'numeric' })} - ${formatDate(report.weekEndDate, { month: 'numeric', day: 'numeric' })}`,
    openLabel: report.openYn === 'Y' ? '공개 보고' : '비공개 보고',
    publishedLabel: report.publishedAt ? `게시 ${formatDate(report.publishedAt)}` : `수정 ${formatDate(report.updatedAt)}`,
    excerpt: getReportExcerpt(report.content)
  }))
})

const blockerItems = computed<DashboardIssueItem[]>(() => {
  return activeWorkUnits.value
    .map((workUnit) => {
      const summary = todoTreeMap.value[workUnit.workUnitUuid]?.summary
      const delayedCount = summary?.delayedCount ?? 0

      if (workUnit.status !== 'ON_HOLD' && delayedCount === 0) {
        return null
      }

      return {
        key: workUnit.workUnitUuid,
        title: workUnit.title,
        description: workUnit.status === 'ON_HOLD'
          ? '업무 상태가 보류로 표시되어 있어 우선 확인이 필요합니다.'
          : `지연된 세부 작업 ${delayedCount}건이 남아 있습니다.`,
        meta: workUnit.category ? `분류 ${workUnit.category}` : '분류 없음',
        badge: workUnit.status === 'ON_HOLD' ? '보류' : `${delayedCount}건 지연`,
        tone: workUnit.status === 'ON_HOLD' ? 'warning' : 'danger',
        to: `/work/todos/${workUnit.workUnitUuid}`
      }
    })
    .filter((item): item is DashboardIssueItem => item !== null)
    .slice(0, 4)
})

const scheduleItems = computed<DashboardScheduleItem[]>(() => {
  const upcomingTodos = activeWorkUnits.value.flatMap((workUnit) => {
    const todoTree = todoTreeMap.value[workUnit.workUnitUuid]

    if (!todoTree) {
      return []
    }

    return flattenTodoTree(todoTree.todos)
      .filter(todo => Boolean(todo.dueDate) && todo.status !== 'DONE')
      .map((todo) => ({
        workUnitUuid: workUnit.workUnitUuid,
        workUnitTitle: workUnit.title,
        todo
      }))
  })

  return upcomingTodos
    .sort((left, right) => new Date(left.todo.dueDate ?? '').getTime() - new Date(right.todo.dueDate ?? '').getTime())
    .slice(0, 5)
    .map(item => ({
      key: `${item.workUnitUuid}-${item.todo.todoUuid}`,
      title: item.todo.title,
      workUnitTitle: item.workUnitTitle,
      dueDateLabel: `마감 ${formatDate(item.todo.dueDate)}`,
      statusLabel: getTodoStatusLabel(item.todo.status),
      tone: item.todo.status === 'DELAYED' ? 'danger' : item.todo.status === 'IN_PROGRESS' ? 'primary' : 'neutral',
      to: `/work/todos/${item.workUnitUuid}`
    }))
})

const updateItems = computed<DashboardUpdateItem[]>(() => {
  const noticeItems: DashboardUpdateItem[] = recentNotices.value.map(notice => ({
    key: `notice-${notice.noticeUuid}`,
    typeLabel: '공지',
    title: notice.title,
    meta: `${notice.createdBy} · ${formatDate(notice.createdAt)}`,
    tone: 'warning',
    noticeUuid: notice.noticeUuid
  }))
  const boardItems: DashboardUpdateItem[] = recentBoards.value.map(board => ({
    key: `board-${board.boardPostUuid}`,
    typeLabel: '게시글',
    title: board.title,
    meta: `${board.author} · 댓글 ${board.commentCount} · ${formatDate(board.createdAt)}`,
    tone: 'primary',
    to: `/board/${board.boardPostUuid}`
  }))
  const reportItems: DashboardUpdateItem[] = dailyReports.value.slice(0, 3).map(report => ({
    key: `daily-report-${report.uuid}`,
    typeLabel: '일일보고',
    title: report.workUnits.map(workUnit => workUnit.title).slice(0, 2).join(', ') || '작성된 일일보고',
    meta: `${formatDate(report.reportDate)} · 업무 ${report.workUnits.length}건`,
    tone: 'success',
    to: '/work/daily-reports'
  }))

  return [...noticeItems, ...boardItems, ...reportItems].slice(0, 6)
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
  <main class="dashboard-page">
    <section class="content-panel dashboard-page__hero">
      <div class="dashboard-page__hero-copy">
        <p class="dashboard-page__eyebrow">Workspace Dashboard</p>
        <h1>업무 현황 대시보드</h1>
        <p class="dashboard-page__hero-description">
          {{ authStore.currentUser?.nickname || authStore.currentUser?.loginId }} 님의 업무, 이슈, 보고를 한눈에 확인할 수 있습니다.
        </p>
      </div>

      <div class="dashboard-page__hero-actions">
        <CommonBaseButton to="/work/daily-reports/create">
          오늘 보고 작성
        </CommonBaseButton>
        <CommonBaseButton variant="secondary" to="/work/weekly-reports">
          주간보고 열기
        </CommonBaseButton>
      </div>
    </section>

    <section class="dashboard-page__toolbar">
      <div class="dashboard-page__toolbar-title">
        <p class="dashboard-page__eyebrow">Filters</p>
        <h2 class="section-title">기간 / 상태 / 담당자 필터</h2>
      </div>

      <div class="dashboard-page__filters">
        <label class="dashboard-page__filter">
          <span>기간</span>
          <select v-model="periodFilter" class="dashboard-page__filter-select">
            <option value="this-week">이번 주</option>
            <option value="all">전체</option>
          </select>
        </label>

        <label class="dashboard-page__filter">
          <span>상태</span>
          <select v-model="statusFilter" class="dashboard-page__filter-select">
            <option value="all">전체</option>
            <option value="IN_PROGRESS">진행중</option>
            <option value="DONE">완료</option>
            <option value="AT_RISK">리스크</option>
          </select>
        </label>

        <label class="dashboard-page__filter">
          <span>담당자</span>
          <select v-model="ownerFilter" class="dashboard-page__filter-select">
            <option value="ME">내 업무</option>
          </select>
        </label>
      </div>
    </section>

    <section class="dashboard-page__metrics">
      <article
        v-for="metric in metricCards"
        :key="metric.key"
        class="content-panel dashboard-page__metric-card"
        :class="`dashboard-page__metric-card--${metric.tone}`"
      >
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <p>{{ metric.hint }}</p>
      </article>
    </section>

    <section class="dashboard-page__workspace">
      <DashboardWorkspaceTaskTable
        :rows="taskRows"
        :is-loading="isWorkTreeLoading"
        :error-message="workErrorMessage"
      />

      <DashboardWorkspaceSidePanel
        :reports="recentReports"
        :issues="blockerItems"
        :schedules="scheduleItems"
        :updates="updateItems"
        :report-error-message="reportErrorMessage || noticeErrorMessage || boardErrorMessage"
        @open-notice="openNoticeDetail"
      />
    </section>

    <NoticeDetailModal
      :visible="Boolean(selectedNoticeUuid)"
      :notice="selectedNotice"
      :is-loading="isDetailLoading"
      :error-message="detailErrorMessage"
      @close="closeNoticeDetail"
    />
  </main>
</template>

<style scoped>
.dashboard-page {
  width: 100%;
  padding: 24px;
  display: grid;
  gap: 20px;
}

.dashboard-page__hero {
  padding: 28px;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.dashboard-page__hero-copy {
  max-width: 760px;
  display: grid;
  gap: 12px;
}

.dashboard-page__eyebrow {
  margin: 0;
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dashboard-page__hero h1 {
  margin: 0;
  font-size: clamp(1.7rem, 3vw, 2.4rem);
  line-height: 1.12;
}

.dashboard-page__hero-description {
  margin: 0;
  color: rgba(232, 244, 255, 0.82);
  line-height: 1.7;
}

.dashboard-page__hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.dashboard-page__toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
}

.dashboard-page__toolbar-title {
  display: grid;
  gap: 8px;
}

.dashboard-page__filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.dashboard-page__filter {
  min-width: 160px;
  display: grid;
  gap: 8px;
}

.dashboard-page__filter span {
  color: var(--color-text-muted);
  font-size: 0.84rem;
  font-weight: 700;
}

.dashboard-page__filter-select {
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: var(--radius-small);
  background: rgba(10, 22, 40, 0.7);
  color: var(--color-text);
}

.dashboard-page__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.dashboard-page__metric-card {
  padding: 20px;
  display: grid;
  gap: 8px;
}

.dashboard-page__metric-card span {
  color: var(--color-text-muted);
  font-size: 0.86rem;
  font-weight: 700;
}

.dashboard-page__metric-card strong {
  font-size: clamp(1.5rem, 2.2vw, 2rem);
}

.dashboard-page__metric-card p {
  margin: 0;
  color: rgba(232, 244, 255, 0.72);
  font-size: 0.88rem;
}

.dashboard-page__metric-card--primary {
  border-color: rgba(110, 193, 255, 0.24);
}

.dashboard-page__metric-card--success {
  border-color: rgba(84, 214, 160, 0.24);
}

.dashboard-page__metric-card--warning {
  border-color: rgba(255, 188, 92, 0.24);
}

.dashboard-page__metric-card--danger {
  border-color: rgba(224, 92, 92, 0.24);
}

.dashboard-page__workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 0.9fr);
  gap: 20px;
  align-items: start;
}

@media (max-width: 1280px) {
  .dashboard-page__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-page__workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-page {
    padding: 16px;
  }

  .dashboard-page__hero,
  .dashboard-page__toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .dashboard-page__metrics {
    grid-template-columns: 1fr;
  }

  .dashboard-page__filter {
    min-width: 0;
    width: 100%;
  }
}
</style>
