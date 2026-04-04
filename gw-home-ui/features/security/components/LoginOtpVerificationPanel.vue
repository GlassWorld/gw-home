<script setup lang="ts">
const props = defineProps<{
  otpCode: string
  errorMessage: string
  isSubmitting: boolean
}>()

const emit = defineEmits<{
  back: []
  verify: []
  updateOtpCode: [value: string]
}>()
</script>

<template>
  <div class="login-otp-panel">
    <div class="login-otp-panel__header">
      <h2>OTP 인증</h2>
      <p>Google Authenticator 앱에 표시된 6자리 코드를 입력해 주세요.</p>
    </div>

    <AuthOtpCodeInput
      :model-value="props.otpCode"
      :disabled="props.isSubmitting"
      @update:model-value="emit('updateOtpCode', $event)"
      @complete="emit('verify')"
    />

    <p v-if="props.errorMessage" class="message-error">
      {{ props.errorMessage }}
    </p>

    <div class="login-otp-panel__actions">
      <CommonBaseButton
        variant="secondary"
        :disabled="props.isSubmitting"
        @click="emit('back')"
      >
        뒤로가기
      </CommonBaseButton>
      <CommonBaseButton
        :disabled="props.otpCode.length !== 6 || props.isSubmitting"
        @click="emit('verify')"
      >
        {{ props.isSubmitting ? '인증 중...' : 'OTP 인증' }}
      </CommonBaseButton>
    </div>
  </div>
</template>

<style scoped>
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

@media (max-width: 768px) {
  .login-otp-panel__actions {
    grid-template-columns: 1fr;
  }
}
</style>
