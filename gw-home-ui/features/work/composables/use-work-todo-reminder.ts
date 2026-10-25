import { useWorkNotificationApi } from '~/composables/use-work-notification-api'
import type { WorkNotificationSettings, WorkTodoReminder } from '../types/work-notification.types'
import { notificationInstant, notificationLocalTime } from '../utils/notification-time'

export function useWorkTodoReminder(todoUuid: string) {
  const api = useWorkNotificationApi()
  const { showToast } = useToast()
  const settings = ref<WorkNotificationSettings | null>(null)
  const reminder = ref<WorkTodoReminder | null>(null)
  const localTime = ref('')
  const loading = ref(true)
  const busy = ref(false)
  const error = ref('')
  const timeZone = computed(() => settings.value?.timeZone ?? 'Asia/Seoul')
  const ready = computed(() => settings.value?.serverReady && settings.value.verified && settings.value.reminderEnabled)
  function apply(value: WorkTodoReminder) {
    reminder.value = value
    localTime.value = value.sendAt ? notificationLocalTime(value.sendAt, value.timeZone) : ''
  }
  function errorText(failure: unknown) {
    const value = failure as { data?: { message?: string }; message?: string }
    return value.data?.message ?? value.message ?? '개별 알림을 처리하지 못했습니다.'
  }
  async function load() {
    loading.value = true; error.value = ''
    try {
      const [setting, value] = await Promise.all([api.getSettings(), api.getReminder(todoUuid)])
      settings.value = setting; apply(value)
    } catch (failure) { error.value = errorText(failure) }
    finally { loading.value = false }
  }
  async function save() {
    if (busy.value) return
    busy.value = true; error.value = ''
    try {
      const instant = notificationInstant(localTime.value, timeZone.value)
      if (new Date(instant).getTime() <= Date.now()) throw new Error('미래 발송 시각을 지정하세요.')
      apply(await api.saveReminder(todoUuid, instant))
      showToast('개별 알림을 예약했습니다.', { variant: 'success' })
    } catch (failure) { error.value = errorText(failure) }
    finally { busy.value = false }
  }
  async function cancel() {
    if (busy.value) return
    busy.value = true; error.value = ''
    try {
      await api.cancelReminder(todoUuid)
      apply(await api.getReminder(todoUuid))
      showToast('개별 알림 예약을 취소했습니다.', { variant: 'success' })
    } catch (failure) { error.value = errorText(failure) }
    finally { busy.value = false }
  }
  onMounted(() => { void load() })
  return { settings, reminder, localTime, loading, busy, error, timeZone, ready, load, save, cancel }
}
