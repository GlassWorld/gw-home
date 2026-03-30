<script setup lang="ts">
import type { DailyReport, DailyReportMissing, DailyReportWorkUnit, UpdateDailyReportPayload, WorkUnitOption } from '~/types/work'

definePageMeta({
  middleware: 'auth'
})

interface DailyReportFormState {
  reportDate: string
  workSummary: string
  issueNote: string
}

const { fetchDailyReports, createDailyReport, updateDailyReport, fetchMissingDailyReports } = useDailyReportApi()
const { fetchWorkUnitOptions } = useWorkUnitApi()
const { showToast } = useToast()

function formatDateInput(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getDaysAgoInput(days: number): string {
  const targetDate = new Date()
  targetDate.setDate(targetDate.getDate() - days)
  return formatDateInput(targetDate)
}

function formatDateTime(value: string): string {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function formatDate(value: string): string {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(toLocalDate(value))
}

function formatDateWithWeekday(value: string): string {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'short'
  }).format(toLocalDate(value))
}

function toLocalDate(value?: string | null): Date {
  if (!value) {
    return new Date(1970, 0, 1)
  }

  const [year = 1970, month = 1, day = 1] = value.split('-').map((item) => Number(item))
  return new Date(year, month - 1, day)
}

function isWeekend(value: string): boolean {
  const date = toLocalDate(value)
  const day = date.getDay()
  return day === 0 || day === 6
}

function isFutureDate(value: string): boolean {
  const date = toLocalDate(value)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() > today.getTime()
}

function formatMissingDateLabel(value: string): string {
  const weekday = new Intl.DateTimeFormat('ko-KR', { weekday: 'short' }).format(toLocalDate(value))
  return `${formatDate(value)} (${weekday})`
}

function getWorkUnitStatusLabel(status: WorkUnitOption['status']): string {
  if (status === 'DONE') {
    return '완료'
  }

  if (status === 'ON_HOLD') {
    return '보류'
  }

  return '진행중'
}

function extractSection(note: string | null | undefined, heading: string): string {
  if (!note) {
    return ''
  }

  const escapedHeading = heading.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const pattern = new RegExp(`\\[${escapedHeading}\\]\\n([\\s\\S]*?)(?=\\n\\n\\[[^\\]]+\\]\\n|$)`)
  const match = note.match(pattern)
  return match?.[1]?.trim() ?? ''
}

function removeSection(note: string | null | undefined, heading: string): string {
  if (!note) {
    return ''
  }

  const escapedHeading = heading.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const pattern = new RegExp(`\\n?\\[${escapedHeading}\\]\\n([\\s\\S]*?)(?=\\n\\n\\[[^\\]]+\\]\\n|$)`, 'g')
  return note.replace(pattern, '').trim()
}

function buildNotePayload(formState: DailyReportFormState): string {
  return formState.issueNote.trim()
}

function summarizeReport(report: DailyReport): string {
  const summarySource = report.content || report.note || report.workUnits.map((workUnit) => workUnit.title).join(', ')
  return summarySource.replace(/\s+/g, ' ').trim()
}

const filters = reactive({
  dateFrom: getDaysAgoInput(14),
  dateTo: formatDateInput(new Date()),
  keyword: '',
  page: 1,
  size: 10
})

const formState = reactive<DailyReportFormState>({
  reportDate: formatDateInput(new Date()),
  workSummary: '',
  issueNote: ''
})

const editingDailyReportUuid = ref('')
const isFormVisible = ref(false)
const isDetailVisible = ref(false)
const isMissingPanelVisible = ref(false)
const isLoading = ref(false)
const isSubmitting = ref(false)
const isTaskPanelCollapsed = ref(false)
const isHistoryPanelCollapsed = ref(false)
const viewportWidth = ref<number>(Number.POSITIVE_INFINITY)
const isWorkUnitLoading = ref(false)
const isHistoryLoading = ref(false)
const workUnitSearchKeyword = ref('')
const activeTaskUuid = ref('')
const selectedTaskUuids = ref<string[]>([])
const selectedTaskSnapshotMap = ref<Record<string, DailyReportWorkUnit>>({})
const historyPreviewReportUuid = ref('')
const dailyReportPage = ref({
  content: [] as DailyReport[],
  page: 1,
  size: 10,
  totalCount: 0,
  totalPages: 0
})
const selectedDetailReport = ref<DailyReport | null>(null)
const missingDailyReports = ref<DailyReportMissing[]>([])
const workUnitOptions = ref<WorkUnitOption[]>([])
const recentDailyReports = ref<DailyReport[]>([])

