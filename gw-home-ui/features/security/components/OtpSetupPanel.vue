<script setup lang="ts">
const props = withDefaults(defineProps<{
  otpAuthUrl: string
  qrCodeDataUrl: string
  activationOtpCode: string
  activationErrorMessage?: string
  isSetupSubmitting?: boolean
  isActivateSubmitting?: boolean
  setupButtonLabel?: string
  activateButtonLabel?: string
  setupDescription?: string
  activateDescription?: string
  showSetupAction?: boolean
}>(), {
  activationErrorMessage: '',
  isSetupSubmitting: false,
  isActivateSubmitting: false,
  setupButtonLabel: 'OTP 설정 시작',
  activateButtonLabel: 'OTP 활성화',
  setupDescription: 'QR 스캔이 어렵다면 아래 URL을 수동 등록에 사용할 수 있습니다.',
  activateDescription: '앱에 표시된 6자리 코드를 입력하면 설정이 완료됩니다.',
  showSetupAction: true
})

const emit = defineEmits<{
  'update:activationOtpCode': [value: string]
  setup: []
  activate: []
}>()
</script>

<template>
  <div class="otp-setup">
    <div v-if="props.showSetupAction" class="page-section-header">
      <div>
        <h2>OTP 설정 시작</h2>
        <p class="message-muted">앱에서 QR 코드를 스캔한 뒤 6자리 코드를 입력해 활성화하세요.</p>
      </div>

      <CommonBaseButton :disabled="props.isSetupSubmitting" @click="emit('setup')">
        {{ props.isSetupSubmitting ? '설정 준비 중...' : props.setupButtonLabel }}
      </CommonBaseButton>
    </div>

    <div v-if="props.otpAuthUrl" class="otp-setup-grid">
      <div class="otp-setup-panel">
        <img v-if="props.qrCodeDataUrl" :src="props.qrCodeDataUrl" alt="OTP QR 코드" class="otp-setup-qr-image">
        <p class="message-muted">{{ props.setupDescription }}</p>
        <code class="otp-setup-url">{{ props.otpAuthUrl }}</code>
      </div>

      <div class="otp-setup-panel">
        <h3>OTP 코드 확인</h3>
        <p class="message-muted">{{ props.activateDescription }}</p>
        <AuthOtpCodeInput
          :model-value="props.activationOtpCode"
          :disabled="props.isActivateSubmitting || props.isSetupSubmitting"
          @update:model-value="emit('update:activationOtpCode', $event)"
          @complete="emit('activate')"
        />
        <p v-if="props.activationErrorMessage" class="message-error">
          {{ props.activationErrorMessage }}
        </p>
        <CommonBaseButton
          block
          :disabled="props.activationOtpCode.length !== 6 || props.isActivateSubmitting || props.isSetupSubmitting"
          @click="emit('activate')"
        >
          {{ props.isActivateSubmitting ? '활성화 중...' : props.activateButtonLabel }}
        </CommonBaseButton>
      </div>
    </div>
  </div>
</template>
