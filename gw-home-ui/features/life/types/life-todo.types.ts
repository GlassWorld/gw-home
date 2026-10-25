export type LifeTodoGroup = 'ALL' | 'TODAY' | 'UPCOMING' | 'OVERDUE' | 'COMPLETED' | 'UNSCHEDULED'

export interface LifeCategory { categoryUuid: string; name: string }
export interface LifeTodo {
  todoUuid: string
  title: string
  memo: string | null
  categoryUuid: string | null
  categoryName: string | null
  dueDate: string | null
  completed: boolean
  completedAt: string | null
  group: Exclude<LifeTodoGroup, 'ALL'>
  createdAt: string
  updatedAt: string
}
export interface LifeTodoSummary {
  totalCount: number
  todayCount: number
  upcomingCount: number
  overdueCount: number
  completedCount: number
  unscheduledCount: number
  progressRate: number
  asOfDate: string
}
export interface LifeTodoParams {
  categoryUuid?: string
  uncategorized?: boolean
  keyword?: string
  group?: LifeTodoGroup
  page?: number
  size?: number
}
export interface LifeTodoForm { title: string; memo: string; categoryUuid: string; dueDate: string }
export const lifeTodoGroups: { value: LifeTodoGroup; label: string; count: keyof LifeTodoSummary }[] = [
  { value: 'ALL', label: '전체', count: 'totalCount' },
  { value: 'TODAY', label: '오늘', count: 'todayCount' },
  { value: 'UPCOMING', label: '예정', count: 'upcomingCount' },
  { value: 'OVERDUE', label: '지연', count: 'overdueCount' },
  { value: 'COMPLETED', label: '완료', count: 'completedCount' },
  { value: 'UNSCHEDULED', label: '일정 미지정', count: 'unscheduledCount' }
]
