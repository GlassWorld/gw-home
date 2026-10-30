import type { ApiResponse } from '~/types/api/common'
import type { LifeDailyMonth } from '../types/life-daily.types'

interface MonthResponse {
  month: string; today: string; time_zone: string
  items: { item_uuid: string; title: string; start_date: string; end_date: string | null }[]
  checks: { item_uuid: string; date: string }[]
}

export function useLifeDailyApi() {
  const { authorizedFetch } = useAuth()
  const base = '/api/v1/life-daily'
  const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Seoul'

  async function fetchMonth(month: string): Promise<LifeDailyMonth> {
    const response = await authorizedFetch<ApiResponse<MonthResponse>>(base, { method: 'GET', query: { month, timeZone } })
    if (!response.success || !response.data) throw new Error('매일 체크를 불러오지 못했습니다.')
    const data = response.data
    return { month: data.month, today: data.today, timeZone: data.time_zone,
      items: data.items.map(item => ({ itemUuid: item.item_uuid, title: item.title, startDate: item.start_date, endDate: item.end_date })),
      checks: data.checks.map(check => ({ itemUuid: check.item_uuid, date: check.date })) }
  }

  async function save(path: string, method: 'POST' | 'PUT' | 'DELETE', body?: { title: string } | { completed: boolean }) {
    const response = await authorizedFetch<ApiResponse<unknown>>(`${base}${path}`, { method, query: { timeZone }, body })
    if (!response.success) throw new Error(response.message || '저장하지 못했습니다.')
  }

  return { fetchMonth,
    createItem: (title: string) => save('', 'POST', { title }),
    renameItem: (uuid: string, title: string) => save(`/${uuid}`, 'PUT', { title }),
    deleteItem: (uuid: string) => save(`/${uuid}`, 'DELETE'),
    saveCheck: (uuid: string, date: string, completed: boolean) => save(`/${uuid}/checks/${date}`, 'PUT', { completed }) }
}
