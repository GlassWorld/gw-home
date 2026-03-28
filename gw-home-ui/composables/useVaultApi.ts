import type { ApiResponse } from '~/types/api/common'
import type { Credential, CredentialCategory, CredentialListParams, SaveCredentialPayload } from '~/types/vault'

interface CredentialCategoryApi extends Partial<CredentialCategory> {
  category_uuid?: string
}

export function useVaultApi() {
  const { authorizedFetch } = useAuth()

  function toCredential(credential: Partial<Credential> & {
    credential_uuid?: string
    categories?: CredentialCategoryApi[]
    login_id?: string | null
    created_at?: string
  }): Credential {
    const categories = (credential.categories ?? []).map((category: CredentialCategoryApi) => ({
      categoryUuid: category.categoryUuid ?? category.category_uuid ?? '',
      name: category.name ?? '',
      color: category.color ?? null
    }))

    return {
      credentialUuid: credential.credentialUuid ?? credential.credential_uuid ?? '',
      title: credential.title ?? '',
      categories,
      loginId: credential.loginId ?? credential.login_id ?? null,
      password: credential.password ?? '',
      memo: credential.memo ?? null,
      createdAt: credential.createdAt ?? credential.created_at ?? ''
    }
  }

  function toRequestBody(payload: SaveCredentialPayload): Record<string, string | string[] | undefined> {
    return {
      title: payload.title,
      category_uuids: payload.categoryUuids,
      login_id: payload.loginId,
      password: payload.password,
      memo: payload.memo
    }
  }

  async function fetchCredentialList(params: CredentialListParams = {}): Promise<Credential[]> {
    const response = await authorizedFetch<ApiResponse<Credential[]>>('/api/v1/vault/credentials', {
      method: 'GET',
      query: {
        keyword: params.keyword,
        categoryUuid: params.categoryUuid
      }
    })

    return response.data.map(toCredential)
  }

  async function fetchCredential(credentialUuid: string): Promise<Credential> {
    const response = await authorizedFetch<ApiResponse<Credential>>(`/api/v1/vault/credentials/${credentialUuid}`)
    return toCredential(response.data)
  }

  async function createCredential(payload: SaveCredentialPayload): Promise<Credential> {
    const response = await authorizedFetch<ApiResponse<Credential>>('/api/v1/vault/credentials', {
      method: 'POST',
      body: toRequestBody(payload)
    })

    return toCredential(response.data)
  }

  async function updateCredential(credentialUuid: string, payload: SaveCredentialPayload): Promise<Credential> {
    const response = await authorizedFetch<ApiResponse<Credential>>(`/api/v1/vault/credentials/${credentialUuid}`, {
      method: 'PUT',
      body: toRequestBody(payload)
    })

    return toCredential(response.data)
  }

  async function removeCredential(credentialUuid: string): Promise<void> {
    await authorizedFetch<ApiResponse<null>>(`/api/v1/vault/credentials/${credentialUuid}`, {
      method: 'DELETE'
    })
  }

  return {
    fetchCredentialList,
    fetchCredential,
    createCredential,
    updateCredential,
    removeCredential
  }
}
