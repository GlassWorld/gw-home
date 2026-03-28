<script setup lang="ts">
import type { AccountRole, AccountStatus, AdminAccount, AdminAccountDetail, AdminCreateAccountForm } from '~/types/admin'

definePageMeta({
  middleware: 'admin'
})

const { fetchAccounts, fetchAccount, createAccount, updateRole, updateStatus, deleteAccount, unlockAccount, resetOtpFailure, resetOtp, resetPassword } = useAdminAccountApi()
const { showToast } = useToast()
const { confirm } = useDialog()

const roleOptions: AccountRole[] = ['ADMIN', 'USER', 'GUEST']
const statusOptions: AccountStatus[] = ['ACTIVE', 'INACTIVE']

const filters = reactive({
  loginId: '',
  role: '' as AccountRole | '',
  status: '' as AccountStatus | '',
  page: 1,
  size: 20
})

const createForm = reactive<AdminCreateAccountForm>({
  loginId: '',
  email: '',
  password: '',
  role: 'USER'
})

const accountPage = ref({
  content: [] as AdminAccount[],
  page: 1,
  size: 20,
  totalCount: 0,
  totalPages: 0
})
const isLoading = ref(false)
const isCreateModalVisible = ref(false)
const isCreating = ref(false)
const updatingRoleUuid = ref('')
const updatingStatusUuid = ref('')
const deletingUuid = ref('')
const openDropdownKey = ref('')
const actionLoadingKey = ref('')
const isTemporaryPasswordModalVisible = ref(false)
const temporaryPassword = ref('')
const temporaryPasswordTargetLoginId = ref('')
const isManageModalVisible = ref(false)
const isManageLoading = ref(false)
const selectedAccount = ref<AdminAccountDetail | null>(null)

function formatDateTime(value: string): string {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function getRoleLabel(role: AccountRole | ''): string {
  return role || '전체'
}

function getStatusLabel(status: AccountStatus | ''): string {
  return status || '전체'
}

function toggleDropdown(key: string) {
  openDropdownKey.value = openDropdownKey.value === key ? '' : key
}

function closeDropdown() {
  openDropdownKey.value = ''
}

function getActionLoadingKey(action: string, uuid: string): string {
  return `${action}:${uuid}`
}

function getSelectedActionLoadingKey(action: string): string {
  if (!selectedAccount.value) {
    return ''
  }

  return getActionLoadingKey(action, selectedAccount.value.uuid)
}

function handleDocumentMouseDown(event: MouseEvent) {
  const target = event.target as HTMLElement | null

  if (!target?.closest('[data-dropdown-root]')) {
    closeDropdown()
  }
}

async function loadAccounts() {
  isLoading.value = true

  try {
    accountPage.value = await fetchAccounts({
      loginId: filters.loginId.trim() || undefined,
      role: filters.role,
      status: filters.status,
      page: filters.page,
      size: filters.size
    })
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    accountPage.value.content = []
    showToast(fetchError.data?.message ?? '계정 목록을 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isLoading.value = false
  }
}

function openCreateModal() {
  createForm.loginId = ''
  createForm.email = ''
  createForm.password = ''
  createForm.role = 'USER'
  isCreateModalVisible.value = true
}

function closeCreateModal() {
  isCreateModalVisible.value = false
}

async function openManageModal(account: AdminAccount) {
  isManageModalVisible.value = true
  isManageLoading.value = true
  selectedAccount.value = null
  closeDropdown()

  try {
    selectedAccount.value = await fetchAccount(account.uuid)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '계정 정보를 불러오지 못했습니다.', { variant: 'error' })
    isManageModalVisible.value = false
  } finally {
    isManageLoading.value = false
  }
}

function closeManageModal() {
  if (actionLoadingKey.value) {
    return
  }

  isManageModalVisible.value = false
  isManageLoading.value = false
  selectedAccount.value = null
  closeDropdown()
}

async function handleCreateAccount() {
  if (isCreating.value) {
    return
  }

  isCreating.value = true

  try {
    await createAccount({
      loginId: createForm.loginId.trim(),
      email: createForm.email.trim(),
      password: createForm.password,
      role: createForm.role
    })
    showToast('계정을 생성했습니다.', { variant: 'success' })
    closeCreateModal()
    filters.page = 1
    await loadAccounts()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '계정 생성에 실패했습니다.', { variant: 'error' })
  } finally {
    isCreating.value = false
  }
}