const visibleMissingDailyReports = computed(() =>
  missingDailyReports.value.filter((item) => !isWeekend(item.reportDate) && !isFutureDate(item.reportDate))
)

const summary = computed(() => ({
  missingCount: visibleMissingDailyReports.value.length,
  totalCount: Number.isFinite(dailyReportPage.value.totalCount) ? dailyReportPage.value.totalCount : 0
}))

const filteredWorkUnitOptions = computed(() => {
  const keyword = workUnitSearchKeyword.value.trim().toLowerCase()

  if (!keyword) {
    return workUnitOptions.value
  }

  return workUnitOptions.value.filter((workUnit) =>
    [workUnit.title, workUnit.category ?? '', getWorkUnitStatusLabel(workUnit.status)]
      .join(' ')
      .toLowerCase()
      .includes(keyword)
  )
})

function buildSelectedTaskSnapshotMap(workUnits: DailyReportWorkUnit[]): Record<string, DailyReportWorkUnit> {
  return workUnits.reduce<Record<string, DailyReportWorkUnit>>((snapshotMap, workUnit) => {
    snapshotMap[workUnit.workUnitUuid] = workUnit
    return snapshotMap
  }, {})
}

const selectedWorkUnits = computed(() => {
  const workUnitMap = new Map(workUnitOptions.value.map((workUnit) => [workUnit.workUnitUuid, workUnit]))
  return selectedTaskUuids.value
    .map((workUnitUuid) => {
      const workUnit = workUnitMap.get(workUnitUuid)
      if (workUnit) {
        return {
          workUnitUuid: workUnit.workUnitUuid,
          title: workUnit.title,
          category: workUnit.category
        }
      }

      return selectedTaskSnapshotMap.value[workUnitUuid] ?? null
    })
    .filter((workUnit): workUnit is DailyReportWorkUnit => Boolean(workUnit))
})

const activeTask = computed(() => {
  const workUnit = workUnitOptions.value.find((item) => item.workUnitUuid === activeTaskUuid.value)

  if (workUnit) {
    return {
      workUnitUuid: workUnit.workUnitUuid,
      title: workUnit.title,
      category: workUnit.category
    }
  }

  return selectedTaskSnapshotMap.value[activeTaskUuid.value] ?? null
})

const historyReports = computed(() => {
  if (!activeTaskUuid.value) {
    return []
  }

  return recentDailyReports.value
    .filter((report) => report.workUnits.some((workUnit) => workUnit.workUnitUuid === activeTaskUuid.value))
    .sort((first, second) => second.reportDate.localeCompare(first.reportDate))
})

const historyPreviewReport = computed(() => {
  if (!historyReports.value.length) {
    return null
  }

  return historyReports.value.find((report) => report.uuid === historyPreviewReportUuid.value) ?? historyReports.value.at(0) ?? null
})

const dailyReportContent = computed(() => {
  return formState.workSummary.trim()
})

const isDesktopModalLayout = computed(() => viewportWidth.value > 1100)

const modalLayoutColumns = computed(() => {
  const taskColumn = isTaskPanelCollapsed.value ? '44px' : '280px'
  const historyColumn = isHistoryPanelCollapsed.value ? '44px' : 'minmax(0, 4fr)'
  const editorColumn = isHistoryPanelCollapsed.value ? 'minmax(0, 1fr)' : 'minmax(0, 6fr)'

  return `${taskColumn} ${historyColumn} ${editorColumn}`
})

const modalLayoutStyle = computed(() =>
  isDesktopModalLayout.value
    ? { gridTemplateColumns: modalLayoutColumns.value }
    : {}
)

function syncViewportWidth() {
  if (!import.meta.client) {
    return
  }

  viewportWidth.value = window.innerWidth

  if (viewportWidth.value <= 1100) {
    isTaskPanelCollapsed.value = false
    isHistoryPanelCollapsed.value = false
  }
}

