<script setup lang="ts">
const savedLoginIdStorageKey = 'saved-login-id'

const emit = defineEmits<{
  submit: [payload: { loginId: string; password: string }]
  googleLogin: []
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
      <label for="login-id">아이디</label>
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
      <label for="password">비밀번호</label>
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

    <p v-if="props.errorMessage" class="message-error" role="alert">
      {{ props.errorMessage }}
    </p>

    <CommonBaseButton class="login-form__submit" block type="submit" :loading="props.isSubmitting">
      {{ props.isSubmitting ? '로그인 중...' : '로그인' }}
    </CommonBaseButton>

    <div class="login-form__divider">
      <span>또는</span>
    </div>

    <CommonBaseButton
      class="login-form__google"
      block
      type="button"
      variant="secondary"
      :disabled="props.isSubmitting"
      @click="emit('googleLogin')"
    >
      Google로 로그인
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
  color: var(--color-text-muted);
  font-size: 0.92rem;
  width: fit-content;
  min-height: 44px;
}

.login-form__checkbox input {
  appearance: none;
  width: 18px;
  height: 18px;
  margin: 0;
  border: 1px solid var(--color-border-strong);
  border-radius: 3px;
  background: var(--color-surface);
  display: grid;
  place-items: center;
  box-shadow: var(--shadow-soft);
}

.login-form__checkbox input::after {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: var(--color-primary);
  transform: scale(0);
  transition: transform 0.16s ease;
}

.login-form__checkbox input:checked::after {
  transform: scale(1);
}

.login-form__checkbox input:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.login-form__group label {
  color: var(--color-text-muted);
  font-weight: 700;
  font-size: 0.875rem;
}

.login-form__caps-warning {
  margin: 0;
  color: var(--color-warning);
  font-size: 0.88rem;
  line-height: 1.4;
}

.login-form :deep(.message-error) {
  border-color: var(--color-danger);
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.login-form :deep(.base-button--disabled) {
  cursor: wait;
  opacity: 0.72;
  transform: none;
}

.login-form__divider {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 12px;
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.login-form__divider::before,
.login-form__divider::after {
  content: '';
  height: 1px;
  background: var(--color-surface-muted);
}

.login-form__google {
  text-transform: none;
  letter-spacing: 0;
}
</style>
