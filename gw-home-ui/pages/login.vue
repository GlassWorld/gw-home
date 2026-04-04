<script setup lang="ts">
import LoginOtpSetupRequiredModal from '~/features/security/components/LoginOtpSetupRequiredModal.vue'
import LoginOtpVerificationPanel from '~/features/security/components/LoginOtpVerificationPanel.vue'
import { useOtpSetupFlow } from '~/features/security/composables/use-otp-setup-flow'

definePageMeta({
  middleware: 'guest'
})

type LoginStep = 'credentials' | 'otp'

const { login } = useAuth()
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
</script>

<template>
  <main class="login-page">
    <section class="login-page__shell">
      <header class="login-page__hero">
        <p class="login-page__brand">Glass World</p>
        <p class="login-page__subtitle">유리 세계에 오신 것을 환영합니다</p>
      </header>

      <div class="login-card">
        <p class="login-card__description">
          🌏Wellcome...
        </p>
        <AuthLoginForm
          v-if="loginStep === 'credentials'"
          :error-message="errorMessage"
          :is-submitting="isSubmitting"
          @submit="handleLogin"
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
.login-page {
  flex: 1;
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  overflow: hidden;
  isolation: isolate;
  background: transparent;
}

.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  pointer-events: none;
}

.login-page::before {
  inset: 0;
  background: linear-gradient(180deg, rgba(10, 20, 42, 0.02) 0%, rgba(10, 20, 42, 0.14) 100%);
  opacity: 0.72;
}

.login-page::after {
  inset: -12%;
  background:
    radial-gradient(circle at 18% 18%, rgba(161, 228, 255, 0.24) 0%, rgba(161, 228, 255, 0) 20%),
    radial-gradient(circle at 78% 20%, rgba(138, 200, 255, 0.18) 0%, rgba(138, 200, 255, 0) 20%),
    radial-gradient(circle at 52% 82%, rgba(105, 238, 224, 0.16) 0%, rgba(105, 238, 224, 0) 22%);
  filter: blur(46px);
  opacity: 0.82;
}

.login-page__shell {
  position: relative;
  z-index: 1;
  width: min(100%, 540px);
  display: grid;
  gap: 24px;
}

.login-page__hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
}

.login-page__brand {
  margin: 0;
  color: #ffffff;
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(3.2rem, 10vw, 5.5rem);
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 0.08em;
  text-shadow:
    0 0 18px rgba(190, 214, 255, 0.36),
    0 0 44px rgba(100, 116, 255, 0.22),
    0 12px 36px rgba(7, 30, 52, 0.55);
}

.login-page__subtitle {
  margin: 0;
  color: rgba(203, 220, 255, 0.8);
  font-size: clamp(1rem, 2.8vw, 1.2rem);
  letter-spacing: 0.18em;
}

.login-card {
  width: min(100%, 420px);
  justify-self: center;
  padding: 26px;
  border: 1px solid rgba(168, 191, 255, 0.26);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(17, 27, 58, 0.86) 0%, rgba(10, 17, 40, 0.82) 100%);
  box-shadow:
    0 30px 80px rgba(2, 8, 24, 0.62),
    inset 0 1px 0 rgba(255, 255, 255, 0.14),
    inset 0 0 0 1px rgba(255, 255, 255, 0.03),
    0 0 36px rgba(102, 101, 255, 0.14);
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
}

.login-card__description {
  margin: 0 0 20px;
  color: rgba(219, 241, 255, 0.72);
  line-height: 1.6;
  text-align: center;
}

.login-page__copyright {
  margin: -8px 0 0;
  color: rgba(219, 241, 255, 0.46);
  font-size: 0.78rem;
  letter-spacing: 0.04em;
  text-align: center;
}

@media (max-width: 768px) {
  .login-page {
    padding: 20px 16px;
  }

  .login-page__shell {
    gap: 20px;
  }

  .login-card {
    padding: 22px 18px;
  }

  .login-card__description {
    margin-bottom: 18px;
    font-size: 0.95rem;
  }
}
</style>
