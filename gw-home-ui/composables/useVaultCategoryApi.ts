import type { ApiResponse } from '~/types/api/common'
import type { SaveVaultCategoryPayload, VaultCategory } from '~/types/vault'

export function useVaultCategoryApi() {
  const { authorizedFetch } = useAuth()

  function toCategory(category: Partial<VaultCategory> & {
    category_uuid?: string
    sort_order?: number
  }): VaultCategory {
    return {
      categoryUuid: category.categoryUuid ?? category.category_uuid ?? '',
      name: category.name ?? '',
      description: category.description ?? null,
      sortOrder: Number(category.sortOrder ?? category.sort_order ?? 0)
    }
  }

  function toRequestBody(payload: SaveVaultCategoryPayload): Record<string, string | number | undefined> {
    return {
      name: payload.name,
      description: payload.description,
      sort_order: payload.sortOrder
    }
  }

  async function fetchCategoryList(): Promise<VaultCategory[]> {
    const response = await authorizedFetch<ApiResponse<VaultCategory[]>>('/api/v1/vault/categories', {
      method: 'GET'
    })

    return response.data.map(toCategory)
  }

  async function fetchAdminCategoryList(): Promise<VaultCategory[]> {
    const response = await authorizedFetch<ApiResponse<VaultCategory[]>>('/api/v1/admin/vault-categories', {
      method: 'GET'
    })

    return response.data.map(toCategory)
  }

  async function createCategory(payload: SaveVaultCategoryPayload): Promise<VaultCategory> {
    const response = await authorizedFetch<ApiResponse<VaultCategory>>('/api/v1/admin/vault-categories', {
      method: 'POST',
      body: toRequestBody(payload)
    })

    return toCategory(response.data)
  }

  async function updateCategory(categoryUuid: string, payload: SaveVaultCategoryPayload): Promise<VaultCategory> {
    const response = await authorizedFetch<ApiResponse<VaultCategory>>(`/api/v1/admin/vault-categories/${categoryUuid}`, {
      method: 'PUT',
      body: toRequestBody(payload)
    })

    return toCategory(response.data)
  }

  async function removeCategory(categoryUuid: string): Promise<void> {
    await authorizedFetch<ApiResponse<null>>(`/api/v1/admin/vault-categories/${categoryUuid}`, {
      method: 'DELETE'
    })
  }

  return {
    fetchCategoryList,
    fetchAdminCategoryList,
    createCategory,
    updateCategory,
    removeCategory
  }
}
