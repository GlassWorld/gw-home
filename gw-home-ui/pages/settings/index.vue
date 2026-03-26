<script setup lang="ts">
definePageMeta({
  middleware: 'auth'
})

const authStore = useAuthStore()
const { showToast } = useToast()
const { changeNickname, changePassword } = useSettingsApi()
const { fetchOtpStatus, setupOtp, activateOtp, disableOtp } = useOtpApi()

const nickname = ref(authStore.currentUser?.nickname ?? '')
const currentPassword = ref('')
const newPassword = ref('')
const newPasswordConfirm = ref('')
const isNicknameSubmitting = ref(false)
const isPasswordSubmitting = ref(false)
const nicknameErrorMessage = ref('')
const passwordErrorMessage = ref('')

const isSecurityLoading = ref(true)
const isSetupSubmitting = ref(false)
const isActivateSubmitting = ref(false)
const isDisableSubmitting = ref(false)
const isDisableModalVisible = ref(false)
const otpEnabled = ref(false)
const otpAuthUrl = ref('')
const activationOtpCode = ref('')
const disableOtpCode = ref('')
const securityErrorMessage = ref('')
const activationErrorMessage = ref('')
const disableErrorMessage = ref('')

watch(() => authStore.currentUser?.nickname, (currentNickname) => {
  nickname.value = currentNickname ?? ''
}, { immediate: true })

const passwordConfirmMessage = computed(() => {
  if (!newPasswordConfirm.value) {
    return ''
  }

  return newPassword.value === newPasswordConfirm.value
    ? ''
    : '새 비밀번호 확인이 일치하지 않습니다.'
})

async function handleChangeNickname() {
  if (isNicknameSubmitting.value) {
    return
  }

  nicknameErrorMessage.value = ''
  isNicknameSubmitting.value = true

  try {
    await changeNickname(nickname.value)
    showToast('닉네임이 변경되었습니다.', { variant: 'success' })
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    nicknameErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? '닉네임 변경에 실패했습니다.'
  } finally {
    isNicknameSubmitting.value = false
  }
}

async function handleChangePassword() {
  if (isPasswordSubmitting.value) {
    return
  }

  if (newPassword.value !== newPasswordConfirm.value) {
    passwordErrorMessage.value = '새 비밀번호 확인이 일치하지 않습니다.'
    return
  }

  passwordErrorMessage.value = ''
  isPasswordSubmitting.value = true

  try {
    await changePassword(currentPassword.value, newPassword.value)
    currentPassword.value = ''
    newPassword.value = ''
    newPasswordConfirm.value = ''
    showToast('비밀번호가 변경되었습니다.', { variant: 'success' })
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    passwordErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? '비밀번호 변경에 실패했습니다.'
  } finally {
    isPasswordSubmitting.value = false
  }
}

async function loadOtpStatus() {
  isSecurityLoading.value = true
  securityErrorMessage.value = ''

  try {
    const response = await fetchOtpStatus()
    otpEnabled.value = response.otpEnabled
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    securityErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? 'OTP 상태를 불러오지 못했습니다.'
  } finally {
    isSecurityLoading.value = false
  }
}

async function handleSetupOtp() {
  if (isSetupSubmitting.value) {
    return
  }

  isSetupSubmitting.value = true
  activationErrorMessage.value = ''

  try {
    const response = await setupOtp()
    otpAuthUrl.value = response.otpAuthUrl
    activationOtpCode.value = ''
    showToast('OTP 설정을 시작했습니다. 인증 앱에 등록해 주세요.', { variant: 'info' })
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    activationErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? 'OTP 설정을 시작하지 못했습니다.'
  } finally {
    isSetupSubmitting.value = false
  }
}

async function handleActivateOtp() {
  if (isActivateSubmitting.value || activationOtpCode.value.length !== 6) {
    return
  }

  isActivateSubmitting.value = true
  activationErrorMessage.value = ''

  try {
    await activateOtp(activationOtpCode.value)
    otpEnabled.value = true
    otpAuthUrl.value = ''
    activationOtpCode.value = ''
    showToast('OTP가 활성화되었습니다.', { variant: 'success' })
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    activationErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? 'OTP 활성화에 실패했습니다.'
  } finally {
    isActivateSubmitting.value = false
  }
}

function openDisableModal() {
  disableOtpCode.value = ''
  disableErrorMessage.value = ''
  isDisableModalVisible.value = true
}

function closeDisableModal() {
  if (isDisableSubmitting.value) {
    return
  }

  isDisableModalVisible.value = false
  disableOtpCode.value = ''
  disableErrorMessage.value = ''
}

