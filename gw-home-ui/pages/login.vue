<script setup lang="ts">
import QRCode from 'qrcode'

definePageMeta({
  middleware: 'guest'
})

type LoginStep = 'credentials' | 'otp'

const { login } = useAuth()
const { verifyOtp, setupOtp, activateOtp } = useOtpApi()
const errorMessage = ref('')
const isSubmitting = ref(false)
const loginStep = ref<LoginStep>('credentials')
const otpTempToken = ref('')
const otpCode = ref('')
const isOtpSetupRequired = ref(false)
const isOtpSetupSubmitting = ref(false)
const isOtpActivateSubmitting = ref(false)
const otpAuthUrl = ref('')
const qrCodeDataUrl = ref('')
const activationOtpCode = ref('')
const otpSetupErrorMessage = ref('')

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
  if (isOtpSetupSubmitting.value) {
    return
  }

  isOtpSetupSubmitting.value = true
  otpSetupErrorMessage.value = ''

  try {
    const response = await setupOtp()
    otpAuthUrl.value = response.otpAuthUrl
    qrCodeDataUrl.value = await QRCode.toDataURL(response.otpAuthUrl, {
      width: 220,
      margin: 1
    })
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    otpSetupErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? 'OTP 설정을 시작하지 못했습니다.'
  } finally {
    isOtpSetupSubmitting.value = false
  }
}

async function handleActivateOtp() {
  if (isOtpActivateSubmitting.value || activationOtpCode.value.length !== 6) {
    return
  }

  isOtpActivateSubmitting.value = true
  otpSetupErrorMessage.value = ''

  try {
    await activateOtp(activationOtpCode.value)
    isOtpSetupRequired.value = false
    otpAuthUrl.value = ''
    qrCodeDataUrl.value = ''
    activationOtpCode.value = ''
    await navigateTo('/dashboard')
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    otpSetupErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? 'OTP 활성화에 실패했습니다.'
  } finally {
    isOtpActivateSubmitting.value = false
  }
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

        <div v-else class="login-otp-panel">
          <div class="login-otp-panel__header">
            <h2>OTP 인증</h2>
            <p>Google Authenticator 앱에 표시된 6자리 코드를 입력해 주세요.</p>
          </div>

          <AuthOtpCodeInput
            v-model="otpCode"
            :disabled="isSubmitting"
            @complete="handleVerifyOtp"
          />

          <p v-if="errorMessage" class="message-error">
            {{ errorMessage }}
          </p>

          <div class="login-otp-panel__actions">
            <CommonBaseButton
              variant="secondary"
              :disabled="isSubmitting"
              @click="handleBackToCredentials"
            >
              뒤로가기
            </CommonBaseButton>
            <CommonBaseButton
              :disabled="otpCode.length !== 6 || isSubmitting"
              @click="handleVerifyOtp"
            >
              {{ isSubmitting ? '인증 중...' : 'OTP 인증' }}
            </CommonBaseButton>
          </div>
        </div>
      </div>

      <p class="login-page__copyright">Copyright 2026 chjsa11</p>
    </section>

    <CommonBaseModal
      :visible="isOtpSetupRequired"
      eyebrow="Security"
      title="OTP 등록이 필요합니다"
      width="680px"
      @close="void 0"
    >
      <div class="login-page__otp-setup">
        <div class="login-page__otp-setup-header">
          <p class="message-muted">
            로그인하려면 먼저 OTP를 등록해야 합니다. 앱에서 QR 코드를 스캔하고 6자리 코드를 입력해 주세요.
          </p>
        </div>

        <div class="login-page__otp-setup-grid">
          <div class="login-page__otp-setup-panel">
            <img v-if="qrCodeDataUrl" :src="qrCodeDataUrl" alt="OTP QR 코드" class="login-page__otp-setup-qr">
            <p class="message-muted">QR 스캔이 어렵다면 아래 URL을 수동 등록에 사용할 수 있습니다.</p>
            <code class="login-page__otp-setup-url">{{ otpAuthUrl }}</code>
          </div>

          <div class="login-page__otp-setup-panel">
            <h3>OTP 코드 입력</h3>
            <p class="message-muted">앱에 표시된 6자리 코드를 입력하면 등록이 완료됩니다.</p>
            <AuthOtpCodeInput
              v-model="activationOtpCode"
              :disabled="isOtpActivateSubmitting || isOtpSetupSubmitting"
              @complete="handleActivateOtp"
            />
            <p v-if="otpSetupErrorMessage" class="message-error">
              {{ otpSetupErrorMessage }}
            </p>
          </div>
        </div>
      </div>

      <template #actions>
        <div class="login-page__otp-setup-actions">
          <CommonBaseButton variant="secondary" :disabled="isOtpSetupSubmitting" @click="handleSetupOtp">
            {{ isOtpSetupSubmitting ? 'QR 생성 중...' : 'QR 다시 생성' }}
          </CommonBaseButton>
          <CommonBaseButton
            :disabled="activationOtpCode.length !== 6 || isOtpActivateSubmitting || isOtpSetupSubmitting"
            @click="handleActivateOtp"
          >
            {{ isOtpActivateSubmitting ? '등록 중...' : 'OTP 등록 완료' }}
          </CommonBaseButton>
        </div>
      </template>
    </CommonBaseModal>
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
}

