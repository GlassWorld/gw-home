import type { ApiResponse, PageResponse } from '~/types/api/common'
import type {
  AdminAccount,
  AdminAccountDetail,
  AdminAccountListParams,
  AdminCreateAccountForm,
  AdminPasswordResetResponse,
  UpdateRoleForm,
  UpdateStatusForm
} from '~/types/admin'

interface AdminAccountApi {
  uuid?: string
  login_id?: string
  email?: string
  role?: AdminAccount['role']
  acct_stat?: AdminAccount['acctStat']
  lck_yn?: boolean
  created_at?: string
  lck_at?: string | null
  updated_at?: string | null
}

interface AdminPasswordResetApi {
  temporary_password?: string
}

interface AdminAccountPageApi {
  content?: AdminAccountApi[]
  page?: number
  size?: number
  total_count?: number
  total_pages?: number
}

function toAdminAccount(account: AdminAccountApi): AdminAccount {
  return {
    uuid: account.uuid ?? '',
    loginId: account.login_id ?? '',
    email: account.email ?? '',
    role: account.role ?? 'USER',
    acctStat: account.acct_stat ?? 'ACTIVE',
    lckYn: account.lck_yn ?? false,
    createdAt: account.created_at ?? ''
  }
}

function toAdminAccountDetail(account: AdminAccountApi): AdminAccountDetail {
  return {
    ...toAdminAccount(account),
    lckAt: account.lck_at ?? null,
    updatedAt: account.updated_at ?? null
  }
}

export function useAdminAccountApi() {
  const { authorizedFetch } = useAuth()

  async function fetchAccounts(params: AdminAccountListParams = {}): Promise<PageResponse<AdminAccount>> {
    const response = await authorizedFetch<ApiResponse<AdminAccountPageApi>>('/api/v1/admin/accounts', {
      method: 'GET',
      query: {
        loginId: params.loginId,
        role: params.role || undefined,
        status: params.status || undefined,
        page: params.page ?? 1,
        size: params.size ?? 20
      }
    })

    return {
      content: (response.data.content ?? []).map(toAdminAccount),
      page: response.data.page ?? 1,
      size: response.data.size ?? 20,
      totalCount: response.data.total_count ?? 0,
      totalPages: response.data.total_pages ?? 0
    }
  }

  async function fetchAccount(uuid: string): Promise<AdminAccountDetail> {
    const response = await authorizedFetch<ApiResponse<AdminAccountApi>>(`/api/v1/admin/accounts/${uuid}`)
    return toAdminAccountDetail(response.data)
  }

  async function createAccount(form: AdminCreateAccountForm): Promise<AdminAccountDetail> {
    const response = await authorizedFetch<ApiResponse<AdminAccountApi>>('/api/v1/admin/accounts', {
      method: 'POST',
      body: {
        login_id: form.loginId,
        email: form.email,
        password: form.password,
        role: form.role
      }
    })
    return toAdminAccountDetail(response.data)
  }

  async function updateRole(uuid: string, form: UpdateRoleForm): Promise<AdminAccountDetail> {
    const response = await authorizedFetch<ApiResponse<AdminAccountApi>>(`/api/v1/admin/accounts/${uuid}/role`, {
      method: 'PUT',
      body: {
        role: form.role
      }
    })
    return toAdminAccountDetail(response.data)
  }

  async function updateStatus(uuid: string, form: UpdateStatusForm): Promise<AdminAccountDetail> {
    const response = await authorizedFetch<ApiResponse<AdminAccountApi>>(`/api/v1/admin/accounts/${uuid}/status`, {
      method: 'PUT',
      body: {
        status: form.status
      }
    })
    return toAdminAccountDetail(response.data)
  }

  async function deleteAccount(uuid: string): Promise<void> {
    await authorizedFetch<ApiResponse<null>>(`/api/v1/admin/accounts/${uuid}`, {
      method: 'DELETE'
    })
  }

  async function unlockAccount(uuid: string): Promise<AdminAccountDetail> {
    const response = await authorizedFetch<ApiResponse<AdminAccountApi>>(`/api/v1/admin/accounts/${uuid}/unlock`, {
      method: 'PUT'
    })
    return toAdminAccountDetail(response.data)
  }

  async function resetOtpFailure(uuid: string): Promise<AdminAccountDetail> {
    const response = await authorizedFetch<ApiResponse<AdminAccountApi>>(`/api/v1/admin/accounts/${uuid}/otp-failure/reset`, {
      method: 'PUT'
    })
    return toAdminAccountDetail(response.data)
  }

  async function resetOtp(uuid: string): Promise<AdminAccountDetail> {
    const response = await authorizedFetch<ApiResponse<AdminAccountApi>>(`/api/v1/admin/accounts/${uuid}/otp/reset`, {
      method: 'PUT'
    })
    return toAdminAccountDetail(response.data)
  }

  async function resetPassword(uuid: string): Promise<AdminPasswordResetResponse> {
    const response = await authorizedFetch<ApiResponse<AdminPasswordResetApi>>(`/api/v1/admin/accounts/${uuid}/password/reset`, {
      method: 'PUT'
    })
    return {
      temporaryPassword: response.data.temporary_password ?? ''
    }
  }

  return {
    fetchAccounts,
    fetchAccount,
    createAccount,
    updateRole,
    updateStatus,
    deleteAccount,
    unlockAccount,
    resetOtpFailure,
    resetOtp,
    resetPassword
  }
}
