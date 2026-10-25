import type { WorkTodoGroup } from '../types/work.types'

export const workTodoGroups: { value: WorkTodoGroup; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'TODAY', label: '오늘' },
  { value: 'UPCOMING', label: '예정' },
  { value: 'OVERDUE', label: '지연' },
  { value: 'COMPLETED', label: '완료' },
  { value: 'UNSCHEDULED', label: '일정 없음' }
]

export function getWorkTodoTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Seoul'
}

export function formatTodoCompletion(value: string | null): string {
  if (!value) return '완료 시각 미기록'
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: getWorkTodoTimeZone(), dateStyle: 'medium', timeStyle: 'short'
  }).format(new Date(value))
}