async function handleDisableOtp() {
  if (isDisableSubmitting.value || disableOtpCode.value.length !== 6) {
    return
  }

  isDisableSubmitting.value = true
  disableErrorMessage.value = ''

  try {
    await disableOtp(disableOtpCode.value)
    otpEnabled.value = false
    otpAuthUrl.value = ''
    closeDisableModal()
    showToast('OTP가 해제되었습니다.', { variant: 'success' })
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    disableErrorMessage.value = fetchError.data?.message ?? fetchError.message ?? 'OTP 해제에 실패했습니다.'
  } finally {
    isDisableSubmitting.value = false
  }
}

await loadOtpStatus()
</script>

<template>
  <main class="page-container settings-page">
    <section class="settings-page__hero content-panel">
      <div>
        <p class="settings-page__eyebrow">Settings</p>
        <h1 class="section-title">계정 설정</h1>
        <p class="section-description">
          닉네임, 비밀번호, OTP 보안 설정을 한 곳에서 관리할 수 있습니다.
        </p>
      </div>

      <div class="settings-page__summary">
        <span>현재 계정</span>
        <strong>{{ authStore.currentUser?.nickname }}</strong>
        <p>{{ authStore.currentUser?.loginId }} · {{ authStore.currentUser?.email }}</p>
      </div>
    </section>

    <section class="settings-page__section content-panel">
      <div class="settings-page__section-header">
        <div>
          <h2>이름 변경</h2>
          <p class="message-muted">헤더와 서비스 전반에 표시되는 닉네임을 수정합니다.</p>
        </div>
      </div>

      <div class="settings-page__form-grid">
        <label class="settings-page__field">
          <span>현재 닉네임</span>
          <input class="input-field" type="text" :value="authStore.currentUser?.nickname ?? ''" disabled>
        </label>

        <label class="settings-page__field">
          <span>새 닉네임</span>
          <input
            v-model="nickname"
            class="input-field"
            type="text"
            maxlength="50"
            placeholder="새 닉네임을 입력해 주세요"
          >
        </label>
      </div>

      <p v-if="nicknameErrorMessage" class="message-error">
        {{ nicknameErrorMessage }}
      </p>

      <div class="settings-page__actions">
        <CommonBaseButton :disabled="isNicknameSubmitting || !nickname.trim()" @click="handleChangeNickname">
          {{ isNicknameSubmitting ? '저장 중...' : '닉네임 저장' }}
        </CommonBaseButton>
      </div>
    </section>

    <section class="settings-page__section content-panel">
      <div class="settings-page__section-header">
        <div>
          <h2>비밀번호 변경</h2>
          <p class="message-muted">현재 비밀번호를 확인한 뒤 새 비밀번호로 교체합니다.</p>
        </div>
      </div>

      <div class="settings-page__form-grid">
        <label class="settings-page__field">
          <span>현재 비밀번호</span>
          <input
            v-model="currentPassword"
            class="input-field"
            type="password"
            autocomplete="current-password"
            placeholder="현재 비밀번호"
          >
        </label>

        <label class="settings-page__field">
          <span>새 비밀번호</span>
          <input
            v-model="newPassword"
            class="input-field"
            type="password"
            autocomplete="new-password"
            placeholder="8자 이상 새 비밀번호"
          >
        </label>

        <label class="settings-page__field">
          <span>새 비밀번호 확인</span>
          <input
            v-model="newPasswordConfirm"
            class="input-field"
            type="password"
            autocomplete="new-password"
            placeholder="새 비밀번호를 다시 입력해 주세요"
          >
        </label>
      </div>

      <p v-if="passwordErrorMessage" class="message-error">
        {{ passwordErrorMessage }}
      </p>
      <p v-else-if="passwordConfirmMessage" class="message-error">
        {{ passwordConfirmMessage }}
      </p>

      <div class="settings-page__actions">
        <CommonBaseButton
          :disabled="isPasswordSubmitting || !currentPassword || !newPassword || !newPasswordConfirm"
          @click="handleChangePassword"
        >
          {{ isPasswordSubmitting ? '변경 중...' : '비밀번호 변경' }}
        </CommonBaseButton>
      </div>
    </section>

    <section class="settings-page__section content-panel">
      <div class="settings-page__section-header">
        <div>
          <h2>보안 설정</h2>
          <p class="message-muted">Google Authenticator 호환 OTP 2차 인증을 관리합니다.</p>
        </div>
      </div>

      <p v-if="securityErrorMessage" class="message-error">
        {{ securityErrorMessage }}
      </p>

      <p v-else-if="isSecurityLoading" class="message-muted">
        OTP 상태를 확인하고 있습니다.
      </p>

      <template v-else-if="!otpEnabled">
        <div class="settings-page__security-card">
          <div>
            <h3>OTP 미설정</h3>
            <p class="message-muted">설정을 시작하면 인증 앱에 등록할 수 있는 URL이 표시됩니다.</p>
          </div>

          <CommonBaseButton :disabled="isSetupSubmitting" @click="handleSetupOtp">
            {{ isSetupSubmitting ? '준비 중...' : 'OTP 설정 시작' }}
          </CommonBaseButton>
        </div>

        <div v-if="otpAuthUrl" class="settings-page__otp-grid">
          <div class="settings-page__otp-panel">
            <h3>등록 URL</h3>
            <p class="message-muted">인증 앱의 수동 등록 기능에 아래 값을 입력할 수 있습니다.</p>
            <code class="settings-page__otp-url">{{ otpAuthUrl }}</code>
          </div>

          <div class="settings-page__otp-panel">
            <h3>OTP 활성화</h3>
            <p class="message-muted">앱에 표시된 6자리 코드를 입력해 활성화를 완료합니다.</p>
            <AuthOtpCodeInput
              v-model="activationOtpCode"
              :disabled="isActivateSubmitting"
              @complete="handleActivateOtp"
            />
            <p v-if="activationErrorMessage" class="message-error">
              {{ activationErrorMessage }}
            </p>
            <CommonBaseButton
              block
              :disabled="activationOtpCode.length !== 6 || isActivateSubmitting"
              @click="handleActivateOtp"
            >
              {{ isActivateSubmitting ? '활성화 중...' : 'OTP 활성화' }}
            </CommonBaseButton>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="settings-page__security-card">
          <div>
            <h3>OTP 사용 중</h3>
            <p class="message-muted">로그인 시 6자리 OTP 코드가 추가로 필요합니다.</p>
          </div>

          <CommonBaseButton variant="secondary" @click="openDisableModal">
            OTP 해제
          </CommonBaseButton>
        </div>
      </template>
    </section>

    <CommonBaseModal
      :visible="isDisableModalVisible"
      title="OTP 해제"
      eyebrow="Settings"
      width="520px"
      @close="closeDisableModal"
    >
      <div class="settings-page__disable-modal">
        <p class="message-muted">
          현재 인증 앱에 표시된 OTP 코드를 입력하면 즉시 해제됩니다.
        </p>
        <AuthOtpCodeInput
          v-model="disableOtpCode"
          :disabled="isDisableSubmitting"
          @complete="handleDisableOtp"
        />
        <p v-if="disableErrorMessage" class="message-error">
          {{ disableErrorMessage }}
        </p>
      </div>

      <template #actions>
        <CommonBaseButton variant="secondary" :disabled="isDisableSubmitting" @click="closeDisableModal">
          취소
        </CommonBaseButton>
        <CommonBaseButton
          :disabled="disableOtpCode.length !== 6 || isDisableSubmitting"
          @click="handleDisableOtp"
        >
          {{ isDisableSubmitting ? '해제 중...' : 'OTP 해제' }}
        </CommonBaseButton>
      </template>
    </CommonBaseModal>
  </main>
