<script setup lang="ts">
import OtpSetupPanel from '~/features/security/components/OtpSetupPanel.vue'
import type { WritableComputedRef } from 'vue'

defineProps<{
  securityErrorMessage: string
  isSecurityLoading: boolean
  otpEnabled: boolean
  isSetupSubmitting: boolean
  isActivateSubmitting: boolean
  otpAuthUrl: string
  qrCodeDataUrl: string
  activationOtpCode: string
  activationErrorMessage: string
}>()

const emit = defineEmits<{
  startSetup: []
  activate: []
  openDisableModal: []
  updateActivationOtpCode: [value: string]
}>()
</script>

<template>
  <section class="settings-section content-panel page-panel-padding-lg">
    <div class="page-section-header">
      <div>
        <h2>보안 설정</h2>
        <p class="message-muted">Google Authenticator 호환 OTP 2차 인증을 관리합니다.</p>
      </div>
    </div>

    <p v-if="securityErrorMessage" class="message-error">
      {{ securityErrorMessage }}
    </p>

    <p v-else-if="isSecurityLoading" class="message-muted">
      OTP 상태를 확인하고 있습니다.
    </p>

    <template v-else-if="!otpEnabled">
      <div class="settings-section__security-card">
        <div>
          <h3>OTP 미설정</h3>
          <p class="message-muted">설정을 시작하면 QR 코드와 수동 등록 URL이 함께 표시됩니다.</p>
        </div>

        <CommonBaseButton :disabled="isSetupSubmitting" @click="emit('startSetup')">
          {{ isSetupSubmitting ? '준비 중...' : 'OTP 설정 시작' }}
        </CommonBaseButton>
      </div>

      <OtpSetupPanel
        :otp-auth-url="otpAuthUrl"
        :qr-code-data-url="qrCodeDataUrl"
        :activation-otp-code="activationOtpCode"
        :activation-error-message="activationErrorMessage"
        :is-setup-submitting="isSetupSubmitting"
        :is-activate-submitting="isActivateSubmitting"
        :show-setup-action="false"
        activate-description="앱에 표시된 6자리 코드를 입력해 활성화를 완료합니다."
        @update:activation-otp-code="emit('updateActivationOtpCode', $event)"
        @activate="emit('activate')"
      />
    </template>

    <template v-else>
      <div class="settings-section__security-card">
        <div>
          <h3>OTP 사용 중</h3>
          <p class="message-muted">로그인 시 6자리 OTP 코드가 추가로 필요합니다.</p>
        </div>

        <CommonBaseButton variant="secondary" @click="emit('openDisableModal')">
          OTP 해제
        </CommonBaseButton>
      </div>
    </template>
  </section>
</template>

<style scoped>
.settings-section__security-card {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 16px;
  padding: 22px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.05);
}

.settings-section__security-card h3 {
  margin: 0;
}

@media (max-width: 900px) {
  .settings-section__security-card {
    align-items: stretch;
  }
}
</style>
