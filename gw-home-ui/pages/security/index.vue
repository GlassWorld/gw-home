<script setup lang="ts">
import QRCode from 'qrcode'

definePageMeta({
  middleware: 'auth'
})

const { showToast } = useToast()
const { fetchOtpStatus, setupOtp, activateOtp, disableOtp } = useOtpApi()

const isLoading = ref(true)
const isSetupSubmitting = ref(false)
const isActivateSubmitting = ref(false)
const isDisableSubmitting = ref(false)
const isDisableModalVisible = ref(false)
const otpEnabled = ref(false)
const otpAuthUrl = ref('')
const qrCodeDataUrl = ref('')
const activationOtpCode = ref('')
const disableOtpCode = ref('')
const pageErrorMessage = ref('')
const activationErrorMessage = ref('')
const disableErrorMessage = ref('')

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

async function handleSetupOtp() {
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
    showToast('OTP 설정을 시작했습니다. 앱에서 QR 코드를 스캔해 주세요.', { variant: 'info' })
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    activationErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? 'OTP 설정을 시작하지 못했습니다.'
  } finally {
    isSetupSubmitting.value = false
  }
}

async function handleActivateOtp() {
  if (isActivateSubmitting.value || activationOtpCode.value.length !== 6) {
    return
  }

  isActivateSubmitting.value = true
  activationErrorMessage.value = ''

  try {
    await activateOtp(activationOtpCode.value)
    otpEnabled.value = true
    otpAuthUrl.value = ''
    qrCodeDataUrl.value = ''
    activationOtpCode.value = ''
    showToast('OTP가 활성화되었습니다.', { variant: 'success' })
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    activationErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? 'OTP 활성화에 실패했습니다.'
  } finally {
    isActivateSubmitting.value = false
  }
}

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

async function handleDisableOtp() {
  if (isDisableSubmitting.value || disableOtpCode.value.length !== 6) {
    return
  }

  isDisableSubmitting.value = true
  disableErrorMessage.value = ''

  try {
    await disableOtp(disableOtpCode.value)
    otpEnabled.value = false
    otpAuthUrl.value = ''
    qrCodeDataUrl.value = ''
    closeDisableModal()
    showToast('OTP가 해제되었습니다.', { variant: 'success' })
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    disableErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? 'OTP 해제에 실패했습니다.'
  } finally {
    isDisableSubmitting.value = false
  }
}

await loadOtpStatus()
</script>

