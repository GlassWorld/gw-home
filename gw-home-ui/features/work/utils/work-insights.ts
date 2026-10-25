import type { WorkGanttRow } from '../types/work-insights.types'

// 일정 DATE를 UTC 달력 좌표로만 계산해 브라우저 시간대·DST로 하루가 바뀌지 않게 한다.
export function dayNumber(value: string): number {
  return Date.parse(value + 'T00:00:00Z') / 86400000
}
export function addCalendarDays(value: string, count: number): string {
  return new Date((dayNumber(value) + count) * 86400000).toISOString().slice(0, 10)
}
export function todayInZone(timeZone: string): string {
  const parts = new Intl.DateTimeFormat('en-US', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date())
  return ['year', 'month', 'day'].map(type => parts.find(part => part.type === type)?.value ?? '').join('-')
}
export function calendarDates(from: string, to: string): string[] {
  const length = dayNumber(to) - dayNumber(from) + 1
  return Number.isFinite(length) && length > 0 && length <= 366
    ? Array.from({ length }, (_, index) => addCalendarDays(from, index)) : []
}
// 한쪽 날짜만 있는 일정은 하루 표식으로 표현하고 보이는 기간까지만 자른다.
export function schedulePosition(start: string | null, due: string | null, from: string, to: string) {
  const first = start ?? due
  const last = due ?? start
  if (!first || !last || last < from || first > to) return null
  const days = dayNumber(to) - dayNumber(from) + 1
  return {
    left: (dayNumber(first < from ? from : first) - dayNumber(from)) / days * 100,
    width: (dayNumber(last > to ? to : last) - dayNumber(first < from ? from : first) + 1) / days * 100,
    partial: !start || !due
  }
}
export function scheduleLabel(start: string | null, due: string | null): string {
  if (!start && !due) return '일정 미지정'
  if (!start) return '시작 미정 · 마감 ' + due
  if (!due) return '시작 ' + start + ' · 마감 미정'
  return start === due ? start : start + ' ~ ' + due
}
export function todoStatusLabel(row: WorkGanttRow): string {
  return row.completed ? '완료' : row.delayed ? '지연' : row.status === 'IN_PROGRESS' ? '진행 중' : '할 일'
}
export function todoDetailLink(row: WorkGanttRow): string {
  return '/work/todos/' + encodeURIComponent(row.workUnitUuid) + '?todo=' + encodeURIComponent(row.todoUuid)
}
export const workUnitStatusLabels: Record<string, string> = { IN_PROGRESS: '진행 중', DONE: '완료', ON_HOLD: '보류' }
