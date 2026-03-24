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
    <section class="login-page__shell">
      <header class="login-page__hero">
        <p class="login-page__brand">Glass World</p>
        <p class="login-page__subtitle">유리 세계에 오신 것을 환영합니다</p>
      </header>

      <div class="login-card">
        <p class="login-card__description">
          🌏Wellcome...
        </p>
        <AuthLoginForm
          :error-message="errorMessage"
          :is-submitting="isSubmitting"
          @submit="handleLogin"
        />
      </div>

      <p class="login-page__copyright">Copyright 2026 chjsa11</p>
    </section>
  </main>
</template>

<style scoped>
:global(body) {
  background: linear-gradient(135deg, #0a1628 0%, #0d2847 50%, #0a3d62 100%);
}

.login-page {
  min-height: 100vh;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  overflow: hidden;
}

.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(10px);
}

.login-page::before {
  width: 320px;
  height: 320px;
  top: 8%;
  left: 6%;
  background: radial-gradient(circle, rgba(110, 193, 255, 0.3) 0%, rgba(110, 193, 255, 0) 72%);
}

.login-page::after {
  width: 420px;
  height: 420px;
  right: -8%;
  bottom: -10%;
  background: radial-gradient(circle, rgba(13, 148, 214, 0.28) 0%, rgba(13, 148, 214, 0) 70%);
}

.login-page__shell {
  position: relative;
  z-index: 1;
  width: min(100%, 540px);
  display: grid;
  gap: 24px;
}

.login-page__hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
}

.login-page__brand {
  margin: 0;
  color: #ffffff;
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(3.2rem, 10vw, 5.5rem);
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 0.04em;
  text-shadow:
    0 0 22px rgba(147, 210, 255, 0.35),
    0 12px 36px rgba(7, 30, 52, 0.55);
}

.login-page__subtitle {
  margin: 0;
  color: rgba(147, 210, 255, 0.82);
  font-size: clamp(1rem, 2.8vw, 1.2rem);
  letter-spacing: 0.08em;
}

.login-card {
  width: min(100%, 420px);
  justify-self: center;
  padding: 26px;
  border: 1px solid rgba(147, 210, 255, 0.25);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow:
    0 24px 60px rgba(2, 12, 27, 0.45),
    0 0 0 1px rgba(147, 210, 255, 0.08),
    0 0 28px rgba(54, 152, 219, 0.22);
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
}

.login-card__description {
  margin: 0 0 20px;
  color: rgba(219, 241, 255, 0.76);
  line-height: 1.6;
  text-align: center;
}

.login-page__copyright {
  margin: -8px 0 0;
  color: rgba(219, 241, 255, 0.46);
  font-size: 0.78rem;
  letter-spacing: 0.04em;
  text-align: center;
}

@media (max-width: 768px) {
  .login-page {
    padding: 20px 16px;
  }

  .login-page__shell {
    gap: 20px;
  }

  .login-card {
    padding: 22px 18px;
  }

  .login-card__description {
    margin-bottom: 18px;
    font-size: 0.95rem;
  }
}
</style>