.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  pointer-events: none;
}

.login-page::before {
  inset: 0;
  background:
    radial-gradient(circle at 14% 18%, rgba(255, 255, 255, 0.9) 0 1px, transparent 1.6px),
    radial-gradient(circle at 78% 12%, rgba(255, 255, 255, 0.72) 0 1.2px, transparent 2px),
    radial-gradient(circle at 67% 28%, rgba(175, 224, 255, 0.78) 0 1px, transparent 1.8px),
    radial-gradient(circle at 24% 72%, rgba(255, 255, 255, 0.75) 0 1.1px, transparent 2px),
    radial-gradient(circle at 82% 68%, rgba(166, 215, 255, 0.78) 0 1.3px, transparent 2px),
    radial-gradient(circle at 38% 42%, rgba(255, 255, 255, 0.46) 0 0.8px, transparent 1.6px),
    radial-gradient(circle at 12% 86%, rgba(255, 255, 255, 0.52) 0 0.9px, transparent 1.8px),
    radial-gradient(circle at 92% 34%, rgba(255, 255, 255, 0.45) 0 0.9px, transparent 1.7px);
  opacity: 0.9;
}

.login-page::after {
  inset: -12%;
  background:
    radial-gradient(circle at 22% 24%, rgba(54, 153, 255, 0.26) 0%, rgba(54, 153, 255, 0) 24%),
    radial-gradient(circle at 76% 22%, rgba(170, 92, 255, 0.22) 0%, rgba(170, 92, 255, 0) 22%),
    radial-gradient(circle at 52% 82%, rgba(53, 212, 196, 0.16) 0%, rgba(53, 212, 196, 0) 20%);
  filter: blur(42px);
  opacity: 0.95;
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

.login-otp-panel {
  display: grid;
  gap: 18px;
}

.login-otp-panel__header {
  display: grid;
  gap: 8px;
  text-align: center;
}

.login-otp-panel__header h2,
.login-otp-panel__header p {
  margin: 0;
}

.login-otp-panel__header p {
  color: rgba(219, 241, 255, 0.76);
  line-height: 1.6;
}

.login-otp-panel :deep(.otp-code-input) {
  border-color: rgba(147, 210, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.login-otp-panel__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.login-page__otp-setup {
  display: grid;
  gap: 18px;
}

.login-page__otp-setup-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.login-page__otp-setup-header {
  display: grid;
  gap: 8px;
}

.login-page__otp-setup-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.login-page__otp-setup-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid rgba(168, 191, 255, 0.18);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(20, 29, 62, 0.82) 0%, rgba(12, 18, 43, 0.8) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 18px 40px rgba(4, 10, 28, 0.24);
}

.login-page__otp-setup-qr {
  width: min(100%, 220px);
  justify-self: center;
  padding: 10px;
  border-radius: 8px;
  background: #ffffff;
}

.login-page__otp-setup-url {
  display: block;
  max-height: 180px;
  padding: 12px;
  overflow: auto;
  border-radius: 8px;
  background: rgba(7, 18, 32, 0.84);
  color: rgba(232, 244, 255, 0.94);
  white-space: normal;
  word-break: break-all;
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

  .login-otp-panel__actions {
    grid-template-columns: 1fr;
  }

  .login-page__otp-setup-actions {
    grid-template-columns: 1fr;
  }

  .login-page__otp-setup-grid {
    grid-template-columns: 1fr;
  }
}
</style>
