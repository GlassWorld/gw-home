<script setup lang="ts">
import OtpDisableModal from '~/features/security/components/OtpDisableModal.vue'
import OtpSetupPanel from '~/features/security/components/OtpSetupPanel.vue'
import { useOtpDisableFlow } from '~/features/security/composables/use-otp-disable-flow'
import { useOtpSetupFlow } from '~/features/security/composables/use-otp-setup-flow'

definePageMeta({
  middleware: 'auth'
})

const { fetchOtpStatus } = useOtpApi()

const isLoading = ref(true)
const otpEnabled = ref(false)
const pageErrorMessage = ref('')
const {
  isSetupSubmitting,
  isActivateSubmitting,
  otpAuthUrl,
  qrCodeDataUrl,
  activationOtpCode,
  activationErrorMessage,
  handleSetupOtp,
  handleActivateOtp
} = useOtpSetupFlow()
const {
  isDisableSubmitting,
  isDisableModalVisible,
  disableOtpCode,
  disableErrorMessage,
  openDisableModal,
  closeDisableModal,
  handleDisableOtp
} = useOtpDisableFlow()

async function loadOtpStatus() {
  isLoading.value = true
  pageErrorMessage.value = ''

  try {
    const response = await fetchOtpStatus()
    otpEnabled.value = response.otpEnabled
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    pageErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? '보안 설정을 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

async function startOtpSetup() {
  await handleSetupOtp({
    setupSuccessMessage: 'OTP 설정을 시작했습니다. 앱에서 QR 코드를 스캔해 주세요.'
  })
}

async function submitOtpActivation() {
  await handleActivateOtp({
    activateSuccessMessage: 'OTP가 활성화되었습니다.',
    onActivated: () => {
      otpEnabled.value = true
    }
  })
}

async function submitOtpDisable() {
  await handleDisableOtp({
    disableSuccessMessage: 'OTP가 해제되었습니다.',
    onDisabled: () => {
      otpEnabled.value = false
    }
  })
}

await loadOtpStatus()
</script>

<template>
  <main class="page-container security-page">
    <section class="security-page__hero content-panel page-panel-padding-lg page-hero-split">
      <div>
        <p class="security-page__eyebrow">Security</p>
        <h1 class="section-title">2차 인증 설정</h1>
        <p class="section-description">
          로그인 시 Google Authenticator 호환 OTP 코드를 한 번 더 확인하도록 설정할 수 있습니다.
        </p>
      </div>

      <div class="security-page__status-card page-info-card">
        <span>현재 상태</span>
        <strong>{{ otpEnabled ? 'OTP 사용 중' : 'OTP 미설정' }}</strong>
        <p>{{ otpEnabled ? '로그인 후 6자리 코드를 추가로 입력합니다.' : '보안 강화를 위해 OTP 설정을 권장합니다.' }}</p>
      </div>
    </section>

    <section class="security-page__section content-panel page-panel-padding-lg">
      <p v-if="pageErrorMessage" class="message-error">
        {{ pageErrorMessage }}
      </p>

      <p v-else-if="isLoading" class="message-muted">
        OTP 상태를 확인하고 있습니다.
      </p>

      <template v-else-if="!otpEnabled">
        <OtpSetupPanel
          :otp-auth-url="otpAuthUrl"
          :qr-code-data-url="qrCodeDataUrl"
          :activation-otp-code="activationOtpCode"
          :activation-error-message="activationErrorMessage"
          :is-setup-submitting="isSetupSubmitting"
          :is-activate-submitting="isActivateSubmitting"
          @update:activation-otp-code="activationOtpCode = $event"
          @setup="startOtpSetup"
          @activate="submitOtpActivation"
        />
      </template>

      <template v-else>
        <div class="security-page__section-header page-section-header">
          <div>
            <h2>OTP 사용 중</h2>
            <p class="message-muted">해제하려면 현재 앱의 6자리 OTP 코드를 다시 입력해야 합니다.</p>
          </div>

          <CommonBaseButton variant="secondary" @click="openDisableModal">
            OTP 해제
          </CommonBaseButton>
        </div>
      </template>
    </section>

    <OtpDisableModal
      :visible="isDisableModalVisible"
      :disable-otp-code="disableOtpCode"
      :disable-error-message="disableErrorMessage"
      :is-disable-submitting="isDisableSubmitting"
      description="Google Authenticator 앱에 표시된 현재 OTP 코드를 입력하면 즉시 해제됩니다."
      @update:disable-otp-code="disableOtpCode = $event"
      @close="closeDisableModal"
      @submit="submitOtpDisable"
    />
  </main>
</template>

<style scoped>
.security-page {
  display: grid;
  gap: 24px;
}

.security-page__eyebrow {
  margin: 0 0 10px;
  color: var(--color-accent);
  font-weight: 700;
}

@media (max-width: 768px) {
  .security-page__section-header {
    margin-bottom: 0;
  }
}
</style>
