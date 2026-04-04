interface OtpDisableFlowOptions {
  disableSuccessMessage?: string
  disableFailureMessage?: string
  onDisabled?: () => Promise<void> | void
}

export function useOtpDisableFlow() {
  const { showToast } = useToast()
  const { disableOtp } = useOtpApi()

  const isDisableSubmitting = ref(false)
  const isDisableModalVisible = ref(false)
  const disableOtpCode = ref('')
  const disableErrorMessage = ref('')

  function openDisableModal() {
    disableOtpCode.value = ''
    disableErrorMessage.value = ''
    isDisableModalVisible.value = true
  }

  function closeDisableModal() {
    if (isDisableSubmitting.value) {
      return
    }

    isDisableModalVisible.value = false
    disableOtpCode.value = ''
    disableErrorMessage.value = ''
  }

  async function handleDisableOtp(options: OtpDisableFlowOptions = {}) {
    if (isDisableSubmitting.value || disableOtpCode.value.length !== 6) {
      return false
    }

    isDisableSubmitting.value = true
    disableErrorMessage.value = ''

    try {
      await disableOtp(disableOtpCode.value)
      closeDisableModal()

      if (options.disableSuccessMessage) {
        showToast(options.disableSuccessMessage, { variant: 'success' })
      }

      await options.onDisabled?.()
      return true
    } catch (error) {
      const fetchError = error as { data?: { message?: string }; message?: string }
      disableErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? options.disableFailureMessage ?? 'OTP 해제에 실패했습니다.'
      return false
    } finally {
      isDisableSubmitting.value = false
    }
  }

  return {
    isDisableSubmitting,
    isDisableModalVisible,
    disableOtpCode,
    disableErrorMessage,
    openDisableModal,
    closeDisableModal,
    handleDisableOtp
  }
}
