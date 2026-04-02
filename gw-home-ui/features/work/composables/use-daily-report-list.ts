import type { DailyReport, DailyReportMissing } from '../types/work.types'
import { useDailyReportApi } from '../api/daily-report.api'

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

function toLocalDate(value?: string | null): Date {
  if (!value) {
    return new Date(1970, 0, 1)
  }

  const [year = 1970, month = 1, day = 1] = value.split('-').map((item) => Number(item))
  return new Date(year, month - 1, day)
}

export function useDailyReportList() {
  const { fetchDailyReports, fetchMissingDailyReports } = useDailyReportApi()
  const { showToast } = useToast()

  const filters = reactive({
    dateFrom: getDaysAgoInput(14),
    dateTo: formatDateInput(new Date()),
    keyword: '',
    page: 1,
    size: 10
  })

  const isLoading = ref(false)
  const isMissingPanelVisible = ref(false)
  const isDetailVisible = ref(false)
  const isFormVisible = ref(false)
  const editingDailyReportUuid = ref('')
  const selectedDetailReport = ref<DailyReport | null>(null)
  const missingDailyReports = ref<DailyReportMissing[]>([])
  const dailyReportPage = ref({
    content: [] as DailyReport[],
    page: 1,
    size: 10,
    totalCount: 0,
    totalPages: 0
  })

  const visibleMissingDailyReports = computed(() =>
    missingDailyReports.value.filter((item) => !isWeekend(item.reportDate) && !isFutureDate(item.reportDate))
  )

  const summary = computed(() => ({
    missingCount: visibleMissingDailyReports.value.length,
    totalCount: Number.isFinite(dailyReportPage.value.totalCount) ? dailyReportPage.value.totalCount : 0
  }))

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

  function extractSection(note: string | null | undefined, heading: string): string {
    if (!note) {
      return ''
    }

    const escapedHeading = heading.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const pattern = new RegExp(`\\[${escapedHeading}\\]\\n([\\s\\S]*?)(?=\\n\\n\\[[^\\]]+\\]\\n|$)`)
    const match = note.match(pattern)
    return match?.[1]?.trim() ?? ''
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

  async function handleSearch() {
    filters.page = 1
    await reloadAll()
  }

  async function resetFilters() {
    filters.dateFrom = getDaysAgoInput(14)
    filters.dateTo = formatDateInput(new Date())
    filters.keyword = ''
    await handleSearch()
  }

  async function movePage(nextPage: number) {
    if (nextPage < 1 || nextPage > Math.max(dailyReportPage.value.totalPages, 1) || nextPage === filters.page) {
      return
    }

    filters.page = nextPage
    await loadDailyReports()
  }

  function openDetailMode(report: DailyReport) {
    selectedDetailReport.value = report
    isDetailVisible.value = true
  }

  function openCreateModal() {
    editingDailyReportUuid.value = ''
    isFormVisible.value = true
  }

  function openEditModal(report: DailyReport) {
    editingDailyReportUuid.value = report.uuid
    isDetailVisible.value = false
    selectedDetailReport.value = null
    isFormVisible.value = true
  }

  function closeDetailModal() {
    isDetailVisible.value = false
    selectedDetailReport.value = null
  }

  function closeFormModal() {
    isFormVisible.value = false
    editingDailyReportUuid.value = ''
  }

  async function handleFormSaved() {
    closeFormModal()
    await reloadAll()
  }

  onMounted(() => {
    void reloadAll()
  })

  return {
    closeDetailModal,
    closeFormModal,
    dailyReportPage,
    editingDailyReportUuid,
    extractSection,
    filters,
    formatDateInput,
    formatDateTime,
    formatDateWithWeekday,
    formatMissingDateLabel,
    getDaysAgoInput,
    handleFormSaved,
    handleSearch,
    isDetailVisible,
    isFormVisible,
    isLoading,
    isMissingPanelVisible,
    movePage,
    openCreateModal,
    openDetailMode,
    openEditModal,
    resetFilters,
    selectedDetailReport,
    summary,
    visibleMissingDailyReports
  }
}
