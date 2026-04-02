import type { WeeklyReport, WeeklyReportGenerationType } from '../types/work.types'
import { useWeeklyReportApi } from '../api/weekly-report.api'

export function useWeeklyReportList() {
  const { fetchWeeklyReports } = useWeeklyReportApi()
  const { showToast } = useToast()

  const generationTypeLabelMap: Record<WeeklyReportGenerationType, string> = {
    MANUAL: '수동',
    OPENAI: 'OpenAI',
    RULE_BASED: '초안'
  }

  const weeklyReports = ref<WeeklyReport[]>([])
  const isComposerVisible = ref(false)
  const editingWeeklyReportUuid = ref('')
  const isLoadingWeeklyReports = ref(false)

  const summary = computed(() => ({
    total: weeklyReports.value.length,
    open: weeklyReports.value.filter((weeklyReport) => weeklyReport.openYn === 'Y').length
  }))

  function formatDate(value: string): string {
    if (!value) {
      return '-'
    }

    return new Intl.DateTimeFormat('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).format(new Date(value))
  }

  async function loadWeeklyReports() {
    isLoadingWeeklyReports.value = true

    try {
      weeklyReports.value = await fetchWeeklyReports()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      weeklyReports.value = []
      showToast(fetchError.data?.message ?? '주간보고 목록을 불러오지 못했습니다.', { variant: 'error' })
    } finally {
      isLoadingWeeklyReports.value = false
    }
  }

  function openCreateModal() {
    editingWeeklyReportUuid.value = ''
    isComposerVisible.value = true
  }

  function openEditModal(weeklyReport: WeeklyReport) {
    editingWeeklyReportUuid.value = weeklyReport.uuid
    isComposerVisible.value = true
  }

  function closeComposerModal() {
    isComposerVisible.value = false
    editingWeeklyReportUuid.value = ''
  }

  async function handleComposerSaved() {
    closeComposerModal()
    await loadWeeklyReports()
  }

  onMounted(() => {
    void loadWeeklyReports()
  })

  return {
    closeComposerModal,
    editingWeeklyReportUuid,
    formatDate,
    generationTypeLabelMap,
    handleComposerSaved,
    isComposerVisible,
    isLoadingWeeklyReports,
    openCreateModal,
    openEditModal,
    summary,
    weeklyReports
  }
}
