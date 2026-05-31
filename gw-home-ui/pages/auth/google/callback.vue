<script setup lang="ts">
import LoginOtpSetupRequiredModal from '~/features/security/components/LoginOtpSetupRequiredModal.vue'
import LoginOtpVerificationPanel from '~/features/security/components/LoginOtpVerificationPanel.vue'
import { useOtpSetupFlow } from '~/features/security/composables/use-otp-setup-flow'

type CallbackMode = 'login' | 'link'
type CallbackStep = 'processing' | 'otp'

const route = useRoute()
const { loginWithGoogle } = useAuth()
const { linkGoogleAccount } = useSettingsApi()
const { verifyOtp } = useOtpApi()
const step = ref<CallbackStep>('processing')
const mode = ref<CallbackMode>('login')
const errorMessage = ref('')
const isSubmitting = ref(false)
const otpTempToken = ref('')
const otpCode = ref('')
const isOtpSetupRequired = ref(false)
const {
  isSetupSubmitting: isOtpSetupSubmitting,
  isActivateSubmitting: isOtpActivateSubmitting,
  otpAuthUrl,
  qrCodeDataUrl,
  activationOtpCode,
  activationErrorMessage: otpSetupErrorMessage,
  handleSetupOtp,
  handleActivateOtp: submitOtpActivationFlow
} = useOtpSetupFlow()

function getQueryValue(value: unknown): string {
  if (Array.isArray(value)) {
    return value[0] ?? ''
  }

  return typeof value === 'string' ? value : ''
}

async function processLogin(code: string, redirectUri: string) {
  const response = await loginWithGoogle(code, redirectUri)

  if (response.status === 'OTP_REQUIRED') {
    step.value = 'otp'
    otpTempToken.value = response.otpTempToken
    return
  }

  if (response.status === 'OTP_SETUP_REQUIRED') {
    isOtpSetupRequired.value = true
    activationOtpCode.value = ''
    otpSetupErrorMessage.value = ''
    await handleSetupOtp()
    return
  }

  await navigateTo('/dashboard')
}

async function processLink(code: string, redirectUri: string) {
  await linkGoogleAccount(code, redirectUri)
  await navigateTo('/settings?google_linked=success')
}

async function processCallback() {
  if (!import.meta.client) {
    return
  }

  const code = getQueryValue(route.query.code)
  const state = getQueryValue(route.query.state)
  const oauthError = getQueryValue(route.query.error)
  const savedState = sessionStorage.getItem('gw-home-google-oauth-state') ?? ''
  const savedMode = sessionStorage.getItem('gw-home-google-oauth-mode')

  mode.value = savedMode === 'link' ? 'link' : 'login'
  sessionStorage.removeItem('gw-home-google-oauth-state')
  sessionStorage.removeItem('gw-home-google-oauth-mode')

  if (oauthError) {
    errorMessage.value = 'Google 인증이 취소되었거나 실패했습니다.'
    return
  }

  if (!code || !state || state !== savedState) {
    errorMessage.value = 'Google 인증 상태를 확인할 수 없습니다.'
    return
  }

  isSubmitting.value = true

  try {
    const redirectUri = `${window.location.origin}/auth/google/callback`

    if (mode.value === 'link') {
      await processLink(code, redirectUri)
      return
    }

    await processLogin(code, redirectUri)
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    errorMessage.value = fetchError.data?.message ?? fetchError.message ?? 'Google 인증 처리에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

async function handleVerifyOtp() {
  if (isSubmitting.value || otpCode.value.length !== 6 || !otpTempToken.value) {
    return
  }

  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await verifyOtp(otpTempToken.value, otpCode.value)
    await navigateTo('/dashboard')
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    errorMessage.value = fetchError.data?.message ?? fetchError.message ?? 'OTP 인증에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

async function handleActivateOtp() {
  await submitOtpActivationFlow({
    onActivated: async () => {
      isOtpSetupRequired.value = false
      await navigateTo('/dashboard')
    }
  })
}

onMounted(processCallback)
</script>

<template>
  <main class="google-callback-page">
    <section class="google-callback-page__panel content-panel page-panel-padding-lg">
      <template v-if="step === 'processing'">
        <p class="google-callback-page__eyebrow">Google</p>
        <h1 class="section-title">{{ mode === 'link' ? '계정 연동 처리 중' : '로그인 처리 중' }}</h1>
        <p v-if="!errorMessage" class="message-muted">
          Google 인증 결과를 확인하고 있습니다.
        </p>
        <p v-else class="message-error">
          {{ errorMessage }}
        </p>
        <CommonBaseButton v-if="errorMessage" :to="mode === 'link' ? '/settings' : '/login'">
          돌아가기
        </CommonBaseButton>
      </template>

      <LoginOtpVerificationPanel
        v-else
        :otp-code="otpCode"
        :error-message="errorMessage"
        :is-submitting="isSubmitting"
        @update-otp-code="otpCode = $event"
        @back="navigateTo('/login')"
        @verify="handleVerifyOtp"
      />
    </section>

    <LoginOtpSetupRequiredModal
      :visible="isOtpSetupRequired"
      :otp-auth-url="otpAuthUrl"
      :qr-code-data-url="qrCodeDataUrl"
      :activation-otp-code="activationOtpCode"
      :activation-error-message="otpSetupErrorMessage"
      :is-setup-submitting="isOtpSetupSubmitting"
      :is-activate-submitting="isOtpActivateSubmitting"
      @update-activation-otp-code="activationOtpCode = $event"
      @setup="handleSetupOtp"
      @activate="handleActivateOtp"
    />
  </main>
</template>

<style scoped>
.google-callback-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.google-callback-page__panel {
  width: min(100%, 480px);
  display: grid;
  gap: 18px;
}

.google-callback-page__eyebrow {
  margin: 0;
  color: var(--color-accent);
  font-weight: 700;
}
</style>
