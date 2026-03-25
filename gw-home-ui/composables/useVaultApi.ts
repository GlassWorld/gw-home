import type { ApiResponse } from '~/types/api/common'
import type { Credential, CredentialListParams, SaveCredentialPayload } from '~/types/vault'

export function useVaultApi() {
  const { authorizedFetch } = useAuth()

  function toCredential(credential: Partial<Credential> & {
    credential_uuid?: string
    category_uuid?: string | null
    category_name?: string | null
    login_id?: string | null
    created_at?: string
  }): Credential {
    return {
      credentialUuid: credential.credentialUuid ?? credential.credential_uuid ?? '',
      title: credential.title ?? '',
      categoryUuid: credential.categoryUuid ?? credential.category_uuid ?? null,
      categoryName: credential.categoryName ?? credential.category_name ?? null,
      loginId: credential.loginId ?? credential.login_id ?? null,
      password: credential.password ?? '',
      memo: credential.memo ?? null,
      createdAt: credential.createdAt ?? credential.created_at ?? ''
    }
  }

  function toRequestBody(payload: SaveCredentialPayload): Record<string, string | undefined> {
    return {
      title: payload.title,
      category_uuid: payload.categoryUuid,
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
