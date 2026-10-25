import type { WorkNotificationSettings, WorkNotificationSettingsPayload, WorkNotificationDelivery } from '../types/work-notification.types'
import type { PageResponse } from '~/types/api/common'
import { useWorkNotificationApi } from '~/composables/use-work-notification-api'

export function useWorkNotificationSettings() {
  const api = useWorkNotificationApi()
  const { showToast } = useToast()
  const saved = ref<WorkNotificationSettings | null>(null)
  const form = reactive<WorkNotificationSettingsPayload>({ email: '', timeZone: 'Asia/Seoul', digestTime: '09:00', digestCycle: 'DAILY', digestEnabled: false, reminderEnabled: false })
  const code = ref('')
  const busy = ref(false)
  const loading = ref(true)
  const error = ref('')
  const verificationMessage = ref('')
  const history = ref<PageResponse<WorkNotificationDelivery> | null>(null)
  const historyError = ref('')
  const historyLoading = ref(false)
  const verified = computed(() => saved.value?.verified === true && form.email === saved.value.email)
  const canActivate = computed(() => verified.value && saved.value?.serverReady === true)
  const timeZones = ref<string[]>(['Asia/Seoul', 'UTC', 'America/New_York', 'Europe/London'])
  function apply(value: WorkNotificationSettings) { saved.value = value; Object.assign(form, value) }
  function errorText(failure: unknown): string {
    const requestError = failure as { data?: { message?: string }; message?: string }
    return requestError.data?.message ?? requestError.message ?? '메일 알림 설정을 처리하지 못했습니다.'
  }
  async function load() {
    loading.value = true; error.value = ''
    try { apply(await api.getSettings()) } catch (failure) { error.value = errorText(failure) }
    finally { loading.value = false }
  }
  async function action(operation: () => Promise<void>) {
    if (busy.value) return
    busy.value = true; error.value = ''
    try { await operation() } catch (failure) { error.value = errorText(failure) }
    finally { busy.value = false }
  }
  async function save() {
    await action(async () => {
      apply(await api.saveSettings(form))
      verificationMessage.value = ''
      showToast('메일 알림 설정을 저장했습니다.', { variant: 'success' })
      if (history.value) await loadHistory(1)
    })
  }
  async function requestCode() {
    await action(async () => {
      const result = await api.requestVerification()
      verificationMessage.value = result === 'SENT' ? '확인 코드를 보냈습니다. 10분 안에 입력하세요.'
        : result === 'UNKNOWN' ? '접수 결과를 확인할 수 없습니다. 수신함을 먼저 확인하고 60초 뒤 다시 요청하세요.'
          : '확인 메일을 보내지 못했습니다. 60초 뒤 다시 시도하세요.'
    })
  }
  async function verify() {
    await action(async () => {
      const result = await api.verifyAddress(code.value)
      if (!result.verified) { error.value = '코드가 일치하지 않거나 만료되었습니다. 최대 5회 확인 후 다시 요청하세요.'; return }
      code.value = ''; apply(await api.getSettings())
      verificationMessage.value = '수신 주소 확인을 마쳤습니다. 원하는 알림을 켜고 저장하세요.'
    })
  }
  async function loadHistory(page = 1) {
    historyLoading.value = true; historyError.value = ''
    try { history.value = await api.getHistory(page) } catch { historyError.value = '발송 내역을 불러오지 못했습니다.' }
    finally { historyLoading.value = false }
  }
  watch(() => form.email, email => {
    if (email !== saved.value?.email) { form.digestEnabled = false; form.reminderEnabled = false; verificationMessage.value = '' }
  })
  onMounted(() => {
    if (typeof Intl.supportedValuesOf === 'function') timeZones.value = ['UTC', ...Intl.supportedValuesOf('timeZone')]
    void load()
  })
  return { saved, form, code, busy, loading, error, verificationMessage, verified, canActivate, timeZones,
    history, historyError, historyLoading, load, save, requestCode, verify, loadHistory }
}
