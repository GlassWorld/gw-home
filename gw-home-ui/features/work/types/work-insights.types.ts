import type { WorkTodoInboxSummary } from './work.types'

export type WorkInsightMode = 'gantt' | 'statistics'
export type CompletionBucket = 'day' | 'week' | 'month'
export interface WorkInsightFilters {
  workUnitUuid: string
  fromDate: string
  toDate: string
  includeInactive: boolean
  timeZone: string
  bucket: CompletionBucket
}
export interface WorkInsightPeriod {
  fromDate: string
  toDate: string
  timeZone: string
  asOfDate: string
  includeInactive: boolean
  workUnitUuid: string | null
}
export interface WorkInsightUnit {
  workUnitUuid: string
  title: string
  status: string
  active: boolean
  startDate: string | null
  dueDate: string | null
  partialSchedule: boolean
  summary: WorkTodoInboxSummary
  periodCompletedCount: number
}
export interface WorkGanttRow {
  todoUuid: string
  workUnitUuid: string
  parentTodoUuid: string | null
  title: string
  startDate: string | null
  dueDate: string | null
  status: string
  progressRate: number
  leaf: boolean
  completed: boolean
  delayed: boolean
  contextOnly: boolean
  depth: number
}
export interface WorkGantt {
  period: WorkInsightPeriod
  units: WorkInsightUnit[]
  rows: WorkGanttRow[]
}
export interface WorkCompletionPoint {
  startDate: string
  endDate: string
  completedCount: number
}
export interface WorkStatistics {
  period: WorkInsightPeriod
  bucket: CompletionBucket
  summary: WorkTodoInboxSummary
  periodCompletedCount: number
  trend: WorkCompletionPoint[]
  units: WorkInsightUnit[]
}
