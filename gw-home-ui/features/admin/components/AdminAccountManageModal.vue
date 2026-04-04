<script setup lang="ts">
import type { AccountRole, AccountStatus, AdminAccountDetail } from '~/types/admin'

const props = defineProps<{
  visible: boolean
  selectedAccount: AdminAccountDetail | null
  isManageLoading: boolean
  roleOptions: AccountRole[]
  statusOptions: AccountStatus[]
  openDropdownKey: string
  updatingRoleUuid: string
  updatingStatusUuid: string
  deletingUuid: string
  actionLoadingKey: string
  formatDateTime: (value: string) => string
  getSelectedActionLoadingKey: (action: string) => string
}>()

const emit = defineEmits<{
  close: []
  toggleDropdown: [key: string]
  selectManageRole: [role: AccountRole]
  selectManageStatus: [status: AccountStatus]
  unlock: [account: AdminAccountDetail]
  resetOtpFailure: [account: AdminAccountDetail]
  resetOtp: [account: AdminAccountDetail]
  resetPassword: [account: AdminAccountDetail]
  delete: [account: AdminAccountDetail]
}>()

const handleRoleSelect = (value: string) => {
  emit('selectManageRole', value as AccountRole)
}

const handleStatusSelect = (value: string) => {
  emit('selectManageStatus', value as AccountStatus)
}
</script>

<template>
  <CommonBaseModal
    :visible="props.visible"
    eyebrow="Admin"
    title="계정 관리"
    width="760px"
    @close="emit('close')"
  >
    <div v-if="props.isManageLoading" class="admin-account-manage-modal__loading">
      계정 정보를 불러오는 중입니다.
    </div>

    <div v-else-if="props.selectedAccount" class="admin-account-manage-modal">
      <div class="admin-account-manage-modal__summary">
        <div class="admin-account-manage-modal__card">
          <span>로그인 ID</span>
          <strong>{{ props.selectedAccount.loginId }}</strong>
        </div>
        <div class="admin-account-manage-modal__card">
          <span>이메일</span>
          <strong>{{ props.selectedAccount.email }}</strong>
        </div>
        <div class="admin-account-manage-modal__card">
          <span>잠금 상태</span>
          <strong>{{ props.selectedAccount.lckYn ? '잠김' : '정상' }}</strong>
        </div>
        <div class="admin-account-manage-modal__card">
          <span>가입일</span>
          <strong>{{ props.formatDateTime(props.selectedAccount.createdAt) }}</strong>
        </div>
      </div>

      <div class="admin-account-manage-modal__controls">
        <AdminAccountSelectField
          label="권한"
          :selected-label="props.selectedAccount.role"
          :options="props.roleOptions"
          :active-value="props.selectedAccount.role"
          :open="props.openDropdownKey === 'manage-role'"
          menu-direction="top"
          :disabled="props.updatingRoleUuid === props.selectedAccount.uuid"
          @toggle="emit('toggleDropdown', 'manage-role')"
          @select="handleRoleSelect"
        />
        <AdminAccountSelectField
          label="상태"
          :selected-label="props.selectedAccount.acctStat"
          :options="props.statusOptions"
          :active-value="props.selectedAccount.acctStat"
          :open="props.openDropdownKey === 'manage-status'"
          menu-direction="top"
          :disabled="props.updatingStatusUuid === props.selectedAccount.uuid"
          @toggle="emit('toggleDropdown', 'manage-status')"
          @select="handleStatusSelect"
        />
      </div>

      <div class="admin-account-manage-modal__actions">
        <CommonBaseButton
          variant="secondary"
          :disabled="props.actionLoadingKey === props.getSelectedActionLoadingKey('unlock')"
          @click="emit('unlock', props.selectedAccount)"
        >
          잠금 초기화
        </CommonBaseButton>
        <CommonBaseButton
          variant="secondary"
          :disabled="props.actionLoadingKey === props.getSelectedActionLoadingKey('otp-failure')"
          @click="emit('resetOtpFailure', props.selectedAccount)"
        >
          OTP 실패 초기화
        </CommonBaseButton>
        <CommonBaseButton
          variant="secondary"
          :disabled="props.actionLoadingKey === props.getSelectedActionLoadingKey('otp-reset')"
          @click="emit('resetOtp', props.selectedAccount)"
        >
          OTP 재등록 초기화
        </CommonBaseButton>
        <CommonBaseButton
          variant="secondary"
          :disabled="props.actionLoadingKey === props.getSelectedActionLoadingKey('password-reset')"
          @click="emit('resetPassword', props.selectedAccount)"
        >
          비밀번호 초기화
        </CommonBaseButton>
        <CommonBaseButton
          variant="danger"
          :disabled="props.deletingUuid === props.selectedAccount.uuid"
          @click="emit('delete', props.selectedAccount)"
        >
          계정 삭제
        </CommonBaseButton>
      </div>
    </div>

    <template #actions>
      <CommonBaseButton variant="secondary" @click="emit('close')">
        닫기
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.admin-account-manage-modal__loading {
  padding: 28px 0;
  color: var(--color-text-muted);
  text-align: center;
}

.admin-account-manage-modal {
  display: grid;
  gap: 18px;
}

.admin-account-manage-modal__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.admin-account-manage-modal__card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: 18px;
  background: rgba(8, 21, 38, 0.62);
}

.admin-account-manage-modal__card span {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.admin-account-manage-modal__card strong {
  overflow-wrap: anywhere;
  color: rgba(232, 244, 255, 0.96);
  font-size: 0.98rem;
}

.admin-account-manage-modal__controls {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.admin-account-manage-modal__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.admin-account-manage-modal__actions :deep(button) {
  width: 100%;
}

@media (max-width: 900px) {
  .admin-account-manage-modal__summary,
  .admin-account-manage-modal__controls,
  .admin-account-manage-modal__actions {
    grid-template-columns: 1fr;
  }
}
</style>
