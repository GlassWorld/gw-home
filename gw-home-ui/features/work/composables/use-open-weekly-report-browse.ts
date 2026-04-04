import type { OpenWeeklyReport, OpenWeeklyReportMember } from '../types/work.types'
import { useWeeklyReportApi } from '../api/weekly-report.api'

export function useOpenWeeklyReportBrowse() {
  const {
    fetchOpenWeeklyReport,
    fetchOpenWeeklyReportMembers,
    fetchOpenWeeklyReports
  } = useWeeklyReportApi()
  const { showToast } = useToast()

  const openWeeklyReportMembers = ref<OpenWeeklyReportMember[]>([])
  const openWeeklyReports = ref<OpenWeeklyReport[]>([])
  const selectedOpenMemberUuid = ref('')
  const selectedYearMonth = ref('')
  const selectedWeekOfMonth = ref('')
  const selectedOpenWeeklyReport = ref<OpenWeeklyReport | null>(null)
  const isOpenDetailVisible = ref(false)
  const isLoadingOpenMembers = ref(false)
  const isLoadingOpenWeeklyReports = ref(false)
  const isLoadingOpenWeeklyReportDetail = ref(false)

  const availableYearMonths = computed(() =>
    Array.from(new Set(openWeeklyReports.value.map((report) => report.weekStartDate.slice(0, 7)).filter(Boolean)))
      .sort((left, right) => right.localeCompare(left))
  )

  const filteredOpenWeeklyReports = computed(() => {
    return openWeeklyReports.value.filter((report) => {
      const matchesYearMonth = !selectedYearMonth.value || report.weekStartDate.slice(0, 7) === selectedYearMonth.value
      const matchesWeekOfMonth = !selectedWeekOfMonth.value || String(resolveWeekOfMonth(report.weekStartDate)) === selectedWeekOfMonth.value
      return matchesYearMonth && matchesWeekOfMonth
    })
  })

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

  async function loadOpenWeeklyReports() {
    if (!selectedOpenMemberUuid.value) {
      openWeeklyReports.value = []
      return
    }

    isLoadingOpenWeeklyReports.value = true

    try {
      openWeeklyReports.value = await fetchOpenWeeklyReports(selectedOpenMemberUuid.value)
      const firstYearMonth = availableYearMonths.value[0] ?? ''

      if (firstYearMonth && !selectedYearMonth.value) {
        selectedYearMonth.value = firstYearMonth
      }

      if (selectedYearMonth.value && !availableYearMonths.value.includes(selectedYearMonth.value)) {
        selectedYearMonth.value = firstYearMonth
      }

      if (selectedWeekOfMonth.value) {
        const hasSelectedWeek = openWeeklyReports.value.some((report) =>
          (!selectedYearMonth.value || report.weekStartDate.slice(0, 7) === selectedYearMonth.value)
          && String(resolveWeekOfMonth(report.weekStartDate)) === selectedWeekOfMonth.value
        )

        if (!hasSelectedWeek) {
          selectedWeekOfMonth.value = ''
        }
      }
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      openWeeklyReports.value = []
      showToast(fetchError.data?.message ?? '공개 주간보고 목록을 불러오지 못했습니다.', { variant: 'error' })
    } finally {
      isLoadingOpenWeeklyReports.value = false
    }
  }

  async function loadOpenWeeklyReportMembers() {
    isLoadingOpenMembers.value = true

    try {
      openWeeklyReportMembers.value = await fetchOpenWeeklyReportMembers()

      if (!openWeeklyReportMembers.value.length) {
        selectedOpenMemberUuid.value = ''
        openWeeklyReports.value = []
        return
      }

      const firstOpenMember = openWeeklyReportMembers.value[0]

      if (firstOpenMember && (!selectedOpenMemberUuid.value || !openWeeklyReportMembers.value.some((member) => member.memberUuid === selectedOpenMemberUuid.value))) {
        selectedOpenMemberUuid.value = firstOpenMember.memberUuid
      }

      await loadOpenWeeklyReports()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      openWeeklyReportMembers.value = []
      openWeeklyReports.value = []
      selectedOpenMemberUuid.value = ''
      showToast(fetchError.data?.message ?? '공개 주간보고 사용자 목록을 불러오지 못했습니다.', { variant: 'error' })
    } finally {
      isLoadingOpenMembers.value = false
    }
  }

  async function changeSelectedOpenMember(memberUuid: string) {
    selectedOpenMemberUuid.value = memberUuid
    await loadOpenWeeklyReports()
  }

  function changeSelectedYearMonth(value: string) {
    selectedYearMonth.value = value
  }

  function changeSelectedWeekOfMonth(value: string) {
    selectedWeekOfMonth.value = value
  }

  function resolveWeekOfMonth(value: string): number {
    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
      return 0
    }

    return Math.ceil(date.getDate() / 7)
  }

  async function openOpenWeeklyReportDetail(reportUuid: string) {
    isLoadingOpenWeeklyReportDetail.value = true
    isOpenDetailVisible.value = true

    try {
      selectedOpenWeeklyReport.value = await fetchOpenWeeklyReport(reportUuid)
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      selectedOpenWeeklyReport.value = null
      isOpenDetailVisible.value = false
      showToast(fetchError.data?.message ?? '공개 주간보고 상세를 불러오지 못했습니다.', { variant: 'error' })
    } finally {
      isLoadingOpenWeeklyReportDetail.value = false
    }
  }

  function closeOpenWeeklyReportDetail() {
    isOpenDetailVisible.value = false
    selectedOpenWeeklyReport.value = null
  }

  onMounted(() => {
    void loadOpenWeeklyReportMembers()
  })

  return {
    changeSelectedOpenMember,
    changeSelectedYearMonth,
    changeSelectedWeekOfMonth,
    closeOpenWeeklyReportDetail,
    availableYearMonths,
    filteredOpenWeeklyReports,
    formatDate,
    isLoadingOpenMembers,
    isLoadingOpenWeeklyReportDetail,
    isLoadingOpenWeeklyReports,
    isOpenDetailVisible,
    openOpenWeeklyReportDetail,
    openWeeklyReportMembers,
    openWeeklyReports,
    resolveWeekOfMonth,
    selectedOpenMemberUuid,
    selectedWeekOfMonth,
    selectedOpenWeeklyReport
    ,
    selectedYearMonth
  }
}
