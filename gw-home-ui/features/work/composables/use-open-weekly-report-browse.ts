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
  const selectedOpenWeeklyReport = ref<OpenWeeklyReport | null>(null)
  const isOpenDetailVisible = ref(false)
  const isLoadingOpenMembers = ref(false)
  const isLoadingOpenWeeklyReports = ref(false)
  const isLoadingOpenWeeklyReportDetail = ref(false)

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
    closeOpenWeeklyReportDetail,
    formatDate,
    isLoadingOpenMembers,
    isLoadingOpenWeeklyReportDetail,
    isLoadingOpenWeeklyReports,
    isOpenDetailVisible,
    openOpenWeeklyReportDetail,
    openWeeklyReportMembers,
    openWeeklyReports,
    selectedOpenMemberUuid,
    selectedOpenWeeklyReport
  }
}
