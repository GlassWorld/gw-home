import type { AdminAccount, AdminAccountDetail, AccountRole, AccountStatus } from '~/types/admin'
import type { AdminAccountListPage } from '~/types/admin'
import { formatDateTime } from '~/utils/date'
import {
  adminAccountRoleOptions,
  adminAccountStatusOptions,
  createInitialAdminAccountFilterState,
  createInitialAdminAccountForm
} from '~/features/admin/types/admin-account-management'

export function useAdminAccountManagement() {
  const { fetchAccounts, fetchAccount, createAccount, updateRole, updateStatus, updateOtpRequired, deleteAccount, unlockAccount, resetOtpFailure, resetOtp, resetPassword } = useAdminAccountApi()
  const { showToast } = useToast()
  const { confirm } = useDialog()

  const filters = reactive(createInitialAdminAccountFilterState())
  const createForm = reactive(createInitialAdminAccountForm())

  const accountPage = ref<AdminAccountListPage>({
    content: [],
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
  const updatingOtpRequiredUuid = ref('')
  const deletingUuid = ref('')
  const openDropdownKey = ref('')
  const actionLoadingKey = ref('')
  const isTemporaryPasswordModalVisible = ref(false)
  const temporaryPassword = ref('')
  const temporaryPasswordTargetLoginId = ref('')
  const isManageModalVisible = ref(false)
  const isManageLoading = ref(false)
  const selectedAccount = ref<AdminAccountDetail | null>(null)

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

  function resetFilters() {
    Object.assign(filters, createInitialAdminAccountFilterState())
  }

  function resetCreateForm() {
    Object.assign(createForm, createInitialAdminAccountForm())
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
    resetCreateForm()
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

  async function reloadSelectedAccount(account: AdminAccount) {
    await loadAccounts()

    if (selectedAccount.value?.uuid === account.uuid) {
      selectedAccount.value = await fetchAccount(account.uuid)
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
      await reloadSelectedAccount(account)
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
      await reloadSelectedAccount(account)
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '상태 변경에 실패했습니다.', { variant: 'error' })
    } finally {
      updatingStatusUuid.value = ''
    }
  }

  async function handleChangeOtpRequired(account: AdminAccount, otpRequired: boolean) {
    if (updatingOtpRequiredUuid.value) {
      return
    }

    updatingOtpRequiredUuid.value = account.uuid

    try {
      await updateOtpRequired(account.uuid, { otpRequired })
      showToast(otpRequired ? 'OTP 사용 필수로 변경했습니다.' : 'OTP 사용 선택으로 변경했습니다.', { variant: 'success' })
      await reloadSelectedAccount(account)
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? 'OTP 사용 여부 변경에 실패했습니다.', { variant: 'error' })
    } finally {
      updatingOtpRequiredUuid.value = ''
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

  async function runAccountAction(account: AdminAccount, action: string, callback: () => Promise<void>, successMessage: string, failureMessage: string) {
    if (actionLoadingKey.value) {
      return
    }

    actionLoadingKey.value = getActionLoadingKey(action, account.uuid)

    try {
      await callback()
      showToast(successMessage, { variant: 'success' })
      await reloadSelectedAccount(account)
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? failureMessage, { variant: 'error' })
    } finally {
      actionLoadingKey.value = ''
    }
  }

  async function handleUnlockAccount(account: AdminAccount) {
    await runAccountAction(account, 'unlock', () => unlockAccount(account.uuid).then(() => undefined), '로그인 잠금을 초기화했습니다.', '로그인 잠금 초기화에 실패했습니다.')
  }

  async function handleResetOtpFailure(account: AdminAccount) {
    await runAccountAction(account, 'otp-failure', () => resetOtpFailure(account.uuid).then(() => undefined), 'OTP 실패 횟수를 초기화했습니다.', 'OTP 실패 초기화에 실패했습니다.')
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

    await runAccountAction(account, 'otp-reset', () => resetOtp(account.uuid).then(() => undefined), 'OTP 재등록 초기화를 완료했습니다.', 'OTP 재등록 초기화에 실패했습니다.')
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

  function handleToggleOtpRequired(account: AdminAccount, otpRequired: boolean) {
    void handleChangeOtpRequired(account, otpRequired)
  }

  onMounted(() => {
    document.addEventListener('mousedown', handleDocumentMouseDown)
  })

  onBeforeUnmount(() => {
    document.removeEventListener('mousedown', handleDocumentMouseDown)
  })

  return {
    adminAccountRoleOptions,
    adminAccountStatusOptions,
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
    closeDropdown,
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
  }
}