const normalizedSelectedTaskUuids = computed(() =>
  Array.from(new Set(
    selectedTaskUuids.value
      .filter(Boolean)
  ))
)

function resetForm() {
  editingDailyReportUuid.value = ''
  activeTaskUuid.value = ''
  selectedTaskUuids.value = []
  selectedTaskSnapshotMap.value = {}
  historyPreviewReportUuid.value = ''
  workUnitSearchKeyword.value = ''
  formState.reportDate = formatDateInput(new Date())
  formState.workSummary = ''
  formState.issueNote = ''
  isTaskPanelCollapsed.value = false
  isHistoryPanelCollapsed.value = false
}

function selectTask(workUnitUuid: string) {
  activeTaskUuid.value = workUnitUuid
  historyPreviewReportUuid.value = ''
}

function setTaskSelection(workUnitUuid: string, checked: boolean) {
  if (!checked) {
    selectedTaskUuids.value = selectedTaskUuids.value.filter((uuid) => uuid !== workUnitUuid)
    if (activeTaskUuid.value === workUnitUuid) {
      activeTaskUuid.value = selectedTaskUuids.value[0] ?? ''
      historyPreviewReportUuid.value = ''
    }
    return
  }

  const selectedWorkUnit = workUnitOptions.value.find((workUnit) => workUnit.workUnitUuid === workUnitUuid)
  if (selectedWorkUnit) {
    selectedTaskSnapshotMap.value = {
      ...selectedTaskSnapshotMap.value,
      [workUnitUuid]: {
        workUnitUuid: selectedWorkUnit.workUnitUuid,
        title: selectedWorkUnit.title,
        category: selectedWorkUnit.category
      }
    }
  }

  selectedTaskUuids.value = [...selectedTaskUuids.value, workUnitUuid]
  if (!activeTaskUuid.value) {
    activeTaskUuid.value = workUnitUuid
  }
}

function startCreateMode() {
  resetForm()
  isDetailVisible.value = false
  selectedDetailReport.value = null
  isFormVisible.value = true
  void Promise.all([loadWorkUnits(), loadRecentDailyReports()])
}

function toggleTaskPanel() {
  isTaskPanelCollapsed.value = !isTaskPanelCollapsed.value
}

function toggleHistoryPanel() {
  isHistoryPanelCollapsed.value = !isHistoryPanelCollapsed.value
}

function startEditMode(report: DailyReport) {
  const legacyIssueNote = report.note ?? extractSection(report.content, '이슈 / 특이사항')

  isDetailVisible.value = false
  selectedDetailReport.value = null
  editingDailyReportUuid.value = report.uuid
  workUnitSearchKeyword.value = ''
  selectedTaskSnapshotMap.value = buildSelectedTaskSnapshotMap(report.workUnits)
  selectedTaskUuids.value = report.workUnits.map((workUnit) => workUnit.workUnitUuid)
  activeTaskUuid.value = selectedTaskUuids.value[0] ?? ''
  formState.reportDate = report.reportDate
  formState.workSummary = removeSection(report.content, '이슈 / 특이사항')
  formState.issueNote = legacyIssueNote ?? ''
  isFormVisible.value = true
  historyPreviewReportUuid.value = report.uuid
  void Promise.all([loadWorkUnits(), loadRecentDailyReports()])
}

function openDetailMode(report: DailyReport) {
  selectedDetailReport.value = report
  isDetailVisible.value = true
}

function closeDetailModal() {
  isDetailVisible.value = false
  selectedDetailReport.value = null
}

function closeFormModal() {
  isFormVisible.value = false
  resetForm()
}

function applyHistoryDraft(report: DailyReport) {
  formState.workSummary = removeSection(report.content, '이슈 / 특이사항') || summarizeReport(report)
  formState.issueNote = report.note ?? extractSection(report.content, '이슈 / 특이사항')
  historyPreviewReportUuid.value = report.uuid
}