async function handleChangeRole(account: AdminAccount, role: AccountRole) {
  if (updatingRoleUuid.value) {
    return
  }

  updatingRoleUuid.value = account.uuid

  try {
    await updateRole(account.uuid, { role })
    showToast('권한을 변경했습니다.', { variant: 'success' })
    await loadAccounts()
    if (selectedAccount.value?.uuid === account.uuid) {
      selectedAccount.value = await fetchAccount(account.uuid)
    }
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '권한 변경에 실패했습니다.', { variant: 'error' })
  } finally {
    updatingRoleUuid.value = ''
  }
}

async function handleChangeStatus(account: AdminAccount, status: AccountStatus) {
  if (updatingStatusUuid.value) {
    return
  }

  updatingStatusUuid.value = account.uuid

  try {
    await updateStatus(account.uuid, { status })
    showToast('상태를 변경했습니다.', { variant: 'success' })
    await loadAccounts()
    if (selectedAccount.value?.uuid === account.uuid) {
      selectedAccount.value = await fetchAccount(account.uuid)
    }
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '상태 변경에 실패했습니다.', { variant: 'error' })
  } finally {
    updatingStatusUuid.value = ''
  }
}

async function handleDeleteAccount(account: AdminAccount) {
  if (deletingUuid.value) {
    return
  }

  const shouldDelete = await confirm(`${account.loginId} 계정을 삭제할까요?`, {
    title: '계정 삭제',
    confirmText: '삭제',
    cancelText: '취소'
  })

  if (!shouldDelete) {
    return
  }

  deletingUuid.value = account.uuid

  try {
    await deleteAccount(account.uuid)
    showToast('계정을 삭제했습니다.', { variant: 'success' })
    if (selectedAccount.value?.uuid === account.uuid) {
      closeManageModal()
    }
    if (accountPage.value.content.length === 1 && filters.page > 1) {
      filters.page -= 1
    }
    await loadAccounts()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '계정 삭제에 실패했습니다.', { variant: 'error' })
  } finally {
    deletingUuid.value = ''
  }
}

async function handleUnlockAccount(account: AdminAccount) {
  if (actionLoadingKey.value) {
    return
  }

  actionLoadingKey.value = getActionLoadingKey('unlock', account.uuid)

  try {
    await unlockAccount(account.uuid)
    showToast('로그인 잠금을 초기화했습니다.', { variant: 'success' })
    await loadAccounts()
    if (selectedAccount.value?.uuid === account.uuid) {
      selectedAccount.value = await fetchAccount(account.uuid)
    }
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '로그인 잠금 초기화에 실패했습니다.', { variant: 'error' })
  } finally {
    actionLoadingKey.value = ''
  }
}

async function handleResetOtpFailure(account: AdminAccount) {
  if (actionLoadingKey.value) {
    return
  }

  actionLoadingKey.value = getActionLoadingKey('otp-failure', account.uuid)

  try {
    await resetOtpFailure(account.uuid)
    showToast('OTP 실패 횟수를 초기화했습니다.', { variant: 'success' })
    await loadAccounts()
    if (selectedAccount.value?.uuid === account.uuid) {
      selectedAccount.value = await fetchAccount(account.uuid)
    }
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? 'OTP 실패 초기화에 실패했습니다.', { variant: 'error' })
  } finally {
    actionLoadingKey.value = ''
  }
}

