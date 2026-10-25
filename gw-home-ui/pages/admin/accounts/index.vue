<script setup lang="ts">
import AdminAccountCreateModal from '~/features/admin/components/AdminAccountCreateModal.vue'
import AdminAccountFilterForm from '~/features/admin/components/AdminAccountFilterForm.vue'
import AdminAccountManageModal from '~/features/admin/components/AdminAccountManageModal.vue'
import AdminAccountTable from '~/features/admin/components/AdminAccountTable.vue'
import AdminTemporaryPasswordModal from '~/features/admin/components/AdminTemporaryPasswordModal.vue'
import { useAdminAccountManagement } from '~/features/admin/composables/use-admin-account-management'

definePageMeta({
  middleware: 'admin'
})

const {
  adminAccountRoleOptions: roleOptions,
  adminAccountStatusOptions: statusOptions,
  filters,
  createForm,
  accountPage,
  isLoading,
  isCreateModalVisible,
  isCreating,
  updatingRoleUuid,
  updatingStatusUuid,
  updatingOtpRequiredUuid,
  deletingUuid,
  openDropdownKey,
  actionLoadingKey,
  isTemporaryPasswordModalVisible,
  temporaryPassword,
  temporaryPasswordTargetLoginId,
  isManageModalVisible,
  isManageLoading,
  selectedAccount,
  formatDateTime,
  getRoleLabel,
  getStatusLabel,
  toggleDropdown,
  resetFilters,
  loadAccounts,
  openCreateModal,
  closeCreateModal,
  openManageModal,
  closeManageModal,
  handleCreateAccount,
  handleDeleteAccount,
  handleUnlockAccount,
  handleResetOtpFailure,
  handleResetOtp,
  handleResetPassword,
  handleToggleOtpRequired,
  copyTemporaryPassword,
  closeTemporaryPasswordModal,
  handleSearch,
  movePage,
  getSelectedActionLoadingKey,
  selectFilterRole,
  selectFilterStatus,
  selectCreateRole,
  selectManageRole,
  selectManageStatus
} = useAdminAccountManagement()

await loadAccounts()
</script>

<template>
  <main class="page-container page-stack admin-accounts-page">
    <section class="content-panel page-panel-padding-md admin-accounts-page__panel">
      <CommonBasePageHeader title="계정 관리" eyebrow="Admin">
        <template #actions>
          <CommonBaseButton @click="openCreateModal">계정 생성</CommonBaseButton>
        </template>
      </CommonBasePageHeader>

      <AdminAccountFilterForm
        :filters="filters"
        :role-options="roleOptions"
        :status-options="statusOptions"
        :open-dropdown-key="openDropdownKey"
        :get-role-label="getRoleLabel"
        :get-status-label="getStatusLabel"
        @submit="handleSearch"
        @reset="resetFilters(); void handleSearch()"
        @toggle-dropdown="toggleDropdown"
        @select-filter-role="selectFilterRole"
        @select-filter-status="selectFilterStatus"
      />
    </section>

    <section class="content-panel page-panel-padding-md admin-accounts-page__panel">
      <AdminAccountTable
        :account-page="accountPage"
        :is-loading="isLoading"
        :format-date-time="formatDateTime"
        :current-page="filters.page"
        @manage="openManageModal"
        @previous="movePage(filters.page - 1)"
        @next="movePage(filters.page + 1)"
      />
    </section>

    <AdminAccountManageModal
      :visible="isManageModalVisible"
      :selected-account="selectedAccount"
      :is-manage-loading="isManageLoading"
      :role-options="roleOptions"
      :status-options="statusOptions"
      :open-dropdown-key="openDropdownKey"
      :updating-role-uuid="updatingRoleUuid"
      :updating-status-uuid="updatingStatusUuid"
      :updating-otp-required-uuid="updatingOtpRequiredUuid"
      :deleting-uuid="deletingUuid"
      :action-loading-key="actionLoadingKey"
      :format-date-time="formatDateTime"
      :get-selected-action-loading-key="getSelectedActionLoadingKey"
      @close="closeManageModal"
      @toggle-dropdown="toggleDropdown"
      @select-manage-role="selectManageRole"
      @select-manage-status="selectManageStatus"
      @toggle-otp-required="handleToggleOtpRequired"
      @unlock="handleUnlockAccount"
      @reset-otp-failure="handleResetOtpFailure"
      @reset-otp="handleResetOtp"
      @reset-password="handleResetPassword"
      @delete="handleDeleteAccount"
    />

    <AdminAccountCreateModal
      :visible="isCreateModalVisible"
      :create-form="createForm"
      :is-creating="isCreating"
      :role-options="roleOptions"
      :open-dropdown-key="openDropdownKey"
      @close="closeCreateModal"
      @submit="handleCreateAccount"
      @toggle-dropdown="toggleDropdown"
      @select-role="selectCreateRole"
    />

    <AdminTemporaryPasswordModal
      :visible="isTemporaryPasswordModalVisible"
      :temporary-password="temporaryPassword"
      :temporary-password-target-login-id="temporaryPasswordTargetLoginId"
      @close="closeTemporaryPasswordModal"
      @copy="copyTemporaryPassword"
    />
  </main>
</template>

<style scoped>
.admin-accounts-page__panel {
  position: relative;
  overflow: visible;
}

.admin-accounts-page__panel:first-child {
  z-index: 3;
}

.admin-accounts-page__panel:last-child {
  z-index: 1;
}

@media (max-width: 900px) {
  .admin-accounts-page__panel {
    overflow: visible;
  }
}

@media (max-width: 768px) {
  .admin-accounts-page {
    gap: var(--section-gap);
  }

}

:deep(.base-modal__panel) {
  overflow: visible;
}

:deep(.base-modal__body) {
  overflow: visible;
}
</style>
