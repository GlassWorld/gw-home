<script setup lang="ts">
import SettingsOtpSection from '~/features/settings/components/SettingsOtpSection.vue'
import SettingsPasswordSection from '~/features/settings/components/SettingsPasswordSection.vue'
import SettingsProfileSection from '~/features/settings/components/SettingsProfileSection.vue'
import OtpDisableModal from '~/features/security/components/OtpDisableModal.vue'
import { useOtpDisableFlow } from '~/features/security/composables/use-otp-disable-flow'
import { useOtpSetupFlow } from '~/features/security/composables/use-otp-setup-flow'

definePageMeta({
  middleware: 'auth'
})

const authStore = useAuthStore()
const { showToast } = useToast()
const { changeNickname, changePassword } = useSettingsApi()
const { fetchOtpStatus } = useOtpApi()

const nickname = ref(authStore.currentUser?.nickname ?? '')
const currentPassword = ref('')
const newPassword = ref('')
const newPasswordConfirm = ref('')
const isNicknameSubmitting = ref(false)
const isPasswordSubmitting = ref(false)
const nicknameErrorMessage = ref('')
const passwordErrorMessage = ref('')

const isSecurityLoading = ref(true)
const otpEnabled = ref(false)
const securityErrorMessage = ref('')
const {
  isSetupSubmitting,
  isActivateSubmitting,
  otpAuthUrl,
  qrCodeDataUrl,
  activationOtpCode,
  activationErrorMessage,
  handleSetupOtp,
  handleActivateOtp
} = useOtpSetupFlow()
const {
  isDisableSubmitting,
  isDisableModalVisible,
  disableOtpCode,
  disableErrorMessage,
  openDisableModal,
  closeDisableModal,
  handleDisableOtp
} = useOtpDisableFlow()

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

async function startOtpSetup() {
  await handleSetupOtp({
    setupSuccessMessage: 'OTP 설정을 시작했습니다. 인증 앱에 등록해 주세요.'
  })
}

async function submitOtpActivation() {
  const activated = await handleActivateOtp({
    activateSuccessMessage: 'OTP가 활성화되었습니다.',
    onActivated: () => {
      otpEnabled.value = true
    }
  })

  if (activated) {
    securityErrorMessage.value = ''
  }
}

async function submitOtpDisable() {
  const disabled = await handleDisableOtp({
    disableSuccessMessage: 'OTP가 해제되었습니다.',
    onDisabled: () => {
      otpEnabled.value = false
    }
  })

  if (disabled) {
    securityErrorMessage.value = ''
  }
}

await loadOtpStatus()
</script>

<template>
  <main class="page-container settings-page">
    <section class="settings-page__hero content-panel page-panel-padding-lg page-hero-split">
      <div>
        <p class="settings-page__eyebrow">Settings</p>
        <h1 class="section-title">계정 설정</h1>
        <p class="section-description">
          닉네임, 비밀번호, OTP 보안 설정을 한 곳에서 관리할 수 있습니다.
        </p>
      </div>

      <div class="settings-page__summary page-info-card">
        <span>현재 계정</span>
        <strong>{{ authStore.currentUser?.nickname }}</strong>
        <p>{{ authStore.currentUser?.loginId }} · {{ authStore.currentUser?.email }}</p>
      </div>
    </section>

    <SettingsProfileSection
      :current-nickname="authStore.currentUser?.nickname ?? ''"
      :nickname="nickname"
      :nickname-error-message="nicknameErrorMessage"
      :is-nickname-submitting="isNicknameSubmitting"
      @update-nickname="nickname = $event"
      @submit="handleChangeNickname"
    />

    <SettingsPasswordSection
      :current-password="currentPassword"
      :new-password="newPassword"
      :new-password-confirm="newPasswordConfirm"
      :password-error-message="passwordErrorMessage"
      :password-confirm-message="passwordConfirmMessage"
      :is-password-submitting="isPasswordSubmitting"
      @update-current-password="currentPassword = $event"
      @update-new-password="newPassword = $event"
      @update-new-password-confirm="newPasswordConfirm = $event"
      @submit="handleChangePassword"
    />

    <SettingsOtpSection
      :security-error-message="securityErrorMessage"
      :is-security-loading="isSecurityLoading"
      :otp-enabled="otpEnabled"
      :is-setup-submitting="isSetupSubmitting"
      :is-activate-submitting="isActivateSubmitting"
      :otp-auth-url="otpAuthUrl"
      :qr-code-data-url="qrCodeDataUrl"
      :activation-otp-code="activationOtpCode"
      :activation-error-message="activationErrorMessage"
      @start-setup="startOtpSetup"
      @activate="submitOtpActivation"
      @open-disable-modal="openDisableModal"
      @update-activation-otp-code="activationOtpCode = $event"
    />

    <OtpDisableModal
      :visible="isDisableModalVisible"
      eyebrow="Settings"
      :disable-otp-code="disableOtpCode"
      :disable-error-message="disableErrorMessage"
      :is-disable-submitting="isDisableSubmitting"
      @update:disable-otp-code="disableOtpCode = $event"
      @close="closeDisableModal"
      @submit="submitOtpDisable"
    />
  </main>
</template>

<style scoped>
.settings-page {
  display: grid;
  gap: 24px;
}

.settings-page__eyebrow {
  margin: 0 0 10px;
  color: var(--color-accent);
  font-weight: 700;
}

</style>