async function handleResetOtp(account: AdminAccount) {
  if (actionLoadingKey.value) {
    return
  }

  const shouldReset = await confirm(`${account.loginId} 계정의 OTP를 재등록 상태로 초기화할까요?`, {
    title: 'OTP 재등록 초기화',
    confirmText: '초기화',
    cancelText: '취소'
  })

  if (!shouldReset) {
    return
  }

  actionLoadingKey.value = getActionLoadingKey('otp-reset', account.uuid)

  try {
    await resetOtp(account.uuid)
    showToast('OTP 재등록 초기화를 완료했습니다.', { variant: 'success' })
    await loadAccounts()
    if (selectedAccount.value?.uuid === account.uuid) {
      selectedAccount.value = await fetchAccount(account.uuid)
    }
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? 'OTP 재등록 초기화에 실패했습니다.', { variant: 'error' })
  } finally {
    actionLoadingKey.value = ''
  }
}

async function handleResetPassword(account: AdminAccount) {
  if (actionLoadingKey.value) {
    return
  }

  const shouldReset = await confirm(`${account.loginId} 계정의 비밀번호를 임시 비밀번호로 초기화할까요?`, {
    title: '비밀번호 초기화',
    confirmText: '초기화',
    cancelText: '취소'
  })

  if (!shouldReset) {
    return
  }

  actionLoadingKey.value = getActionLoadingKey('password-reset', account.uuid)

  try {
    const response = await resetPassword(account.uuid)
    temporaryPassword.value = response.temporaryPassword
    temporaryPasswordTargetLoginId.value = account.loginId
    isTemporaryPasswordModalVisible.value = true
    showToast('임시 비밀번호를 발급했습니다.', { variant: 'success' })
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '비밀번호 초기화에 실패했습니다.', { variant: 'error' })
  } finally {
    actionLoadingKey.value = ''
  }
}

async function copyTemporaryPassword() {
  try {
    await navigator.clipboard.writeText(temporaryPassword.value)
    showToast('임시 비밀번호를 복사했습니다.', { variant: 'success' })
  } catch {
    showToast('클립보드 복사에 실패했습니다.', { variant: 'error' })
  }
}

function closeTemporaryPasswordModal() {
  isTemporaryPasswordModalVisible.value = false
  temporaryPassword.value = ''
  temporaryPasswordTargetLoginId.value = ''
}

async function handleSearch() {
  closeDropdown()
  filters.page = 1
  await loadAccounts()
}

async function movePage(nextPage: number) {
  if (nextPage < 1 || nextPage > Math.max(accountPage.value.totalPages, 1) || nextPage === filters.page) {
    return
  }

  filters.page = nextPage
  await loadAccounts()
}

function selectFilterRole(role: AccountRole | '') {
  filters.role = role
  closeDropdown()
}

function selectFilterStatus(status: AccountStatus | '') {
  filters.status = status
  closeDropdown()
}

function selectCreateRole(role: AccountRole) {
  createForm.role = role
  closeDropdown()
}

function selectManageRole(role: AccountRole) {
  if (!selectedAccount.value) {
    return
  }

  closeDropdown()
  void handleChangeRole(selectedAccount.value, role)
}

function selectManageStatus(status: AccountStatus) {
  if (!selectedAccount.value) {
    return
  }

  closeDropdown()
  void handleChangeStatus(selectedAccount.value, status)
}

onMounted(() => {
  document.addEventListener('mousedown', handleDocumentMouseDown)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentMouseDown)
})

await loadAccounts()
</script>

