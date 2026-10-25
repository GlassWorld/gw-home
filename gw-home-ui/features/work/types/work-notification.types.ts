export interface WorkNotificationSettings {
  email: string
  verified: boolean
  timeZone: string
  digestTime: string
  digestCycle: 'DAILY' | 'WEEKDAYS'
  digestEnabled: boolean
  reminderEnabled: boolean
  serverReady: boolean
  nextDigestAt: string | null
}
export type WorkNotificationSettingsPayload = Pick<WorkNotificationSettings, 'email' | 'timeZone' | 'digestTime' | 'digestCycle' | 'digestEnabled' | 'reminderEnabled'>
export interface WorkTodoReminder {
  reminderUuid: string | null
  todoUuid: string
  enabled: boolean
  sendAt: string | null
  timeZone: string
}
export type WorkDeliveryStatus = 'PENDING' | 'CLAIMED' | 'SENDING' | 'SENT' | 'RETRY' | 'FAILED' | 'CANCELLED' | 'UNKNOWN'
export interface WorkNotificationDelivery {
  deliveryUuid: string
  kind: 'DIGEST' | 'REMINDER'
  status: WorkDeliveryStatus
  attempts: number
  dueAt: string
  nextTryAt: string
  sentAt: string | null
  resultCode: string | null
}
