import type { PageResponse } from '~/types/api/common'

export type AccountRole = 'ADMIN' | 'USER' | 'GUEST'
export type AccountStatus = 'ACTIVE' | 'INACTIVE'

export interface AdminAccount {
  uuid: string
  loginId: string
  email: string
  role: AccountRole
  acctStat: AccountStatus
  lckYn: boolean
  createdAt: string
}

export interface AdminAccountDetail extends AdminAccount {
  lckAt: string | null
  updatedAt: string | null
}

export interface AdminCreateAccountForm {
  loginId: string
  email: string
  password: string
  role: AccountRole
}

export interface UpdateRoleForm {
  role: AccountRole
}

export interface UpdateStatusForm {
  status: AccountStatus
}

export interface AdminPasswordResetResponse {
  temporaryPassword: string
}

export interface AdminAccountListParams {
  loginId?: string
  role?: AccountRole | ''
  status?: AccountStatus | ''
  page?: number
  size?: number
}

export type AdminAccountListPage = PageResponse<AdminAccount>