<template>
  <main class="page-container admin-accounts-page">
    <section class="content-panel admin-accounts-page__panel">
      <div class="admin-accounts-page__header">
        <div>
          <p class="admin-accounts-page__eyebrow">Admin</p>
          <h1 class="section-title">계정 관리</h1>
        </div>
        <CommonBaseButton @click="openCreateModal">
          계정 생성
        </CommonBaseButton>
      </div>

      <form class="admin-accounts-page__filters" @submit.prevent="handleSearch">
        <label>
          <span>로그인 ID</span>
          <input v-model="filters.loginId" class="input-field" type="text" placeholder="loginId 검색">
        </label>
        <label>
          <span>권한</span>
          <div class="admin-accounts-page__dropdown" data-dropdown-root>
            <button
              type="button"
              class="admin-accounts-page__dropdown-trigger"
              :class="{ 'admin-accounts-page__dropdown-trigger--open': openDropdownKey === 'filter-role' }"
              @click="toggleDropdown('filter-role')"
            >
              <span>{{ getRoleLabel(filters.role) }}</span>
            </button>
            <div v-if="openDropdownKey === 'filter-role'" class="admin-accounts-page__dropdown-menu">
              <button type="button" class="admin-accounts-page__dropdown-option" @click="selectFilterRole('')">
                전체
              </button>
              <button
                v-for="role in roleOptions"
                :key="role"
                type="button"
                class="admin-accounts-page__dropdown-option"
                :class="{ 'admin-accounts-page__dropdown-option--active': filters.role === role }"
                @click="selectFilterRole(role)"
              >
                {{ role }}
              </button>
            </div>
          </div>
        </label>
        <label>
          <span>상태</span>
          <div class="admin-accounts-page__dropdown" data-dropdown-root>
            <button
              type="button"
              class="admin-accounts-page__dropdown-trigger"
              :class="{ 'admin-accounts-page__dropdown-trigger--open': openDropdownKey === 'filter-status' }"
              @click="toggleDropdown('filter-status')"
            >
              <span>{{ getStatusLabel(filters.status) }}</span>
            </button>
            <div v-if="openDropdownKey === 'filter-status'" class="admin-accounts-page__dropdown-menu">
              <button type="button" class="admin-accounts-page__dropdown-option" @click="selectFilterStatus('')">
                전체
              </button>
              <button
                v-for="status in statusOptions"
                :key="status"
                type="button"
                class="admin-accounts-page__dropdown-option"
                :class="{ 'admin-accounts-page__dropdown-option--active': filters.status === status }"
                @click="selectFilterStatus(status)"
              >
                {{ status }}
              </button>
            </div>
          </div>
        </label>
        <div class="admin-accounts-page__filter-actions">
          <CommonBaseButton variant="secondary" type="button" @click="filters.loginId = ''; filters.role = ''; filters.status = ''; void handleSearch()">
            초기화
          </CommonBaseButton>
          <CommonBaseButton type="submit">
            검색
          </CommonBaseButton>
        </div>
      </form>
    </section>

    <section class="content-panel admin-accounts-page__panel">
      <div class="admin-accounts-page__summary">
        <strong>총 {{ accountPage.totalCount }}건</strong>
        <span v-if="isLoading">불러오는 중...</span>
      </div>

      <div class="admin-accounts-page__table-wrap">
        <table class="admin-accounts-page__table">
          <thead>
            <tr>
              <th>로그인 ID</th>
              <th>이메일</th>
              <th>권한</th>
              <th>상태</th>
              <th>잠금</th>
              <th>가입일</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="account in accountPage.content" :key="account.uuid">
              <td>{{ account.loginId }}</td>
              <td>{{ account.email }}</td>
              <td><span class="admin-accounts-page__state-badge">{{ account.role }}</span></td>
              <td><span class="admin-accounts-page__state-badge admin-accounts-page__state-badge--status">{{ account.acctStat }}</span></td>
              <td>
                <span :class="['admin-accounts-page__lock-badge', { 'admin-accounts-page__lock-badge--locked': account.lckYn }]">
                  {{ account.lckYn ? '잠김' : '정상' }}
                </span>
              </td>
              <td>{{ formatDateTime(account.createdAt) }}</td>
              <td>
                <CommonBaseButton variant="secondary" size="small" @click="openManageModal(account)">
                  관리
                </CommonBaseButton>
              </td>
            </tr>
            <tr v-if="!isLoading && accountPage.content.length === 0">
              <td colspan="7" class="admin-accounts-page__empty">
                조회된 계정이 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="admin-accounts-page__pagination">
        <CommonBaseButton variant="secondary" :disabled="filters.page <= 1" @click="movePage(filters.page - 1)">
          이전
        </CommonBaseButton>
        <span>{{ accountPage.page }} / {{ Math.max(accountPage.totalPages, 1) }}</span>
        <CommonBaseButton
          variant="secondary"
          :disabled="filters.page >= Math.max(accountPage.totalPages, 1)"
          @click="movePage(filters.page + 1)"
        >
          다음
        </CommonBaseButton>
      </div>
    </section>

    <CommonBaseModal
      :visible="isManageModalVisible"
      eyebrow="Admin"
      title="계정 관리"
      width="760px"
      @close="closeManageModal"
    >
      <div v-if="isManageLoading" class="admin-accounts-page__manage-loading">
        계정 정보를 불러오는 중입니다.
      </div>

      <div v-else-if="selectedAccount" class="admin-accounts-page__manage">
        <div class="admin-accounts-page__manage-summary">
          <div class="admin-accounts-page__manage-card">
            <span>로그인 ID</span>
            <strong>{{ selectedAccount.loginId }}</strong>
          </div>
          <div class="admin-accounts-page__manage-card">
            <span>이메일</span>
            <strong>{{ selectedAccount.email }}</strong>
          </div>
          <div class="admin-accounts-page__manage-card">
            <span>잠금 상태</span>
            <strong>{{ selectedAccount.lckYn ? '잠김' : '정상' }}</strong>
          </div>
          <div class="admin-accounts-page__manage-card">
            <span>가입일</span>
            <strong>{{ formatDateTime(selectedAccount.createdAt) }}</strong>
          </div>
        </div>

        <div class="admin-accounts-page__manage-controls">
          <label>
            <span>권한</span>
            <div class="admin-accounts-page__dropdown" data-dropdown-root>
              <button
                type="button"
                class="admin-accounts-page__dropdown-trigger"
                :class="{ 'admin-accounts-page__dropdown-trigger--open': openDropdownKey === 'manage-role' }"
                :disabled="updatingRoleUuid === selectedAccount.uuid"
                @click="toggleDropdown('manage-role')"
              >
                <span>{{ selectedAccount.role }}</span>
              </button>
              <div v-if="openDropdownKey === 'manage-role'" class="admin-accounts-page__dropdown-menu admin-accounts-page__dropdown-menu--modal">
                <button
                  v-for="role in roleOptions"
                  :key="role"
                  type="button"
                  class="admin-accounts-page__dropdown-option"
                  :class="{ 'admin-accounts-page__dropdown-option--active': selectedAccount.role === role }"
                  @click="selectManageRole(role)"
                >
                  {{ role }}
                </button>
              </div>
            </div>
          </label>

          <label>
            <span>상태</span>
            <div class="admin-accounts-page__dropdown" data-dropdown-root>
              <button
                type="button"
                class="admin-accounts-page__dropdown-trigger"
                :class="{ 'admin-accounts-page__dropdown-trigger--open': openDropdownKey === 'manage-status' }"
                :disabled="updatingStatusUuid === selectedAccount.uuid"
                @click="toggleDropdown('manage-status')"
              >
                <span>{{ selectedAccount.acctStat }}</span>
              </button>
              <div v-if="openDropdownKey === 'manage-status'" class="admin-accounts-page__dropdown-menu admin-accounts-page__dropdown-menu--modal">
                <button
                  v-for="status in statusOptions"
                  :key="status"
                  type="button"
                  class="admin-accounts-page__dropdown-option"
                  :class="{ 'admin-accounts-page__dropdown-option--active': selectedAccount.acctStat === status }"
                  @click="selectManageStatus(status)"
                >
                  {{ status }}
                </button>
              </div>
            </div>
          </label>
        </div>

        <div class="admin-accounts-page__manage-actions">
          <CommonBaseButton
            variant="secondary"
            :disabled="actionLoadingKey === getSelectedActionLoadingKey('unlock')"
            @click="handleUnlockAccount(selectedAccount)"
          >
            잠금 초기화
          </CommonBaseButton>
          <CommonBaseButton
            variant="secondary"
            :disabled="actionLoadingKey === getSelectedActionLoadingKey('otp-failure')"
            @click="handleResetOtpFailure(selectedAccount)"
          >
            OTP 실패 초기화
          </CommonBaseButton>
          <CommonBaseButton
            variant="secondary"
            :disabled="actionLoadingKey === getSelectedActionLoadingKey('otp-reset')"
            @click="handleResetOtp(selectedAccount)"
          >
            OTP 재등록 초기화
          </CommonBaseButton>
          <CommonBaseButton
            variant="secondary"
            :disabled="actionLoadingKey === getSelectedActionLoadingKey('password-reset')"
            @click="handleResetPassword(selectedAccount)"
          >
            비밀번호 초기화
          </CommonBaseButton>
          <CommonBaseButton
            variant="danger"
            :disabled="deletingUuid === selectedAccount.uuid"
            @click="handleDeleteAccount(selectedAccount)"
          >
            계정 삭제
          </CommonBaseButton>
        </div>
      </div>

      <template #actions>
        <CommonBaseButton variant="secondary" @click="closeManageModal">
          닫기
        </CommonBaseButton>
      </template>
    </CommonBaseModal>

    <CommonBaseModal
      :visible="isCreateModalVisible"
      eyebrow="Admin"
      title="계정 생성"
      width="560px"
      @close="closeCreateModal"
    >
      <form class="admin-accounts-page__create-form" @submit.prevent="handleCreateAccount">
        <label>
          <span>로그인 ID</span>
          <input v-model="createForm.loginId" class="input-field" type="text" maxlength="30" required>
        </label>
        <label>
          <span>이메일</span>
          <input v-model="createForm.email" class="input-field" type="email" maxlength="255" required>
        </label>
        <label>
          <span>비밀번호</span>
          <input v-model="createForm.password" class="input-field" type="text" minlength="8" required>
        </label>
        <label>
          <span>권한</span>
          <div class="admin-accounts-page__dropdown admin-accounts-page__dropdown--modal" data-dropdown-root>
            <button
              type="button"
              class="admin-accounts-page__dropdown-trigger"
              :class="{ 'admin-accounts-page__dropdown-trigger--open': openDropdownKey === 'create-role' }"
              @click="toggleDropdown('create-role')"
            >
              <span>{{ createForm.role }}</span>
            </button>
            <div
              v-if="openDropdownKey === 'create-role'"
              class="admin-accounts-page__dropdown-menu admin-accounts-page__dropdown-menu--modal"
            >
              <button
                v-for="role in roleOptions"
                :key="role"
                type="button"
                class="admin-accounts-page__dropdown-option"
                :class="{ 'admin-accounts-page__dropdown-option--active': createForm.role === role }"
                @click="selectCreateRole(role)"
              >
                {{ role }}
              </button>
            </div>
          </div>
        </label>
      </form>

      <template #actions>
        <CommonBaseButton variant="secondary" :disabled="isCreating" @click="closeCreateModal">
          취소
        </CommonBaseButton>
        <CommonBaseButton :disabled="isCreating" @click="handleCreateAccount">
          {{ isCreating ? '생성 중...' : '생성' }}
        </CommonBaseButton>
      </template>
    </CommonBaseModal>

    <CommonBaseModal
      :visible="isTemporaryPasswordModalVisible"
      eyebrow="Admin"
      title="임시 비밀번호 발급"
      width="560px"
      @close="closeTemporaryPasswordModal"
    >
      <div class="admin-accounts-page__temporary-password">
        <p class="message-muted">
          아래 임시 비밀번호를 <strong>{{ temporaryPasswordTargetLoginId }}</strong> 계정 사용자에게 직접 전달해 주세요.
        </p>
        <code class="admin-accounts-page__temporary-password-code">{{ temporaryPassword }}</code>
      </div>

      <template #actions>
        <CommonBaseButton variant="secondary" @click="copyTemporaryPassword">
          복사
        </CommonBaseButton>
        <CommonBaseButton @click="closeTemporaryPasswordModal">
          확인
        </CommonBaseButton>
      </template>
    </CommonBaseModal>
  </main>
