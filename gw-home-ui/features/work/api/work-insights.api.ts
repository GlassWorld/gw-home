import type { ApiResponse } from '~/types/api/common'
import type { WorkTodoInboxSummary } from '../types/work.types'
import type { WorkInsightFilters, WorkInsightPeriod, WorkInsightUnit, WorkGantt, WorkGanttRow, WorkStatistics, CompletionBucket } from '../types/work-insights.types'

interface SummaryApi {
  total_count: number; today_count: number; upcoming_count: number; overdue_count: number
  completed_count: number; unscheduled_count: number; unknown_completion_count: number
  progress_rate: number; as_of_date: string; time_zone: string
}
interface PeriodApi {
  from_date: string; to_date: string; time_zone: string; as_of_date: string
  include_inactive: boolean; work_unit_uuid: string | null
}
interface UnitApi {
  work_unit_uuid: string; title: string; status: string; active: boolean
  start_date: string | null; due_date: string | null; partial_schedule: boolean
  summary: SummaryApi; period_completed_count: number
}
interface RowApi {
  todo_uuid: string; work_unit_uuid: string; parent_todo_uuid: string | null; title: string
  start_date: string | null; due_date: string | null; status: string; progress_rate: number
  leaf: boolean; completed: boolean; delayed: boolean; context_only: boolean; depth: number
}
interface GanttApi { period: PeriodApi; units: UnitApi[]; rows: RowApi[] }
interface StatisticsApi {
  period: PeriodApi; bucket: CompletionBucket; summary: SummaryApi; period_completed_count: number; units: UnitApi[]
  trend: { start_date: string; end_date: string; completed_count: number }[]
}

const toSummary = (value: SummaryApi): WorkTodoInboxSummary => ({
  totalCount: value.total_count, todayCount: value.today_count, upcomingCount: value.upcoming_count,
  overdueCount: value.overdue_count, completedCount: value.completed_count, unscheduledCount: value.unscheduled_count,
  unknownCompletionCount: value.unknown_completion_count, progressRate: value.progress_rate,
  asOfDate: value.as_of_date, timeZone: value.time_zone
})
const toPeriod = (value: PeriodApi): WorkInsightPeriod => ({
  fromDate: value.from_date, toDate: value.to_date, timeZone: value.time_zone, asOfDate: value.as_of_date,
  includeInactive: value.include_inactive, workUnitUuid: value.work_unit_uuid
})
const toUnit = (value: UnitApi): WorkInsightUnit => ({
  workUnitUuid: value.work_unit_uuid, title: value.title, status: value.status, active: value.active,
  startDate: value.start_date, dueDate: value.due_date, partialSchedule: value.partial_schedule,
  summary: toSummary(value.summary), periodCompletedCount: value.period_completed_count
})
const toRow = (value: RowApi): WorkGanttRow => ({
  todoUuid: value.todo_uuid, workUnitUuid: value.work_unit_uuid, parentTodoUuid: value.parent_todo_uuid,
  title: value.title, startDate: value.start_date, dueDate: value.due_date, status: value.status,
  progressRate: value.progress_rate, leaf: value.leaf, completed: value.completed,
  delayed: value.delayed, contextOnly: value.context_only, depth: value.depth
})

export function useWorkInsightsApi() {
  const { authorizedFetch } = useAuth()
  async function read<T>(path: string, filters: WorkInsightFilters): Promise<T> {
    const response = await authorizedFetch<ApiResponse<T>>(path, {
      query: { ...filters, includeInactive: String(filters.includeInactive), workUnitUuid: filters.workUnitUuid || undefined }
    })
    if (!response.success || response.data == null) throw new Error(response.message ?? '조회하지 못했습니다.')
    return response.data
  }
  async function fetchGantt(filters: WorkInsightFilters): Promise<WorkGantt> {
    const value = await read<GanttApi>('/api/v1/work-todos/gantt', filters)
    return { period: toPeriod(value.period), units: value.units.map(toUnit), rows: value.rows.map(toRow) }
  }
  async function fetchStatistics(filters: WorkInsightFilters): Promise<WorkStatistics> {
    const value = await read<StatisticsApi>('/api/v1/work-todos/statistics', filters)
    return {
      period: toPeriod(value.period), bucket: value.bucket, summary: toSummary(value.summary),
      periodCompletedCount: value.period_completed_count, units: value.units.map(toUnit),
      trend: value.trend.map(point => ({ startDate: point.start_date, endDate: point.end_date, completedCount: point.completed_count }))
    }
  }
  return { fetchGantt, fetchStatistics }
}
