<script setup lang="ts">
const emit = defineEmits<{
  submit: [payload: { loginId: string; password: string }]
}>()

const props = defineProps<{
  errorMessage?: string
  isSubmitting?: boolean
}>()

const loginId = ref('')
const password = ref('')

function handleSubmit() {
  emit('submit', {
    loginId: loginId.value,
    password: password.value
  })
}
</script>

<template>
  <form class="login-form" @submit.prevent="handleSubmit">
    <div class="login-form__group">
      <label for="login-id">Login ID</label>
      <input
        id="login-id"
        v-model="loginId"
        class="input-field"
        type="text"
        name="loginId"
        autocomplete="username"
        placeholder="tester_01"
        required
      >
    </div>

    <div class="login-form__group">
      <label for="password">비밀번호</label>
      <input
        id="password"
        v-model="password"
        class="input-field"
        type="password"
        name="password"
        autocomplete="current-password"
        placeholder="비밀번호를 입력해 주세요"
        required
      >
    </div>

    <p v-if="props.errorMessage" class="message-error">
      {{ props.errorMessage }}
    </p>

    <button class="button-primary login-form__submit" type="submit" :disabled="props.isSubmitting">
      {{ props.isSubmitting ? '로그인 중...' : '로그인' }}
    </button>
  </form>
</template>

<style scoped>
.login-form {
  display: grid;
  gap: 18px;
}

.login-form__group {
  display: grid;
  gap: 8px;
}

.login-form__group label {
  font-weight: 700;
}

.login-form__submit {
  width: 100%;
}
</style>
