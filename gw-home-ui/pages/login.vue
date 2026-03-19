<script setup lang="ts">
definePageMeta({
  middleware: 'guest'
})

const errorMessage = ref('')
const isSubmitting = ref(false)

async function handleLogin(payload: { loginId: string; password: string }) {
  const { login } = useAuth()

  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await login(payload.loginId, payload.password)
    await navigateTo('/dashboard')
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    errorMessage.value = fetchError.data?.message ?? fetchError.message ?? '로그인에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-page__panel content-panel">
      <div class="login-page__intro">
        <p class="login-page__eyebrow">GW HOME COMMUNITY</p>
        <h1 class="section-title">로그인 후 대시보드로 바로 이어집니다.</h1>
        <p class="section-description">
          현재 백엔드 인증 스펙에 맞춰 Login ID와 비밀번호로 로그인합니다.
        </p>
      </div>

      <LoginForm
        :error-message="errorMessage"
        :is-submitting="isSubmitting"
        @submit="handleLogin"
      />
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-page__panel {
  width: min(100%, 960px);
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 36px;
  padding: 38px;
}

.login-page__intro {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-page__eyebrow {
  margin: 0 0 14px;
  color: var(--color-accent);
  font-weight: 700;
  letter-spacing: 0.12em;
}

@media (max-width: 768px) {
  .login-page__panel {
    grid-template-columns: 1fr;
    padding: 24px;
  }
}
</style>
