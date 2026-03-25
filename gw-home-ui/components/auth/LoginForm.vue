<script setup lang="ts">
const savedLoginIdStorageKey = 'saved-login-id'

const emit = defineEmits<{
  submit: [payload: { loginId: string; password: string }]
}>()

const props = defineProps<{
  errorMessage?: string
  isSubmitting?: boolean
}>()

const loginId = ref('')
const password = ref('')
const isLoginIdSaved = ref(false)
const isCapsLockOn = ref(false)

onMounted(() => {
  const savedLoginId = localStorage.getItem(savedLoginIdStorageKey)

  if (!savedLoginId) {
    return
  }

  loginId.value = savedLoginId
  isLoginIdSaved.value = true
})

function handleSavedLoginIdChange() {
  if (!isLoginIdSaved.value) {
    localStorage.removeItem(savedLoginIdStorageKey)
    return
  }

  localStorage.setItem(savedLoginIdStorageKey, loginId.value)
}

function handlePasswordKeyup(event: KeyboardEvent) {
  isCapsLockOn.value = event.getModifierState('CapsLock')
}

function handleSubmit() {
  if (isLoginIdSaved.value) {
    localStorage.setItem(savedLoginIdStorageKey, loginId.value)
  } else {
    localStorage.removeItem(savedLoginIdStorageKey)
  }

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
        placeholder="계정을 입력해 주세요"
        required
      >
    </div>

    <div class="login-form__group">
      <label for="password">Password</label>
      <input
        id="password"
        v-model="password"
        class="input-field"
        type="password"
        name="password"
        autocomplete="current-password"
        placeholder="비밀번호를 입력해 주세요"
        @keyup="handlePasswordKeyup"
        required
      >
      <p v-if="isCapsLockOn" class="login-form__caps-warning">
        Caps Lock이 켜져 있습니다
      </p>
    </div>

    <label class="login-form__checkbox">
      <input
        v-model="isLoginIdSaved"
        type="checkbox"
        name="saveLoginId"
        @change="handleSavedLoginIdChange"
      >
      <span>로그인 아이디 저장</span>
    </label>

    <p v-if="props.errorMessage" class="message-error">
      {{ props.errorMessage }}
    </p>

    <CommonBaseButton class="login-form__submit" block type="submit" :disabled="props.isSubmitting">
      {{ props.isSubmitting ? '로그인 중...' : '로그인' }}
    </CommonBaseButton>
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

.login-form__checkbox {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: rgba(219, 241, 255, 0.78);
  font-size: 0.92rem;
  width: fit-content;
}

.login-form__checkbox input {
  appearance: none;
  width: 18px;
  height: 18px;
  margin: 0;
  border: 1px solid rgba(147, 210, 255, 0.5);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.08);
  display: grid;
  place-items: center;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.login-form__checkbox input::after {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: linear-gradient(135deg, #8fddff 0%, #4aa8ea 100%);
  transform: scale(0);
  transition: transform 0.16s ease;
}

.login-form__checkbox input:checked::after {
  transform: scale(1);
}

.login-form__checkbox input:focus-visible {
  outline: 2px solid rgba(95, 186, 255, 0.28);
  outline-offset: 2px;
}

.login-form__group label {
  color: rgba(147, 210, 255, 0.8);
  font-weight: 700;
  letter-spacing: 0.02em;
}

.login-form__caps-warning {
  margin: 0;
  color: #ffd089;
  font-size: 0.88rem;
  line-height: 1.4;
}

.login-form :deep(.input-field) {
  min-height: 52px;
  border-color: rgba(147, 210, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.login-form :deep(.input-field::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.login-form :deep(.input-field:focus) {
  outline: 2px solid rgba(95, 186, 255, 0.28);
  border-color: rgba(124, 209, 255, 0.88);
  box-shadow:
    0 0 0 4px rgba(15, 92, 158, 0.16),
    0 10px 24px rgba(5, 28, 52, 0.24);
}

.login-form :deep(.message-error) {
  border-color: rgba(255, 161, 161, 0.35);
  background: rgba(122, 28, 28, 0.26);
  color: #ffe2e2;
}

.login-form :deep(.base-button--primary) {
  border: 1px solid rgba(147, 210, 255, 0.18);
  background: linear-gradient(135deg, #1a7fc4 0%, #0d5c9e 100%);
  box-shadow:
    0 18px 34px rgba(10, 65, 114, 0.34),
    0 0 24px rgba(26, 127, 196, 0.22);
}

.login-form :deep(.base-button--primary:hover) {
  box-shadow:
    0 20px 36px rgba(10, 65, 114, 0.4),
    0 0 26px rgba(26, 127, 196, 0.28);
}

.login-form :deep(.base-button--disabled) {
  cursor: wait;
  opacity: 0.72;
  transform: none;
}
</style>
