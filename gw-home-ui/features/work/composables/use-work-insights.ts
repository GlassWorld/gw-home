import { useWorkInsightsApi } from '~/composables/use-work-insights-api'
import { useWorkUnitApi } from '../api/work-unit.api'
import { getWorkTodoTimeZone } from '../utils/work-todo'
import { addCalendarDays, dayNumber, todayInZone } from '../utils/work-insights'
import type { WorkGantt, WorkStatistics, WorkInsightFilters, WorkInsightMode } from '../types/work-insights.types'
import type { WorkUnitOption } from '../types/work.types'

export function useWorkInsights(mode: WorkInsightMode) {
  const api = useWorkInsightsApi()
  const { fetchWorkUnitOptions } = useWorkUnitApi()
  const filters = reactive<WorkInsightFilters>({
    workUnitUuid: '', fromDate: '', toDate: '', includeInactive: mode === 'statistics',
    timeZone: 'Asia/Seoul', bucket: 'day'
  })
  const applied = ref<WorkInsightFilters>({ ...filters })
  const gantt = ref<WorkGantt | null>(null)
  const statistics = ref<WorkStatistics | null>(null)
  const workUnits = ref<WorkUnitOption[]>([])
  const loading = ref(true)
  const errorMessage = ref('')
  const validationMessage = ref('')
  const optionsError = ref('')
  let sequence = 0
  let disposed = false

  async function loadOptions() {
    try {
      const value = await fetchWorkUnitOptions(true)
      if (!disposed) { workUnits.value = value; optionsError.value = '' }
    } catch { if (!disposed) optionsError.value = '업무 목록을 불러오지 못했습니다.' }
  }
  // 최신 요청만 표시해 연속 조회와 페이지 이동 중 늦은 응답이 화면을 덮지 않게 한다.
  async function refresh() {
    const request = ++sequence
    loading.value = true
    errorMessage.value = ''
    gantt.value = null
    statistics.value = null
    try {
      if (mode === 'gantt') {
        const value = await api.fetchGantt(applied.value)
        if (request === sequence) gantt.value = value
      } else {
        const value = await api.fetchStatistics(applied.value)
        if (request === sequence) statistics.value = value
      }
    } catch (error) {
      if (request === sequence) {
        const failure = error as { data?: { message?: string }; message?: string }
        errorMessage.value = failure.data?.message ?? failure.message ?? '조회하지 못했습니다.'
      }
    } finally { if (request === sequence) loading.value = false }
  }
  function applyFilters() {
    const length = dayNumber(filters.toDate) - dayNumber(filters.fromDate) + 1
    const limit = mode === 'gantt' ? 93 : 366
    if (!Number.isFinite(length) || length < 1 || length > limit) {
      validationMessage.value = '시작일과 종료일을 확인해주세요. 최대 ' + limit + '일까지 조회할 수 있습니다.'
      return
    }
    try { new Intl.DateTimeFormat('ko-KR', { timeZone: filters.timeZone }).format() }
    catch { validationMessage.value = '올바른 시간대를 입력해주세요. 예: Asia/Seoul'; return }
    validationMessage.value = ''
    if (!filters.includeInactive && workUnits.value.some(unit => unit.workUnitUuid === filters.workUnitUuid && unit.useYn === 'N'))
      filters.workUnitUuid = ''
    applied.value = { ...filters }
    void refresh()
  }
  function preset(days: number) {
    try {
      const today = todayInZone(filters.timeZone || 'Asia/Seoul')
      filters.fromDate = mode === 'gantt' ? today : addCalendarDays(today, -(days - 1))
      filters.toDate = mode === 'gantt' ? addCalendarDays(today, days - 1) : today
      validationMessage.value = ''
    } catch { validationMessage.value = '올바른 시간대를 입력해주세요. 예: Asia/Seoul' }
  }
  function refreshOnFocus() { if (!loading.value) void refresh() }
  onMounted(() => {
    filters.timeZone = getWorkTodoTimeZone()
    preset(mode === 'gantt' ? 14 : 30)
    applyFilters()
    void loadOptions()
    window.addEventListener('focus', refreshOnFocus)
  })
  onBeforeUnmount(() => {
    disposed = true
    sequence++
    window.removeEventListener('focus', refreshOnFocus)
  })
  return { filters, gantt, statistics, workUnits, loading, errorMessage, validationMessage, optionsError, refresh, loadOptions, applyFilters, preset }
}
