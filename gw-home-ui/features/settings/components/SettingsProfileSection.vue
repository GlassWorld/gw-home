<script setup lang="ts">
defineProps<{
  currentNickname: string
  nickname: string
  nicknameErrorMessage: string
  isNicknameSubmitting: boolean
}>()

const emit = defineEmits<{
  updateNickname: [value: string]
  submit: []
}>()
</script>

<template>
  <section class="settings-section content-panel page-panel-padding-lg">
    <div class="page-section-header">
      <div>
        <h2>이름 변경</h2>
        <p class="message-muted">헤더와 서비스 전반에 표시되는 닉네임을 수정합니다.</p>
      </div>
    </div>

    <div class="page-form-grid">
      <label class="settings-section__field">
        <span>현재 닉네임</span>
        <input class="input-field" type="text" :value="currentNickname" disabled>
      </label>

      <label class="settings-section__field">
        <span>새 닉네임</span>
        <input
          class="input-field"
          type="text"
          maxlength="50"
          placeholder="새 닉네임을 입력해 주세요"
          :value="nickname"
          @input="emit('updateNickname', ($event.target as HTMLInputElement).value)"
        >
      </label>
    </div>

    <p v-if="nicknameErrorMessage" class="message-error">
      {{ nicknameErrorMessage }}
    </p>

    <div class="page-actions">
      <CommonBaseButton :disabled="isNicknameSubmitting || !nickname.trim()" @click="emit('submit')">
        {{ isNicknameSubmitting ? '저장 중...' : '닉네임 저장' }}
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
