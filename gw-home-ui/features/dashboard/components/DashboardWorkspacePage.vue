<script setup lang="ts">
import DashboardMyTodoSummary from '~/features/dashboard/components/DashboardMyTodoSummary.vue'
import DashboardWorkspaceSidePanel from '~/features/dashboard/components/DashboardWorkspaceSidePanel.vue'
import DashboardWorkspaceTaskTable from '~/features/dashboard/components/DashboardWorkspaceTaskTable.vue'
import NoticeDetailModal from '~/features/notice/components/NoticeDetailModal.vue'
import { useWorkTodoApi } from '~/features/work/api/work-todo.api'
import type { BoardSummary } from '~/types/api/board'
import type { NoticeDetail, NoticeSummary } from '~/types/api/notice'
import type { WorkTodo, WorkTodoTree, WorkUnit } from '~/features/work/types/work.types'
import type {
  DashboardIssueItem,
  DashboardMetricCard,
  DashboardScheduleItem,
  DashboardTaskRow,
  DashboardTone,
  DashboardUpdateItem
} from '~/features/dashboard/types/dashboard-workspace'
import { formatDate, isDateInCurrentWeek, sortByDateDesc } from '~/utils/date'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const { fetchBoardList } = useBoard()
const { fetchDashboardNotices, fetchNotice } = useNoticeApi()
const { fetchWorkUnitList } = useWorkUnitApi()
const { fetchWorkTodoTree } = useWorkTodoApi()

const recentBoards = ref<BoardSummary[]>([])
const recentNotices = ref<NoticeSummary[]>([])
const activeWorkUnits = ref<WorkUnit[]>([])
const todoTreeMap = ref<Record<string, WorkTodoTree>>({})
const selectedNotice = ref<NoticeDetail | null>(null)

const isWorkTreeLoading = ref(false)
const boardErrorMessage = ref('')
const noticeErrorMessage = ref('')
const workErrorMessage = ref('')
const detailErrorMessage = ref('')
const isDetailLoading = ref(false)

const selectedNoticeUuid = computed(() =>
  typeof route.query.noticeUuid === 'string' ? route.query.noticeUuid : ''
)


const periodFilter = ref<'all' | 'this-week'>('this-week')
const statusFilter = ref<'all' | 'IN_PROGRESS' | 'DONE' | 'AT_RISK'>('all')
const ownerFilter = ref<'ME'>('ME')

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

const [boardResult, noticeResult, workUnitResult] = await Promise.allSettled([
  fetchBoardList({
    page: 1,
    size: 4,
    sortBy: 'createdAt',
    sortDirection: 'DESC'
  }),
  fetchDashboardNotices(4),
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
        updatedAt: workUnit.updatedAt
      }
    })
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

  return [...noticeItems, ...boardItems].slice(0, 6)
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
    <section class="content-panel dashboard-page__hero">
      <div class="dashboard-page__hero-copy">
        <p class="dashboard-page__eyebrow">Workspace Dashboard</p>
        <h1>대시보드</h1>
        <p class="dashboard-page__hero-description">
          {{ authStore.currentUser?.nickname || authStore.currentUser?.loginId }} 님의 업무와 소식을 한눈에 확인할 수 있습니다.
        </p>
      </div>

      <div class="dashboard-page__hero-actions">
        <CommonBaseButton to="/work/todos">
          내 할 일 열기
        </CommonBaseButton>
        <CommonBaseButton variant="secondary" to="/work">
          업무 관리
        </CommonBaseButton>
      </div>
    </section>

    <div class="dashboard-page__todo-summaries">
      <DashboardMyTodoSummary />
    </div>

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
        :issues="blockerItems"
        :schedules="scheduleItems"
        :updates="updateItems"
        :update-error-message="noticeErrorMessage || boardErrorMessage"
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
  display: grid;
  gap: var(--section-gap);
}

.dashboard-page__todo-summaries { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--section-gap); }
@media (max-width: 767px) { .dashboard-page__todo-summaries { grid-template-columns: minmax(0, 1fr); } }

.dashboard-page__hero {
  padding: var(--panel-padding-large);
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
  color: var(--color-text-muted);
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
  gap: var(--section-gap);
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
  border: 1px solid var(--color-border);
  border-radius: var(--radius-small);
  background: var(--color-surface-muted);
  color: var(--color-text);
}

.dashboard-page__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--section-gap);
}

.dashboard-page__metric-card {
  padding: var(--panel-padding);
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
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.dashboard-page__metric-card--primary {
  border-color: var(--color-border);
}

.dashboard-page__metric-card--success {
  border-color: var(--color-success);
}

.dashboard-page__metric-card--warning {
  border-color: var(--color-warning);
}

.dashboard-page__metric-card--danger {
  border-color: var(--color-danger);
}

.dashboard-page__workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--section-gap);
  align-items: start;
}

@media (max-width: 1280px) {
  .dashboard-page__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

}

@media (max-width: 1535px) {
  .dashboard-page__workspace { grid-template-columns: minmax(0, 1fr); }
}

@media (max-width: 768px) {
  .dashboard-page {
    gap: 14px;
  }

  .dashboard-page__hero,
  .dashboard-page__toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 14px;
  }

  .dashboard-page__hero {
    padding: var(--mobile-panel-padding);
  }

  .dashboard-page__hero-copy {
    gap: 10px;
  }

  .dashboard-page__hero h1 {
    font-size: 1.55rem;
  }

  .dashboard-page__hero-description {
    line-height: 1.55;
  }

  .dashboard-page__hero-actions {
    gap: 10px;
  }

  .dashboard-page__hero-actions :deep(.base-button) {
    width: 100%;
  }

  .dashboard-page__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .dashboard-page__metric-card {
    padding: var(--mobile-panel-padding);
  }

  .dashboard-page__filter {
    min-width: 0;
    width: 100%;
  }

  .dashboard-page__filters {
    gap: 10px;
  }

  .dashboard-page__workspace {
    gap: 14px;
  }
}

</style>
