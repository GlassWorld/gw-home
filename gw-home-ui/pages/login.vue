<script setup lang="ts">
import LoginOtpSetupRequiredModal from '~/features/security/components/LoginOtpSetupRequiredModal.vue'
import LoginOtpVerificationPanel from '~/features/security/components/LoginOtpVerificationPanel.vue'
import { useOtpSetupFlow } from '~/features/security/composables/use-otp-setup-flow'

definePageMeta({
  middleware: 'guest'
})

type LoginStep = 'credentials' | 'otp'

const { login, ensureAuthenticated, isOtpSetupPending, getGoogleLoginAuthorizationUrl } = useAuth()
const { verifyOtp } = useOtpApi()
const errorMessage = ref('')
const isSubmitting = ref(false)
const loginStep = ref<LoginStep>('credentials')
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
  handleSetupOtp: startOtpSetupFlow,
  handleActivateOtp: submitOtpActivationFlow
} = useOtpSetupFlow()

async function handleLogin(payload: { loginId: string; password: string }) {
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    const response = await login(payload.loginId, payload.password)

    if (response.status === 'OTP_REQUIRED') {
      loginStep.value = 'otp'
      otpTempToken.value = response.otpTempToken
      otpCode.value = ''
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
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    errorMessage.value = fetchError.data?.message ?? fetchError.message ?? '로그인에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

async function handleGoogleLogin() {
  if (!import.meta.client || isSubmitting.value) {
    return
  }

  errorMessage.value = ''
  isSubmitting.value = true

  try {
    const redirectUri = `${window.location.origin}/auth/google/callback`
    const response = await getGoogleLoginAuthorizationUrl(redirectUri)
    sessionStorage.setItem('gw-home-google-oauth-state', response.state)
    sessionStorage.setItem('gw-home-google-oauth-mode', 'login')
    window.location.href = response.authorization_url
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    errorMessage.value = fetchError.data?.message ?? fetchError.message ?? 'Google 로그인을 시작하지 못했습니다.'
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

function handleBackToCredentials() {
  if (isSubmitting.value) {
    return
  }

  loginStep.value = 'credentials'
  otpTempToken.value = ''
  otpCode.value = ''
  errorMessage.value = ''
}

async function handleSetupOtp() {
  await startOtpSetupFlow()
}

async function handleActivateOtp() {
  await submitOtpActivationFlow({
    onActivated: async () => {
      isOtpSetupRequired.value = false
      await navigateTo('/dashboard')
    }
  })
}

onMounted(async () => {
  if (!isOtpSetupPending()) {
    return
  }

  const isAuthenticated = await ensureAuthenticated()

  if (isAuthenticated || !isOtpSetupPending()) {
    return
  }

  isOtpSetupRequired.value = true
  activationOtpCode.value = ''
  otpSetupErrorMessage.value = ''
  await handleSetupOtp()
})
</script>

<template>
  <main class="login-page">
    <section class="login-page__shell">
      <header class="login-page__hero">
        <p class="login-page__eyebrow">YOUR PERSONAL WORKSPACE</p>
        <h1 class="login-page__brand">Glass World</h1>
        <p class="login-page__subtitle">일상을 정리하는 나만의 업무 공간</p>
      </header>

      <div class="login-card">
        <p class="login-card__description">
          로그인하고 내 공간을 이어가세요.
        </p>
        <AuthLoginForm
          v-if="loginStep === 'credentials'"
          :error-message="errorMessage"
          :is-submitting="isSubmitting"
          @submit="handleLogin"
          @google-login="handleGoogleLogin"
        />

        <LoginOtpVerificationPanel
          v-else
          :otp-code="otpCode"
          :error-message="errorMessage"
          :is-submitting="isSubmitting"
          @update-otp-code="otpCode = $event"
          @back="handleBackToCredentials"
          @verify="handleVerifyOtp"
        />
      </div>

      <p class="login-page__copyright">Copyright 2026 chjsa11</p>
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
.login-page { display: grid; flex: 1; place-items: center; width: 100%; min-height: 100vh; min-height: 100dvh; padding: 48px var(--panel-padding); }
.login-page__shell { display: grid; gap: var(--section-gap); width: min(100%, 420px); }
.login-page__hero { display: grid; gap: 12px; text-align: center; }
.login-page__eyebrow { margin: 0; color: var(--color-primary); font-size: 0.6875rem; font-weight: 700; letter-spacing: 0.16em; }
.login-page__brand { margin: 0; color: var(--color-text); font-size: clamp(2rem, 8vw, 2.75rem); font-weight: 750; letter-spacing: -0.055em; line-height: 1.2; }
.login-page__subtitle { margin: 0; color: var(--color-text-muted); font-size: 0.9375rem; }
.login-card { min-width: 0; padding: var(--panel-padding-large); border: 1px solid var(--color-border); border-radius: var(--radius-large); background: var(--color-surface); box-shadow: var(--shadow-soft); }
.login-card__description { margin: 0 0 24px; color: var(--color-text-muted); font-size: 0.875rem; text-align: center; }
.login-page__copyright { margin: 0; color: var(--color-text-muted); font-size: 0.75rem; text-align: center; }
@media (max-width: 767px) {
  .login-page { padding: max(var(--panel-padding), env(safe-area-inset-top)) max(var(--page-gutter), env(safe-area-inset-left)) max(var(--panel-padding), env(safe-area-inset-bottom)) max(var(--page-gutter), env(safe-area-inset-right)); }
  .login-page__shell { gap: var(--section-gap); }
  .login-card { padding: var(--panel-padding) var(--card-padding); }
}
</style>
