<script setup lang="ts">
const props = withDefaults(defineProps<{
  visible: boolean
  disableOtpCode: string
  disableErrorMessage?: string
  isDisableSubmitting?: boolean
  title?: string
  eyebrow?: string
  description?: string
}>(), {
  disableErrorMessage: '',
  isDisableSubmitting: false,
  title: 'OTP 해제',
  eyebrow: 'Security',
  description: '현재 인증 앱에 표시된 OTP 코드를 입력하면 즉시 해제됩니다.'
})

const emit = defineEmits<{
  close: []
  submit: []
  'update:disableOtpCode': [value: string]
}>()
</script>

<template>
  <CommonBaseModal
    :visible="props.visible"
    :title="props.title"
    :eyebrow="props.eyebrow"
    width="520px"
    @close="emit('close')"
  >
    <div class="otp-disable-modal">
      <p class="message-muted">
        {{ props.description }}
      </p>
      <AuthOtpCodeInput
        :model-value="props.disableOtpCode"
        :disabled="props.isDisableSubmitting"
        @update:model-value="emit('update:disableOtpCode', $event)"
        @complete="emit('submit')"
      />
      <p v-if="props.disableErrorMessage" class="message-error">
        {{ props.disableErrorMessage }}
      </p>
    </div>

    <template #actions>
      <CommonBaseButton variant="secondary" :disabled="props.isDisableSubmitting" @click="emit('close')">
        취소
      </CommonBaseButton>
      <CommonBaseButton
        :disabled="props.disableOtpCode.length !== 6 || props.isDisableSubmitting"
        @click="emit('submit')"
      >
        {{ props.isDisableSubmitting ? '해제 중...' : 'OTP 해제' }}
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.otp-disable-modal {
  display: grid;
  gap: 16px;
}
</style>
