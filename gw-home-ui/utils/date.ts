type DateFormatOptions = {
  year?: 'numeric' | '2-digit'
  month?: 'numeric' | '2-digit'
  day?: 'numeric' | '2-digit'
}

function parseDate(value: string | Date | null | undefined): Date | null {
  if (!value) {
    return null
  }

  const date = value instanceof Date ? value : new Date(value)

  if (Number.isNaN(date.getTime())) {
    return null
  }

  return date
}

function padNumber(value: number): string {
  return String(value).padStart(2, '0')
}

function getKoreanDateParts(value: string | Date | null | undefined) {
  const date = parseDate(value)

  if (!date) {
    return null
  }

  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })

  const parts = formatter.formatToParts(date)
  const getPart = (type: Intl.DateTimeFormatPartTypes) => parts.find(part => part.type === type)?.value ?? '00'

  return {
    year: getPart('year'),
    month: getPart('month'),
    day: getPart('day'),
    hour: getPart('hour'),
    minute: getPart('minute'),
    second: getPart('second')
  }
}

export function formatDate(value: string | Date | null | undefined, options: DateFormatOptions = {}): string {
  const parts = getKoreanDateParts(value)

  if (!parts) {
    return '-'
  }

  const year = options.year === '2-digit' ? parts.year.slice(-2) : parts.year
  const month = options.month === 'numeric' ? String(Number(parts.month)) : parts.month
  const day = options.day === 'numeric' ? String(Number(parts.day)) : parts.day

  return `${year}.${month}.${day}`
}

export function formatDateTime(value: string | Date | null | undefined): string {
  const parts = getKoreanDateParts(value)

  if (!parts) {
    return '-'
  }

  return `${parts.year}.${parts.month}.${parts.day} ${parts.hour}:${parts.minute}:${parts.second}`
}

export function formatDateInput(value: string | Date | null | undefined): string {
  const parts = getKoreanDateParts(value)

  if (!parts) {
    return ''
  }

  return `${parts.year}-${parts.month}-${parts.day}`
}

export function getMonthStartInput(baseDate: string | Date = new Date()): string {
  const date = parseDate(baseDate)

  if (!date) {
    return ''
  }

  return `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}-01`
}

export function getWeekRange(baseDate: string | Date = new Date()) {
  const date = parseDate(baseDate) ?? new Date()
  const currentDay = date.getDay()
  const diffToMonday = currentDay === 0 ? -6 : 1 - currentDay
  const start = new Date(date)
  start.setHours(0, 0, 0, 0)
  start.setDate(start.getDate() + diffToMonday)

  const end = new Date(start)
  end.setDate(start.getDate() + 6)
  end.setHours(23, 59, 59, 999)

  return { start, end }
}

export function isDateInCurrentWeek(value: string | Date | null | undefined, baseDate: string | Date = new Date()): boolean {
  const date = parseDate(value)

  if (!date) {
    return false
  }

  const { start, end } = getWeekRange(baseDate)
  return date >= start && date <= end
}

export function sortByDateDesc<T>(items: T[], selector: (item: T) => string | Date | null | undefined): T[] {
  return [...items].sort((left, right) => {
    const leftDate = parseDate(selector(left))
    const rightDate = parseDate(selector(right))
    const leftTime = leftDate?.getTime() ?? 0
    const rightTime = rightDate?.getTime() ?? 0

    return rightTime - leftTime
  })
}
