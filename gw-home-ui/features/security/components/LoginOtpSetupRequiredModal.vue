<script setup lang="ts">
import OtpSetupPanel from '~/features/security/components/OtpSetupPanel.vue'

const props = defineProps<{
  visible: boolean
  otpAuthUrl: string
  qrCodeDataUrl: string
  activationOtpCode: string
  activationErrorMessage: string
  isSetupSubmitting: boolean
  isActivateSubmitting: boolean
}>()

const emit = defineEmits<{
  setup: []
  activate: []
  updateActivationOtpCode: [value: string]
}>()
</script>

<template>
  <CommonBaseModal
    :visible="props.visible"
    eyebrow="Security"
    title="OTP 등록이 필요합니다"
    width="680px"
    @close="void 0"
  >
    <div class="login-otp-setup-modal">
      <div class="login-otp-setup-modal__header">
        <p class="message-muted">
          로그인하려면 먼저 OTP를 등록해야 합니다. 앱에서 QR 코드를 스캔하고 6자리 코드를 입력해 주세요.
        </p>
      </div>

      <OtpSetupPanel
        :otp-auth-url="props.otpAuthUrl"
        :qr-code-data-url="props.qrCodeDataUrl"
        :activation-otp-code="props.activationOtpCode"
        :activation-error-message="props.activationErrorMessage"
        :is-setup-submitting="props.isSetupSubmitting"
        :is-activate-submitting="props.isActivateSubmitting"
        :show-setup-action="false"
        activate-button-label="OTP 등록 완료"
        activate-description="앱에 표시된 6자리 코드를 입력하면 등록이 완료됩니다."
        @update:activation-otp-code="emit('updateActivationOtpCode', $event)"
        @activate="emit('activate')"
      />
    </div>

    <template #actions>
      <div class="login-otp-setup-modal__actions">
        <CommonBaseButton variant="secondary" :disabled="props.isSetupSubmitting" @click="emit('setup')">
          {{ props.isSetupSubmitting ? 'QR 생성 중...' : 'QR 다시 생성' }}
        </CommonBaseButton>
        <CommonBaseButton
          :disabled="props.activationOtpCode.length !== 6 || props.isActivateSubmitting || props.isSetupSubmitting"
          @click="emit('activate')"
        >
          {{ props.isActivateSubmitting ? '등록 중...' : 'OTP 등록 완료' }}
        </CommonBaseButton>
      </div>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.login-otp-setup-modal {
  display: grid;
  gap: 18px;
}

.login-otp-setup-modal__header {
  display: grid;
  gap: 8px;
}

.login-otp-setup-modal__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

@media (max-width: 768px) {
  .login-otp-setup-modal__actions {
    grid-template-columns: 1fr;
  }
}
</style>