</template>

<style scoped>
.admin-accounts-page {
  display: grid;
  gap: 24px;
}

.admin-accounts-page__panel {
  position: relative;
  overflow: visible;
  padding: 22px;
}

.admin-accounts-page__panel:first-child {
  z-index: 3;
}

.admin-accounts-page__panel:last-child {
  z-index: 1;
}

.admin-accounts-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.admin-accounts-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-accounts-page__filters,
.admin-accounts-page__create-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.admin-accounts-page__filters {
  grid-template-columns: minmax(220px, 1.6fr) minmax(140px, 0.8fr) minmax(140px, 0.8fr) auto;
  align-items: end;
}

.admin-accounts-page__filters label,
.admin-accounts-page__create-form label {
  display: grid;
  gap: 8px;
}

.admin-accounts-page__create-form,
.admin-accounts-page__create-form label {
  overflow: visible;
}

.admin-accounts-page__dropdown {
  position: relative;
}

.admin-accounts-page__dropdown-trigger {
  width: 100%;
  min-height: 46px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 14px;
  border: 1px solid rgba(147, 210, 255, 0.22);
  border-radius: var(--radius-small);
  background:
    linear-gradient(180deg, rgba(17, 43, 74, 0.98), rgba(8, 23, 42, 0.94));
  color: rgba(232, 244, 255, 0.96);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04),
    0 10px 24px rgba(4, 12, 24, 0.16);
  text-align: left;
}

