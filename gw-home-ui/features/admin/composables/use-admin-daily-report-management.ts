import type { AdminDailyReport, AdminDailyReportMissing } from '~/types/work'
import { formatDate, formatDateInput, getMonthStartInput } from '~/utils/date'

function createInitialAdminDailyReportFilters() {
  return {
    dateFrom: getMonthStartInput(),
    dateTo: formatDateInput(new Date()),
    keyword: '',
    page: 1,
    size: 20
  }
}

export function useAdminDailyReportManagement() {
  const { fetchAdminDailyReports, fetchAdminMissingDailyReports } = useDailyReportApi()
  const { showToast } = useToast()

  const filters = reactive(createInitialAdminDailyReportFilters())
  const dailyReportPage = ref({
    content: [] as AdminDailyReport[],
    page: 1,
    size: 20,
    totalCount: 0,
    totalPages: 0
  })
  const missingMembers = ref<AdminDailyReportMissing[]>([])
  const isLoading = ref(false)

  async function loadDailyReports() {
    isLoading.value = true

    try {
      dailyReportPage.value = await fetchAdminDailyReports({
        dateFrom: filters.dateFrom,
        dateTo: filters.dateTo,
        keyword: filters.keyword.trim() || undefined,
        page: filters.page,
        size: filters.size
      })
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      dailyReportPage.value.content = []
      showToast(fetchError.data?.message ?? '관리자 일일보고 목록을 불러오지 못했습니다.', { variant: 'error' })
    } finally {
      isLoading.value = false
    }
  }

  async function loadMissingMembers() {
    try {
      missingMembers.value = await fetchAdminMissingDailyReports({
        dateFrom: filters.dateFrom,
        dateTo: filters.dateTo
      })
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      missingMembers.value = []
      showToast(fetchError.data?.message ?? '누락 현황을 불러오지 못했습니다.', { variant: 'error' })
    }
  }

  async function reloadAll() {
    await Promise.all([loadDailyReports(), loadMissingMembers()])
  }

  async function handleSearch() {
    filters.page = 1
    await reloadAll()
  }

  function resetFilters() {
    Object.assign(filters, createInitialAdminDailyReportFilters())
  }

  async function movePage(nextPage: number) {
    if (nextPage < 1 || nextPage > Math.max(dailyReportPage.value.totalPages, 1) || nextPage === filters.page) {
      return
    }

    filters.page = nextPage
    await loadDailyReports()
  }

  return {
    filters,
    dailyReportPage,
    missingMembers,
    isLoading,
    formatDate,
    formatDateInput,
    getMonthStartInput,
    resetFilters,
    reloadAll,
    handleSearch,
    movePage
  }
}