</template>

<style scoped>
.settings-page {
  display: grid;
  gap: 24px;
}

.settings-page__hero,
.settings-page__section {
  padding: 28px;
}

.settings-page__hero {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 22px;
  align-items: stretch;
}

.settings-page__eyebrow {
  margin: 0 0 10px;
  color: var(--color-accent);
  font-weight: 700;
}

.settings-page__summary {
  display: grid;
  gap: 8px;
  padding: 22px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(95, 186, 255, 0.16), transparent 55%),
    rgba(255, 255, 255, 0.05);
}

.settings-page__summary span,
.settings-page__summary p {
  color: var(--color-text-muted);
}

.settings-page__summary strong {
  font-size: 1.5rem;
}

.settings-page__section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.settings-page__section-header h2,
.settings-page__security-card h3,
.settings-page__otp-panel h3 {
  margin: 0;
}

.settings-page__form-grid {
  display: grid;
  gap: 16px;
}

.settings-page__field {
  display: grid;
  gap: 8px;
}

.settings-page__field span {
  color: rgba(147, 210, 255, 0.82);
  font-weight: 700;
}

.settings-page__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.settings-page__security-card,
.settings-page__otp-panel,
.settings-page__disable-modal {
  display: grid;
  gap: 16px;
}

.settings-page__security-card,
.settings-page__otp-panel {
  padding: 22px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.05);
}

.settings-page__security-card {
  grid-template-columns: 1fr auto;
  align-items: center;
}

.settings-page__otp-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  margin-top: 18px;
}

.settings-page__otp-url {
  display: block;
  overflow-wrap: anywhere;
  padding: 14px;
  border-radius: 16px;
  background: rgba(7, 21, 39, 0.65);
  color: #bfe7ff;
  font-size: 0.88rem;
  line-height: 1.5;
}

@media (max-width: 900px) {
  .settings-page__hero,
  .settings-page__otp-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .settings-page__hero,
  .settings-page__section {
    padding: 22px;
  }

  .settings-page__section-header,
  .settings-page__security-card {
    flex-direction: column;
    align-items: stretch;
  }

  .settings-page__actions {
    justify-content: stretch;
  }

  .settings-page__actions :deep(button) {
    width: 100%;
  }
}
</style>