.admin-accounts-page__dropdown-trigger::after {
  content: '';
  width: 10px;
  height: 10px;
  border-right: 2px solid rgba(216, 241, 255, 0.88);
  border-bottom: 2px solid rgba(216, 241, 255, 0.88);
  transform: translateY(-2px) rotate(45deg);
  flex-shrink: 0;
  transition: transform 0.18s ease;
}

.admin-accounts-page__dropdown-trigger--open::after {
  transform: translateY(2px) rotate(-135deg);
}

.admin-accounts-page__dropdown-trigger:focus {
  outline: 2px solid rgba(110, 193, 255, 0.2);
  border-color: rgba(124, 209, 255, 0.92);
  box-shadow:
    0 0 0 4px rgba(15, 92, 158, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.admin-accounts-page__dropdown-trigger:disabled {
  cursor: wait;
  opacity: 0.7;
}

.admin-accounts-page__dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  z-index: 20;
  display: grid;
  gap: 6px;
  padding: 10px;
  border: 1px solid rgba(147, 210, 255, 0.2);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(13, 33, 58, 0.98), rgba(6, 18, 34, 0.98));
  box-shadow:
    0 18px 40px rgba(2, 8, 18, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.admin-accounts-page__dropdown-option {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  color: rgba(232, 244, 255, 0.94);
  text-align: left;
  transition:
    background 0.18s ease,
    border-color 0.18s ease,
    transform 0.18s ease;
}

.admin-accounts-page__dropdown-option:hover,
.admin-accounts-page__dropdown-option--active {
  border-color: rgba(124, 209, 255, 0.26);
  background: rgba(110, 193, 255, 0.12);
  transform: translateY(-1px);
}

.admin-accounts-page__filter-actions {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  gap: 10px;
  white-space: nowrap;
}

.admin-accounts-page__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.admin-accounts-page__summary span {
  color: var(--color-text-muted);
}

.admin-accounts-page__table-wrap {
  overflow: visible;
}

.admin-accounts-page__table {
  width: 100%;
  border-collapse: collapse;
}

.admin-accounts-page__table th,
.admin-accounts-page__table td {
  padding: 12px 10px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.12);
  text-align: left;
  vertical-align: middle;
}

.admin-accounts-page__table th {
  color: var(--color-text-muted);
  font-size: 0.86rem;
  font-weight: 700;
}

.admin-accounts-page__state-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  padding: 5px 12px;
  border: 1px solid rgba(124, 209, 255, 0.2);
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.12);
  color: rgba(222, 242, 255, 0.96);
  font-size: 0.8rem;
  font-weight: 700;
}

