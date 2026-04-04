<script setup lang="ts">
import type { AccountRole, AdminCreateAccountForm } from '~/types/admin'

const props = defineProps<{
  visible: boolean
  createForm: AdminCreateAccountForm
  isCreating: boolean
  roleOptions: AccountRole[]
  openDropdownKey: string
}>()

const emit = defineEmits<{
  close: []
  submit: []
  toggleDropdown: [key: string]
  selectRole: [role: AccountRole]
}>()

const handleRoleSelect = (value: string) => {
  emit('selectRole', value as AccountRole)
}
</script>

<template>
  <CommonBaseModal
    :visible="props.visible"
    eyebrow="Admin"
    title="계정 생성"
    width="560px"
    @close="emit('close')"
  >
    <form class="admin-account-create-modal__form" @submit.prevent="emit('submit')">
      <label>
        <span>로그인 ID</span>
        <input v-model="props.createForm.loginId" class="input-field" type="text" maxlength="30" required>
      </label>
      <label>
        <span>이메일</span>
        <input v-model="props.createForm.email" class="input-field" type="email" maxlength="255" required>
      </label>
      <label>
        <span>비밀번호</span>
        <input v-model="props.createForm.password" class="input-field" type="text" minlength="8" required>
      </label>
      <AdminAccountSelectField
        label="권한"
        :selected-label="props.createForm.role"
        :options="props.roleOptions"
        :active-value="props.createForm.role"
        :open="props.openDropdownKey === 'create-role'"
        menu-direction="top"
        @toggle="emit('toggleDropdown', 'create-role')"
        @select="handleRoleSelect"
      />
    </form>

    <template #actions>
      <CommonBaseButton variant="secondary" :disabled="props.isCreating" @click="emit('close')">
        취소
      </CommonBaseButton>
      <CommonBaseButton :disabled="props.isCreating" @click="emit('submit')">
        {{ props.isCreating ? '생성 중...' : '생성' }}
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.admin-account-create-modal__form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.admin-account-create-modal__form,
.admin-account-create-modal__form label {
  overflow: visible;
}

@media (max-width: 900px) {
  .admin-account-create-modal__form {
    grid-template-columns: 1fr;
  }
}
</style>