async function loadWorkUnits() {
  isWorkUnitLoading.value = true

  try {
    workUnitOptions.value = await fetchWorkUnitOptions(true)

    if (!activeTaskUuid.value && selectedTaskUuids.value.length) {
      activeTaskUuid.value = selectedTaskUuids.value[0] ?? ''
    }
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    workUnitOptions.value = []
    showToast(fetchError.data?.message ?? '업무등록 목록을 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isWorkUnitLoading.value = false
  }
}

async function loadRecentDailyReports() {
  isHistoryLoading.value = true

  try {
    const response = await fetchDailyReports({
      dateFrom: getDaysAgoInput(90),
      dateTo: formatDateInput(new Date()),
      page: 1,
      size: 100
    })

    recentDailyReports.value = response.content
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    recentDailyReports.value = []
    showToast(fetchError.data?.message ?? '이전 일일보고를 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isHistoryLoading.value = false
  }
}

async function loadDailyReports() {
  isLoading.value = true

  try {
    dailyReportPage.value = await fetchDailyReports({
      dateFrom: filters.dateFrom,
      dateTo: filters.dateTo,
      keyword: filters.keyword.trim() || undefined,
      page: filters.page,
      size: filters.size
    })
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    dailyReportPage.value.content = []
    showToast(fetchError.data?.message ?? '일일보고 목록을 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isLoading.value = false
  }
}

async function loadMissingDailyReports() {
  try {
    missingDailyReports.value = await fetchMissingDailyReports({
      dateFrom: filters.dateFrom,
      dateTo: filters.dateTo
    })
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    missingDailyReports.value = []
    showToast(fetchError.data?.message ?? '누락 현황을 불러오지 못했습니다.', { variant: 'error' })
  }
}

async function reloadAll() {
  await Promise.all([loadDailyReports(), loadMissingDailyReports()])
}

async function handleSubmit() {
  if (isSubmitting.value) {
    return
  }

  if (!formState.reportDate) {
    showToast('작성일자를 선택해주세요.', { variant: 'error' })
    return
  }

  if (!formState.workSummary.trim() && !formState.issueNote.trim()) {
    showToast('오늘 수행 내용 또는 이슈를 입력해주세요.', { variant: 'error' })
    return
  }

  isSubmitting.value = true

  try {
    const payloadNote = buildNotePayload(formState)
    const selectedWorkUnitUuids = [...normalizedSelectedTaskUuids.value]

    if (editingDailyReportUuid.value) {
      const payload: UpdateDailyReportPayload = {
        workUnitUuids: selectedWorkUnitUuids,
        note: payloadNote,
        content: dailyReportContent.value
      }
      const updatedReport = await updateDailyReport(editingDailyReportUuid.value, payload)
      if (selectedWorkUnitUuids.length > 0 && updatedReport.workUnits.length === 0) {
        throw new Error('선택한 업무가 저장되지 않았습니다. 다시 시도해주세요.')
      }
      showToast('일일보고를 수정했습니다.', { variant: 'success' })
    } else {
      const createdReport = await createDailyReport({
        reportDate: formState.reportDate,
        workUnitUuids: selectedWorkUnitUuids,
        note: payloadNote,
        content: dailyReportContent.value
      })
      if (selectedWorkUnitUuids.length > 0 && createdReport.workUnits.length === 0) {
        throw new Error('선택한 업무가 저장되지 않았습니다. 다시 시도해주세요.')
      }
      showToast('일일보고를 저장했습니다.', { variant: 'success' })
    }

    isFormVisible.value = false
    resetForm()
    filters.page = 1
    await Promise.all([reloadAll(), loadRecentDailyReports()])
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    const errorMessage = fetchError.data?.message ?? (error instanceof Error ? error.message : '일일보고 저장에 실패했습니다.')
    showToast(errorMessage, { variant: 'error' })
  } finally {
    isSubmitting.value = false
  }
}

async function handleSearch() {
  filters.page = 1
  await reloadAll()
}

async function movePage(nextPage: number) {
  if (nextPage < 1 || nextPage > Math.max(dailyReportPage.value.totalPages, 1) || nextPage === filters.page) {
    return
  }

  filters.page = nextPage
  await loadDailyReports()
}

watch(historyReports, (nextReports) => {
  if (!nextReports.length) {
    historyPreviewReportUuid.value = ''
    return
  }

  if (!nextReports.some((report) => report.uuid === historyPreviewReportUuid.value)) {
    historyPreviewReportUuid.value = nextReports.at(0)?.uuid ?? ''
  }
})

onMounted(() => {
  syncViewportWidth()
  window.addEventListener('resize', syncViewportWidth)
})

onBeforeUnmount(() => {
  if (!import.meta.client) {
    return
  }

  window.removeEventListener('resize', syncViewportWidth)
})

await reloadAll()
</script>

<template>
  <main class="page-container daily-report-page">
    <section class="content-panel daily-report-page__hero">
      <div class="daily-report-page__hero-header">
        <div>
          <p class="daily-report-page__eyebrow">Daily Report</p>
          <h1 class="section-title">일일보고</h1>
          <p class="section-description">
            개인용 일일보고를 날짜별로 기록하고, 누락 여부를 함께 확인합니다.
          </p>
        </div>

        <div class="daily-report-page__hero-side">
          <CommonBaseButton @click="startCreateMode">
            일일보고 작성
          </CommonBaseButton>

          <div class="daily-report-page__summary" role="status" aria-label="일일보고 요약">
            <span>조회 결과 <strong>{{ summary.totalCount }}</strong>건</span>
            <span class="daily-report-page__summary-divider">|</span>
            <span>누락 <strong>{{ summary.missingCount }}</strong>일</span>
          </div>
        </div>
      </div>
    </section>

    <section class="content-panel daily-report-page__panel">
      <div class="daily-report-page__panel-header">
        <span v-if="isLoading">불러오는 중...</span>
      </div>

      <form class="daily-report-page__filters" @submit.prevent="handleSearch">
        <label>
          <span>시작일</span>
          <input v-model="filters.dateFrom" class="input-field" type="date">
        </label>
        <label>
          <span>종료일</span>
          <input v-model="filters.dateTo" class="input-field" type="date">
        </label>
        <label class="daily-report-page__search-field">
          <span>내용 검색</span>
          <input v-model="filters.keyword" class="input-field" type="search" placeholder="업무명 또는 특이사항">
        </label>
        <div class="daily-report-page__filter-actions">
          <CommonBaseButton variant="secondary" type="button" @click="filters.dateFrom = getDaysAgoInput(14); filters.dateTo = formatDateInput(new Date()); filters.keyword = ''; void handleSearch()">
            초기화
          </CommonBaseButton>
          <CommonBaseButton type="submit">
            조회
          </CommonBaseButton>
        </div>
      </form>

      <div class="daily-report-page__table-wrap">
        <table class="daily-report-page__table">
          <thead>
            <tr>
              <th>작성일자</th>
              <th>작업 업무</th>
              <th>특이사항</th>
              <th>수정일</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="dailyReport in dailyReportPage.content" :key="dailyReport.uuid">
              <td>{{ formatDateWithWeekday(dailyReport.reportDate) }}</td>
              <td class="daily-report-page__content-cell">
                {{ dailyReport.workUnits.length ? dailyReport.workUnits.map((workUnit) => workUnit.title).join(', ') : '-' }}
              </td>
              <td class="daily-report-page__content-cell">
                {{ dailyReport.note || extractSection(dailyReport.content, '이슈 / 특이사항') || '-' }}
              </td>
              <td>{{ formatDateTime(dailyReport.updatedAt) }}</td>
              <td>
                <div class="daily-report-page__row-actions">
                  <CommonBaseButton variant="secondary" size="small" @click="openDetailMode(dailyReport)">
                    상세
                  </CommonBaseButton>
                  <CommonBaseButton variant="secondary" size="small" @click="startEditMode(dailyReport)">
                  수정
                  </CommonBaseButton>
                </div>
              </td>
            </tr>
            <tr v-if="!isLoading && dailyReportPage.content.length === 0">
              <td colspan="5" class="daily-report-page__empty">
                조회된 일일보고가 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <CommonBasePagination
        :page="dailyReportPage.page"
        :total-pages="dailyReportPage.totalPages"
        @previous="movePage(filters.page - 1)"
        @next="movePage(filters.page + 1)"
      />
    </section>

    <button
      type="button"
      class="daily-report-page__missing-toggle"
      :class="{ 'daily-report-page__missing-toggle--active': isMissingPanelVisible }"
      @click="isMissingPanelVisible = !isMissingPanelVisible"
    >
      누락 {{ summary.missingCount }}일
    </button>

    <aside v-if="isMissingPanelVisible" class="daily-report-page__floating-panel">
      <section class="content-panel daily-report-page__panel daily-report-page__floating-panel-card">
        <div class="daily-report-page__panel-header">
          <h2>누락 현황</h2>
          <div class="daily-report-page__floating-header-actions">
            <strong>{{ summary.missingCount }}일 누락</strong>
            <button type="button" class="daily-report-page__floating-close" @click="isMissingPanelVisible = false">
              닫기
            </button>
          </div>
        </div>

        <p class="daily-report-page__empty-text">
          선택한 기간 안에서 아직 작성하지 않은 평일만 표시합니다.
        </p>

        <div v-if="visibleMissingDailyReports.length" class="daily-report-page__missing-list">
          <div
            v-for="missingDailyReport in visibleMissingDailyReports"
            :key="missingDailyReport.reportDate"
            class="daily-report-page__missing-item"
          >
            <span class="daily-report-page__missing-dot" />
            <strong>{{ formatMissingDateLabel(missingDailyReport.reportDate) }}</strong>
          </div>
        </div>
        <p v-else class="daily-report-page__empty-text">
          현재 조회 구간에는 누락된 평일이 없습니다.
        </p>
      </section>
    </aside>

    <WorkDailyReportDetailModal
      :visible="isDetailVisible"
      :report="selectedDetailReport"
      @close="closeDetailModal"
      @edit="startEditMode"
    />

    <CommonBaseModal
      :visible="isFormVisible"
      eyebrow="Daily Report"
      :title="editingDailyReportUuid ? '일일보고 수정' : '일일보고 작성'"
      width="100vw"
      @close="closeFormModal"
    >
      <template #title-extra>
        <div class="daily-report-page__title-actions">
          <button
            v-if="isDesktopModalLayout"
            type="button"
            class="daily-report-page__title-toggle"
            :class="{ 'daily-report-page__title-toggle--inactive': isTaskPanelCollapsed }"
            :aria-expanded="!isTaskPanelCollapsed"
            aria-label="업무목록 열기/닫기"
            @click="toggleTaskPanel"
          >
            <span aria-hidden="true">{{ isTaskPanelCollapsed ? '>' : '<' }}</span>
            <span class="daily-report-page__title-toggle-text">업무</span>
          </button>

          <button
            v-if="isDesktopModalLayout"
            type="button"
            class="daily-report-page__title-toggle"
            :class="{ 'daily-report-page__title-toggle--inactive': isHistoryPanelCollapsed }"
            :aria-expanded="!isHistoryPanelCollapsed"
            aria-label="지난 일일보고 열기/닫기"
            @click="toggleHistoryPanel"
          >
            <span aria-hidden="true">{{ isHistoryPanelCollapsed ? '>' : '<' }}</span>
            <span class="daily-report-page__title-toggle-text">히스토리</span>
          </button>
        </div>
      </template>

      <div class="daily-report-page__modal-layout" :style="modalLayoutStyle">
        <div class="daily-report-page__panel-shell" :class="{ 'daily-report-page__panel-shell--collapsed': isTaskPanelCollapsed }">
          <div class="daily-report-page__panel-content">
            <WorkTaskListPanel
              :tasks="filteredWorkUnitOptions"
              :selected-task-uuids="selectedTaskUuids"
              :active-task-uuid="activeTaskUuid"
              :search-keyword="workUnitSearchKeyword"
              :is-loading="isWorkUnitLoading"
              @update:search-keyword="workUnitSearchKeyword = $event"
              @select-task="selectTask"
              @set-task-selection="setTaskSelection($event.workUnitUuid, $event.checked)"
            />
          </div>
        </div>

        <div class="daily-report-page__panel-shell" :class="{ 'daily-report-page__panel-shell--collapsed': isHistoryPanelCollapsed }">
          <div class="daily-report-page__panel-content">
            <WorkReportHistoryPanel
              :active-task="activeTask"
              :reports="historyReports"
              :selected-report-uuid="historyPreviewReportUuid"
              :preview-report="historyPreviewReport"
              :is-loading="isHistoryLoading"
              @select-report="historyPreviewReportUuid = $event"
              @use-as-draft="applyHistoryDraft"
            />
          </div>
        </div>

        <WorkReportEditorPanel
          :report-date="formState.reportDate"
          :selected-work-units="selectedWorkUnits"
          :work-summary="formState.workSummary"
          :issue-note="formState.issueNote"
          :is-editing="Boolean(editingDailyReportUuid)"
          @update:report-date="formState.reportDate = $event"
          @update:work-summary="formState.workSummary = $event"
          @update:issue-note="formState.issueNote = $event"
        />
      </div>

      <template #actions>
        <CommonBaseButton variant="secondary" type="button" :disabled="isSubmitting" @click="closeFormModal">
          취소
        </CommonBaseButton>
        <CommonBaseButton type="button" :disabled="isSubmitting" @click="handleSubmit">
          {{ editingDailyReportUuid ? '수정 저장' : '작성 완료' }}
        </CommonBaseButton>
      </template>
    </CommonBaseModal>
  </main>
</template>

<style scoped>
.daily-report-page {
  display: grid;
  gap: 24px;
}

.daily-report-page__hero,
.daily-report-page__panel {
  padding: 20px 22px;
}

.daily-report-page__hero {
  display: grid;
  gap: 12px;
}

.daily-report-page__hero-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.daily-report-page__hero-side {
  display: grid;
  justify-items: end;
  gap: 10px;
}

.daily-report-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.daily-report-page__summary {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.daily-report-page__summary strong {
  color: var(--color-text);
  font-size: 1rem;
}

.daily-report-page__summary-divider {
  opacity: 0.45;
}

.daily-report-page__panel {
  display: grid;
  gap: 18px;
}

.daily-report-page__panel-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.daily-report-page__filter-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.daily-report-page__filters {
  display: grid;
  grid-template-columns: minmax(150px, 0.9fr) minmax(150px, 0.9fr) minmax(260px, 1.6fr) auto;
  align-items: end;
  gap: 14px 16px;
}

.daily-report-page__filter-actions {
  justify-content: flex-end;
  white-space: nowrap;
}

.daily-report-page__filters label {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.daily-report-page__filters label span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.daily-report-page__search-field,
.daily-report-page__table-wrap {
  min-width: 0;
}

.daily-report-page__empty-text {
  color: var(--color-text-muted);
}

.daily-report-page__missing-list {
  display: grid;
  gap: 10px;
}

.daily-report-page__missing-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: var(--radius-medium);
  background: rgba(255, 115, 115, 0.08);
  border: 1px solid rgba(255, 115, 115, 0.18);
}

.daily-report-page__missing-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #ff8b8b;
  flex-shrink: 0;
}

.daily-report-page__missing-toggle {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 12;
  min-height: 48px;
  padding: 0 18px;
  border: 1px solid rgba(147, 210, 255, 0.22);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(17, 43, 74, 0.98), rgba(8, 23, 42, 0.94));
  color: rgba(232, 244, 255, 0.96);
  box-shadow: 0 18px 36px rgba(2, 8, 18, 0.32);
  font-weight: 700;
}

.daily-report-page__missing-toggle--active {
  opacity: 0.78;
}

.daily-report-page__floating-panel {
  position: fixed;
  right: 24px;
  top: 140px;
  z-index: 11;
  width: min(360px, calc(100vw - 32px));
}

.daily-report-page__floating-panel-card {
  max-height: calc(100vh - 164px);
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  box-shadow: 0 24px 50px rgba(2, 8, 18, 0.34);
}

.daily-report-page__floating-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.daily-report-page__floating-close {
  border: 0;
  background: transparent;
  color: var(--color-text-muted);
  font: inherit;
}

.daily-report-page__modal-layout {
  display: grid;
  gap: 16px;
  min-height: calc(100vh - 180px);
  min-width: 0;
}

.daily-report-page__panel-shell {
  position: relative;
  min-width: 0;
  overflow: hidden;
  transition: transform 0.28s ease, opacity 0.22s ease, filter 0.22s ease;
}

.daily-report-page__panel-content {
  min-width: 0;
  height: 100%;
  transition: transform 0.28s ease, opacity 0.22s ease;
}

.daily-report-page__panel-shell--collapsed .daily-report-page__panel-content {
  opacity: 0;
  pointer-events: none;
  transform: translateX(-20px) scale(0.98);
}

.daily-report-page__panel-shell:not(.daily-report-page__panel-shell--collapsed) .daily-report-page__panel-content {
  transform: translateX(0);
}

.daily-report-page__title-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.daily-report-page__title-toggle {
  min-width: auto;
  min-height: auto;
  padding: 6px 10px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: 999px;
  background: rgba(7, 21, 39, 0.72);
  color: rgba(226, 240, 255, 0.92);
  font: inherit;
  font-size: 0.76rem;
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  transition: background 0.2s ease, border-color 0.2s ease, opacity 0.2s ease;
}

.daily-report-page__title-toggle:hover {
  background: rgba(18, 46, 77, 0.9);
  border-color: rgba(147, 210, 255, 0.28);
}

.daily-report-page__title-toggle--inactive {
  opacity: 0.72;
}

.daily-report-page__title-toggle span:first-child {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.daily-report-page__title-toggle-text {
  color: var(--color-text-muted);
}

:deep(.base-modal) {
  padding: 12px;
}

.daily-report-page__table {
  width: 100%;
  border-collapse: collapse;
}

.daily-report-page__table th,
.daily-report-page__table td {
  padding: 12px 10px;
  border-bottom: 1px solid rgba(143, 208, 255, 0.12);
  text-align: left;
  vertical-align: top;
}

.daily-report-page__content-cell {
  white-space: pre-wrap;
}

.daily-report-page__row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.daily-report-page__empty {
  text-align: center;
  color: var(--color-text-muted);
}

:deep(.base-modal) {
  display: flex;
  justify-content: flex-end;
  align-items: stretch;
  padding: 0;
}

:deep(.base-modal__backdrop) {
  background: rgba(6, 14, 24, 0.44);
}

:deep(.base-modal__panel.content-panel) {
  width: 100vw !important;
  max-width: 100vw !important;
  min-width: 100vw !important;
  height: 100vh;
  max-height: 100vh;
  margin-left: auto;
  border-radius: 0;
  border-left: 1px solid rgba(147, 210, 255, 0.16);
  box-shadow: -24px 0 60px rgba(2, 8, 18, 0.34);
  overflow: hidden;
  padding-left: 24px;
  padding-right: 24px;
}

:deep(.base-modal__body) {
  min-height: 0;
  min-width: 0;
  overflow-x: hidden;
}

:deep(.base-modal__actions) {
  position: sticky;
  bottom: 0;
  padding-top: 12px;
  background: linear-gradient(180deg, rgba(6, 18, 34, 0), rgba(6, 18, 34, 0.96) 28%);
}

@media (max-width: 1400px) {
  .daily-report-page__modal-layout {
    grid-template-columns: 240px minmax(0, 4fr) minmax(0, 6fr);
    gap: 14px;
  }
}

@media (max-width: 1100px) {
  .daily-report-page__filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .daily-report-page__modal-layout {
    grid-template-columns: 1fr;
  }

  .daily-report-page__panel-shell,
  .daily-report-page__panel-shell--collapsed {
    overflow: visible;
  }

  .daily-report-page__panel-shell--collapsed .daily-report-page__panel-content {
    opacity: 1;
    pointer-events: auto;
  }

}

@media (max-width: 768px) {
  .daily-report-page__hero-header {
    flex-direction: column;
  }

  .daily-report-page__hero-side {
    justify-items: stretch;
  }

  .daily-report-page__filters {
    grid-template-columns: 1fr;
  }

  .daily-report-page__summary {
    width: 100%;
    min-width: 0;
    justify-content: center;
    flex-wrap: wrap;
  }

  .daily-report-page__floating-panel {
    right: 12px;
    left: 12px;
    top: auto;
    bottom: 84px;
    width: auto;
  }

  .daily-report-page__floating-panel-card {
    max-height: calc(100vh - 108px);
  }

  .daily-report-page__missing-toggle {
    right: 12px;
    bottom: 12px;
  }

  :deep(.base-modal) {
    padding: 0;
  }

  :deep(.base-modal__panel) {
    width: 100%;
    max-width: 100% !important;
  }
}
</style>