.admin-accounts-page__state-badge--status {
  background: rgba(148, 235, 184, 0.12);
  border-color: rgba(148, 235, 184, 0.2);
  color: rgba(210, 255, 227, 0.96);
}

.admin-accounts-page__dropdown--inline {
  min-width: 120px;
}

.admin-accounts-page__dropdown-trigger--inline {
  min-height: 40px;
  padding: 0 12px;
}

.admin-accounts-page__dropdown-trigger--inline::after {
  width: 8px;
  height: 8px;
}

.admin-accounts-page__dropdown-menu--inline {
  min-width: 140px;
  right: auto;
}

.admin-accounts-page__dropdown-menu--modal {
  top: auto;
  bottom: calc(100% + 8px);
}

.admin-accounts-page__lock-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
  font-size: 0.85rem;
  font-weight: 700;
}

.admin-accounts-page__lock-badge--locked {
  background: rgba(255, 143, 143, 0.16);
  color: #ff9f9f;
}

.admin-accounts-page__empty {
  color: var(--color-text-muted);
  text-align: center;
}

.admin-accounts-page__pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
}

.admin-accounts-page__temporary-password {
  display: grid;
  gap: 14px;
}

.admin-accounts-page__manage-loading {
  padding: 28px 0;
  color: var(--color-text-muted);
  text-align: center;
}

