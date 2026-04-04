<script setup lang="ts">
import type { AccountRole, AccountStatus } from '~/types/admin'
import type { AdminAccountFilterState } from '~/features/admin/types/admin-account-management'

const props = defineProps<{
  filters: AdminAccountFilterState
  roleOptions: AccountRole[]
  statusOptions: AccountStatus[]
  openDropdownKey: string
  getRoleLabel: (role: AccountRole | '') => string
  getStatusLabel: (status: AccountStatus | '') => string
}>()

const emit = defineEmits<{
  submit: []
  reset: []
  toggleDropdown: [key: string]
  selectFilterRole: [role: AccountRole | '']
  selectFilterStatus: [status: AccountStatus | '']
}>()

const handleRoleSelect = (value: string) => {
  emit('selectFilterRole', value as AccountRole | '')
}

const handleStatusSelect = (value: string) => {
  emit('selectFilterStatus', value as AccountStatus | '')
}
</script>

<template>
  <form class="admin-accounts-filter-form" @submit.prevent="emit('submit')">
    <label>
      <span>로그인 ID</span>
      <input v-model="props.filters.loginId" class="input-field" type="text" placeholder="loginId 검색">
    </label>
    <AdminAccountSelectField
      label="권한"
      :selected-label="props.getRoleLabel(props.filters.role)"
      :options="props.roleOptions"
      :active-value="props.filters.role"
      :open="props.openDropdownKey === 'filter-role'"
      all-label="전체"
      @toggle="emit('toggleDropdown', 'filter-role')"
      @select="handleRoleSelect"
    />
    <AdminAccountSelectField
      label="상태"
      :selected-label="props.getStatusLabel(props.filters.status)"
      :options="props.statusOptions"
      :active-value="props.filters.status"
      :open="props.openDropdownKey === 'filter-status'"
      all-label="전체"
      @toggle="emit('toggleDropdown', 'filter-status')"
      @select="handleStatusSelect"
    />
    <div class="admin-accounts-filter-form__actions">
      <CommonBaseButton variant="secondary" type="button" @click="emit('reset')">
        초기화
      </CommonBaseButton>
      <CommonBaseButton type="submit">
        검색
      </CommonBaseButton>
    </div>
  </form>
</template>

<style scoped>
.admin-accounts-filter-form {
  display: grid;
  grid-template-columns: minmax(220px, 1.6fr) minmax(140px, 0.8fr) minmax(140px, 0.8fr) auto;
  align-items: end;
  gap: 16px;
  margin-top: 18px;
}

.admin-accounts-filter-form__actions {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  gap: 10px;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .admin-accounts-filter-form {
    grid-template-columns: 1fr;
  }

  .admin-accounts-filter-form__actions {
    justify-content: stretch;
  }
}
</style>
