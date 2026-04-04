type DateInput = string | Date | null | undefined

function toValidDate(value: DateInput): Date | null {
  if (!value) {
    return null
  }

  const parsedDate = value instanceof Date ? new Date(value) : new Date(value)

  if (Number.isNaN(parsedDate.getTime())) {
    return null
  }

  return parsedDate
}

export function formatDate(value: DateInput, options?: Intl.DateTimeFormatOptions): string {
  const parsedDate = toValidDate(value)

  if (!parsedDate) {
    return '-'
  }

  return parsedDate.toLocaleDateString('ko-KR', options)
}

export function formatDateInput(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function getMonthStartInput(date = new Date()): string {
  return formatDateInput(new Date(date.getFullYear(), date.getMonth(), 1))
}

export function formatDateTime(value: DateInput, options?: Intl.DateTimeFormatOptions): string {
  const parsedDate = toValidDate(value)

  if (!parsedDate) {
    return '-'
  }

  return options
    ? new Intl.DateTimeFormat('ko-KR', options).format(parsedDate)
    : parsedDate.toLocaleString('ko-KR')
}

export function sortByDateDesc<T>(items: T[], selector: (item: T) => DateInput): T[] {
  return [...items].sort((left, right) => {
    const leftTime = toValidDate(selector(left))?.getTime() ?? 0
    const rightTime = toValidDate(selector(right))?.getTime() ?? 0
    return rightTime - leftTime
  })
}

export function getWeekRange(date = new Date()) {
  const current = new Date(date)
  const day = current.getDay()
  const diffToMonday = day === 0 ? -6 : 1 - day
  const start = new Date(current)
  start.setDate(current.getDate() + diffToMonday)
  start.setHours(0, 0, 0, 0)

  const end = new Date(start)
  end.setDate(start.getDate() + 6)
  end.setHours(23, 59, 59, 999)

  return { start, end }
}

export function isDateInCurrentWeek(value: DateInput): boolean {
  const target = toValidDate(value)

  if (!target) {
    return false
  }

  const { start, end } = getWeekRange()
  return target >= start && target <= end
}