.admin-accounts-page__manage {
  display: grid;
  gap: 18px;
}

.admin-accounts-page__manage-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.admin-accounts-page__manage-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: 18px;
  background: rgba(8, 21, 38, 0.62);
}

.admin-accounts-page__manage-card span {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.admin-accounts-page__manage-card strong {
  overflow-wrap: anywhere;
  color: rgba(232, 244, 255, 0.96);
  font-size: 0.98rem;
}

.admin-accounts-page__manage-controls {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.admin-accounts-page__manage-controls label {
  display: grid;
  gap: 8px;
}

.admin-accounts-page__manage-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.admin-accounts-page__manage-actions :deep(button) {
  width: 100%;
}

.admin-accounts-page__temporary-password-code {
  display: block;
  padding: 16px;
  border-radius: 16px;
  background: rgba(7, 18, 32, 0.88);
  color: rgba(232, 244, 255, 0.96);
  font-size: 1.05rem;
  word-break: break-all;
}

@media (max-width: 900px) {
  .admin-accounts-page__create-form {
    grid-template-columns: 1fr;
  }

  .admin-accounts-page__filters {
    grid-template-columns: 1fr;
  }

  .admin-accounts-page__manage-summary,
  .admin-accounts-page__manage-controls,
  .admin-accounts-page__manage-actions {
    grid-template-columns: 1fr;
  }

  .admin-accounts-page__table-wrap {
    overflow-x: auto;
  }

  .admin-accounts-page__header,
  .admin-accounts-page__summary {
    flex-direction: column;
    align-items: stretch;
  }

  .admin-accounts-page__filter-actions {
    justify-content: stretch;
  }
}

:deep(.base-modal__panel) {
  overflow: visible;
}

:deep(.base-modal__body) {
  overflow: visible;
}
</style>
