import type { AccountRole, AccountStatus, AdminCreateAccountForm } from '~/types/admin'

export interface AdminAccountFilterState {
  loginId: string
  role: AccountRole | ''
  status: AccountStatus | ''
  page: number
  size: number
}

export const adminAccountRoleOptions: AccountRole[] = ['ADMIN', 'USER', 'GUEST']
export const adminAccountStatusOptions: AccountStatus[] = ['ACTIVE', 'INACTIVE']

export function createInitialAdminAccountFilterState(): AdminAccountFilterState {
  return {
    loginId: '',
    role: '',
    status: '',
    page: 1,
    size: 20
  }
}

export function createInitialAdminAccountForm(): AdminCreateAccountForm {
  return {
    loginId: '',
    email: '',
    password: '',
    role: 'USER'
  }
}
