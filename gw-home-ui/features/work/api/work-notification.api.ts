import type { ApiResponse, PageResponse } from '~/types/api/common'
import type { WorkNotificationSettings, WorkNotificationSettingsPayload, WorkTodoReminder, WorkNotificationDelivery } from '../types/work-notification.types'

interface SettingsApi {
  email: string; verified: boolean; time_zone: string; digest_time: string; digest_cycle: 'DAILY' | 'WEEKDAYS'
  digest_enabled: boolean; reminder_enabled: boolean; server_ready: boolean; next_digest_at: string | null
}
interface ReminderApi { reminder_uuid: string | null; todo_uuid: string; enabled: boolean; send_at: string | null; time_zone: string }
interface DeliveryApi {
  delivery_uuid: string; kind: 'DIGEST' | 'REMINDER'; status: WorkNotificationDelivery['status']; attempts: number
  due_at: string; next_try_at: string; sent_at: string | null; result_code: string | null
}
function settings(value: SettingsApi): WorkNotificationSettings {
  return { email: value.email, verified: value.verified, timeZone: value.time_zone, digestTime: value.digest_time,
    digestCycle: value.digest_cycle, digestEnabled: value.digest_enabled, reminderEnabled: value.reminder_enabled,
    serverReady: value.server_ready, nextDigestAt: value.next_digest_at }
}
function reminder(value: ReminderApi): WorkTodoReminder {
  return { reminderUuid: value.reminder_uuid, todoUuid: value.todo_uuid, enabled: value.enabled, sendAt: value.send_at, timeZone: value.time_zone }
}
export function useWorkNotificationApi() {
  const { authorizedFetch } = useAuth()
  async function request<T>(path: string, options: { method?: 'GET' | 'PUT' | 'POST' | 'DELETE'; body?: Record<string, unknown>; query?: Record<string, number> } = {}): Promise<T> {
    const response = await authorizedFetch<ApiResponse<T>>(path, options)
    if (!response.success) throw new Error(response.message || '메일 알림 요청을 처리하지 못했습니다.')
    return response.data
  }
  const base = '/api/v1/work-notifications'
  const reminderPath = (todoUuid: string) => `/api/v1/work-todos/${encodeURIComponent(todoUuid)}/reminder`
  return {
    getSettings: async () => settings(await request<SettingsApi>(base + '/settings')),
    saveSettings: async (value: WorkNotificationSettingsPayload) => settings(await request<SettingsApi>(base + '/settings', {
      method: 'PUT', body: { email: value.email, time_zone: value.timeZone, digest_time: value.digestTime,
        digest_cycle: value.digestCycle, digest_enabled: value.digestEnabled, reminder_enabled: value.reminderEnabled }
    })),
    requestVerification: () => request<'SENT' | 'RETRY' | 'FAILED' | 'UNKNOWN'>(base + '/verification', { method: 'POST' }),
    verifyAddress: (code: string) => request<{ verified: boolean }>(base + '/verification/confirm', { method: 'POST', body: { code } }),
    getReminder: async (todoUuid: string) => reminder(await request<ReminderApi>(reminderPath(todoUuid))),
    saveReminder: async (todoUuid: string, sendAt: string) => reminder(await request<ReminderApi>(reminderPath(todoUuid), { method: 'PUT', body: { send_at: sendAt } })),
    cancelReminder: (todoUuid: string) => request<null>(reminderPath(todoUuid), { method: 'DELETE' }),
    getHistory: async (page: number): Promise<PageResponse<WorkNotificationDelivery>> => {
      const value = await request<{ content: DeliveryApi[]; page: number; size: number; total_count: number; total_pages: number }>(base + '/deliveries', { query: { page, size: 10 } })
      return { content: value.content.map(item => ({ deliveryUuid: item.delivery_uuid, kind: item.kind, status: item.status,
        attempts: item.attempts, dueAt: item.due_at, nextTryAt: item.next_try_at, sentAt: item.sent_at, resultCode: item.result_code })),
        page: value.page, size: value.size, totalCount: value.total_count, totalPages: value.total_pages }
    }
  }
}