<template>
  <main class="page-container security-page">
    <section class="security-page__hero content-panel">
      <div>
        <p class="security-page__eyebrow">Security</p>
        <h1 class="section-title">2차 인증 설정</h1>
        <p class="section-description">
          로그인 시 Google Authenticator 호환 OTP 코드를 한 번 더 확인하도록 설정할 수 있습니다.
        </p>
      </div>

      <div class="security-page__status-card">
        <span>현재 상태</span>
        <strong>{{ otpEnabled ? 'OTP 사용 중' : 'OTP 미설정' }}</strong>
        <p>{{ otpEnabled ? '로그인 후 6자리 코드를 추가로 입력합니다.' : '보안 강화를 위해 OTP 설정을 권장합니다.' }}</p>
      </div>
    </section>

    <section class="security-page__section content-panel">
      <p v-if="pageErrorMessage" class="message-error">
        {{ pageErrorMessage }}
      </p>

      <p v-else-if="isLoading" class="message-muted">
        OTP 상태를 확인하고 있습니다.
      </p>

      <template v-else-if="!otpEnabled">
        <div class="security-page__section-header">
          <div>
            <h2>OTP 설정 시작</h2>
            <p class="message-muted">앱에서 QR 코드를 스캔한 뒤 6자리 코드를 입력해 활성화하세요.</p>
          </div>

          <CommonBaseButton :disabled="isSetupSubmitting" @click="handleSetupOtp">
            {{ isSetupSubmitting ? '설정 준비 중...' : 'OTP 설정 시작' }}
          </CommonBaseButton>
        </div>

        <div v-if="otpAuthUrl" class="security-page__setup-grid">
          <div class="security-page__qr-panel">
            <img v-if="qrCodeDataUrl" :src="qrCodeDataUrl" alt="OTP QR 코드" class="security-page__qr-image">
            <p class="message-muted">QR 스캔이 어렵다면 아래 URL을 수동 등록에 사용할 수 있습니다.</p>
            <code class="security-page__otpauth-url">{{ otpAuthUrl }}</code>
          </div>

          <div class="security-page__activate-panel">
            <h3>OTP 코드 확인</h3>
            <p class="message-muted">앱에 표시된 6자리 코드를 입력하면 설정이 완료됩니다.</p>
            <AuthOtpCodeInput
              v-model="activationOtpCode"
              :disabled="isActivateSubmitting"
              @complete="handleActivateOtp"
            />
            <p v-if="activationErrorMessage" class="message-error">
              {{ activationErrorMessage }}
            </p>
            <CommonBaseButton
              block
              :disabled="activationOtpCode.length !== 6 || isActivateSubmitting"
              @click="handleActivateOtp"
            >
              {{ isActivateSubmitting ? '활성화 중...' : 'OTP 활성화' }}
            </CommonBaseButton>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="security-page__section-header">
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

    <CommonBaseModal
      :visible="isDisableModalVisible"
      title="OTP 해제"
      eyebrow="Security"
      width="520px"
      @close="closeDisableModal"
    >
      <div class="security-page__disable-modal">
        <p class="message-muted">
          Google Authenticator 앱에 표시된 현재 OTP 코드를 입력하면 즉시 해제됩니다.
        </p>
        <AuthOtpCodeInput
          v-model="disableOtpCode"
          :disabled="isDisableSubmitting"
          @complete="handleDisableOtp"
        />
        <p v-if="disableErrorMessage" class="message-error">
          {{ disableErrorMessage }}
        </p>
      </div>

      <template #actions>
        <CommonBaseButton variant="secondary" :disabled="isDisableSubmitting" @click="closeDisableModal">
          취소
        </CommonBaseButton>
        <CommonBaseButton
          :disabled="disableOtpCode.length !== 6 || isDisableSubmitting"
          @click="handleDisableOtp"
        >
          {{ isDisableSubmitting ? '해제 중...' : 'OTP 해제' }}
        </CommonBaseButton>
      </template>
    </CommonBaseModal>
  </main>
</template>

<style scoped>
.security-page {
  display: grid;
  gap: 24px;
}

.security-page__hero,
.security-page__section {
  padding: 28px;
}

.security-page__hero {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 22px;
  align-items: stretch;
}

.security-page__eyebrow {
  margin: 0 0 10px;
  color: var(--color-accent);
  font-weight: 700;
}

.security-page__status-card {
  display: grid;
  gap: 8px;
  padding: 22px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(95, 186, 255, 0.16), transparent 55%),
    rgba(255, 255, 255, 0.05);
}

.security-page__status-card span,
.security-page__status-card p {
  color: var(--color-text-muted);
}

.security-page__status-card strong {
  font-size: 1.5rem;
}

.security-page__section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.security-page__section-header h2,
.security-page__activate-panel h3 {
  margin: 0;
}

.security-page__setup-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.security-page__qr-panel,
.security-page__activate-panel,
.security-page__disable-modal {
  display: grid;
  gap: 16px;
}

.security-page__qr-panel,
.security-page__activate-panel {
  padding: 22px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.05);
}

.security-page__qr-image {
  width: min(100%, 220px);
  border-radius: 18px;
  background: #ffffff;
  padding: 10px;
}

.security-page__otpauth-url {
  display: block;
  overflow-wrap: anywhere;
  padding: 14px;
  border-radius: 16px;
  background: rgba(7, 21, 39, 0.65);
  color: #bfe7ff;
  font-size: 0.88rem;
  line-height: 1.5;
}

@media (max-width: 900px) {
  .security-page__hero,
  .security-page__setup-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .security-page__hero,
  .security-page__section {
    padding: 22px;
  }

  .security-page__section-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
