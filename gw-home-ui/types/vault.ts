export interface Credential {
  credentialUuid: string
  title: string
  categoryUuid: string | null
  categoryName: string | null
  loginId: string | null
  password: string
  memo: string | null
  createdAt: string
}

export interface SaveCredentialPayload {
  title: string
  categoryUuid?: string
  loginId?: string
  password: string
  memo?: string
}

export interface CredentialListParams {
  keyword?: string
  categoryUuid?: string
}

export interface VaultCategory {
  categoryUuid: string
  name: string
  description: string | null
  sortOrder: number
}

export interface SaveVaultCategoryPayload {
  name: string
  description?: string
  sortOrder?: number
}
