// 설정 시간대의 벽시계 시각을 브라우저의 시간대와 무관하게 표시한다.
export function notificationLocalTime(instant: string, timeZone: string): string {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(new Date(instant))
  const part = (type: string) => parts.find(item => item.type === type)?.value ?? ''
  return `${part('year')}-${part('month')}-${part('day')}T${part('hour')}:${part('minute')}`
}
// DST 공백은 거부하고 중복 시각은 먼저 오는 실제 시각으로 예약한다.
export function notificationInstant(localTime: string, timeZone: string): string {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(localTime)) throw new Error('발송 날짜와 시간을 입력하세요.')
  const naive = Date.parse(localTime + ':00Z')
  if (!Number.isFinite(naive) || new Date(naive).toISOString().slice(0, 16) !== localTime) throw new Error('유효한 날짜와 시간을 입력하세요.')
  const offsets = new Set<number>()
  for (const days of [-1, 0, 1]) {
    const sample = naive + days * 86_400_000
    offsets.add(Date.parse(notificationLocalTime(new Date(sample).toISOString(), timeZone) + ':00Z') - sample)
  }
  const candidates = [...offsets].map(offset => naive - offset).filter(candidate =>
    notificationLocalTime(new Date(candidate).toISOString(), timeZone) === localTime).sort((left, right) => left - right)
  const result = candidates[0]
  if (result === undefined) throw new Error('시간대 변경으로 존재하지 않는 시각입니다. 다른 시간을 선택하세요.')
  return new Date(result).toISOString()
}
export function formatNotificationTime(instant: string | null, timeZone: string): string {
  return instant ? new Intl.DateTimeFormat('ko-KR', { timeZone, dateStyle: 'medium', timeStyle: 'short' }).format(new Date(instant)) : '예정 없음'
}
