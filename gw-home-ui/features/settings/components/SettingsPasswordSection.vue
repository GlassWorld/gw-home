<script setup lang="ts">
defineProps<{
  currentPassword: string
  newPassword: string
  newPasswordConfirm: string
  passwordErrorMessage: string
  passwordConfirmMessage: string
  isPasswordSubmitting: boolean
}>()

const emit = defineEmits<{
  updateCurrentPassword: [value: string]
  updateNewPassword: [value: string]
  updateNewPasswordConfirm: [value: string]
  submit: []
}>()
</script>

<template>
  <section class="settings-section content-panel page-panel-padding-lg">
    <div class="page-section-header">
      <div>
        <h2>비밀번호 변경</h2>
        <p class="message-muted">현재 비밀번호를 확인한 뒤 새 비밀번호로 교체합니다.</p>
      </div>
    </div>

    <div class="page-form-grid">
      <label class="settings-section__field">
        <span>현재 비밀번호</span>
        <input
          class="input-field"
          type="password"
          autocomplete="current-password"
          placeholder="현재 비밀번호"
          :value="currentPassword"
          @input="emit('updateCurrentPassword', ($event.target as HTMLInputElement).value)"
        >
      </label>

      <label class="settings-section__field">
        <span>새 비밀번호</span>
        <input
          class="input-field"
          type="password"
          autocomplete="new-password"
          placeholder="8자 이상 새 비밀번호"
          :value="newPassword"
          @input="emit('updateNewPassword', ($event.target as HTMLInputElement).value)"
        >
      </label>

      <label class="settings-section__field">
        <span>새 비밀번호 확인</span>
        <input
          class="input-field"
          type="password"
          autocomplete="new-password"
          placeholder="새 비밀번호를 다시 입력해 주세요"
          :value="newPasswordConfirm"
          @input="emit('updateNewPasswordConfirm', ($event.target as HTMLInputElement).value)"
        >
      </label>
    </div>

    <p v-if="passwordErrorMessage" class="message-error">
      {{ passwordErrorMessage }}
    </p>
    <p v-else-if="passwordConfirmMessage" class="message-error">
      {{ passwordConfirmMessage }}
    </p>

    <div class="page-actions">
      <CommonBaseButton
        :disabled="isPasswordSubmitting || !currentPassword || !newPassword || !newPasswordConfirm"
        @click="emit('submit')"
      >
        {{ isPasswordSubmitting ? '변경 중...' : '비밀번호 변경' }}
      </CommonBaseButton>
    </div>
  </section>
</template>

<style scoped>
.settings-section__field {
  display: grid;
  gap: 8px;
}

.settings-section__field span {
  color: rgba(147, 210, 255, 0.82);
  font-weight: 700;
}
</style>
