import QRCode from 'qrcode'

interface OtpSetupFlowOptions {
  setupSuccessMessage?: string
  activateSuccessMessage?: string
  setupFailureMessage?: string
  activateFailureMessage?: string
  onActivated?: () => Promise<void> | void
}

export function useOtpSetupFlow() {
  const { showToast } = useToast()
  const { setupOtp, activateOtp } = useOtpApi()

  const isSetupSubmitting = ref(false)
  const isActivateSubmitting = ref(false)
  const otpAuthUrl = ref('')
  const qrCodeDataUrl = ref('')
  const activationOtpCode = ref('')
  const activationErrorMessage = ref('')

  async function handleSetupOtp(options: OtpSetupFlowOptions = {}) {
    if (isSetupSubmitting.value) {
      return
    }

    isSetupSubmitting.value = true
    activationErrorMessage.value = ''

    try {
      const response = await setupOtp()
      otpAuthUrl.value = response.otpAuthUrl
      activationOtpCode.value = ''
      qrCodeDataUrl.value = await QRCode.toDataURL(response.otpAuthUrl, {
        width: 220,
        margin: 1
      })

      if (options.setupSuccessMessage) {
        showToast(options.setupSuccessMessage, { variant: 'info' })
      }
    } catch (error) {
      const fetchError = error as { data?: { message?: string }; message?: string }
      activationErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? options.setupFailureMessage ?? 'OTP 설정을 시작하지 못했습니다.'
    } finally {
      isSetupSubmitting.value = false
    }
  }

  async function handleActivateOtp(options: OtpSetupFlowOptions = {}) {
    if (isActivateSubmitting.value || activationOtpCode.value.length !== 6) {
      return false
    }

    isActivateSubmitting.value = true
    activationErrorMessage.value = ''

    try {
      await activateOtp(activationOtpCode.value)
      clearSetupState()

      if (options.activateSuccessMessage) {
        showToast(options.activateSuccessMessage, { variant: 'success' })
      }

      await options.onActivated?.()
      return true
    } catch (error) {
      const fetchError = error as { data?: { message?: string }; message?: string }
      activationErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? options.activateFailureMessage ?? 'OTP 활성화에 실패했습니다.'
      return false
    } finally {
      isActivateSubmitting.value = false
    }
  }

  function clearSetupState() {
    otpAuthUrl.value = ''
    qrCodeDataUrl.value = ''
    activationOtpCode.value = ''
    activationErrorMessage.value = ''
  }

  return {
    isSetupSubmitting,
    isActivateSubmitting,
    otpAuthUrl,
    qrCodeDataUrl,
    activationOtpCode,
    activationErrorMessage,
    handleSetupOtp,
    handleActivateOtp,
    clearSetupState
  }
}
