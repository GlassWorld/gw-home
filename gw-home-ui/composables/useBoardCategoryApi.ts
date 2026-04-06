import type { ApiResponse } from '~/types/api/common'
import type { BoardCategory, SaveBoardCategoryPayload } from '~/types/api/board'

export function useBoardCategoryApi() {
  const { authorizedFetch } = useAuth()

  function toCategory(category: Partial<BoardCategory> & {
    category_name?: string
    sort_order?: number
  }): BoardCategory {
    return {
      categoryUuid: category.categoryUuid ?? '',
      categoryName: category.categoryName ?? category.category_name ?? '',
      sortOrder: Number(category.sortOrder ?? category.sort_order ?? 0)
    }
  }

  function toRequestBody(payload: SaveBoardCategoryPayload): Record<string, string | number> {
    return {
      name: payload.name,
      sortOrder: payload.sortOrder
    }
  }

  async function fetchAdminBoardCategoryList(): Promise<BoardCategory[]> {
    const response = await authorizedFetch<ApiResponse<BoardCategory[]>>('/api/v1/admin/board-categories', {
      method: 'GET'
    })

    return response.data.map(toCategory)
  }

  async function createBoardCategory(payload: SaveBoardCategoryPayload): Promise<BoardCategory> {
    const response = await authorizedFetch<ApiResponse<BoardCategory>>('/api/v1/admin/board-categories', {
      method: 'POST',
      body: toRequestBody(payload)
    })

    return toCategory(response.data)
  }

  async function updateBoardCategory(categoryUuid: string, payload: SaveBoardCategoryPayload): Promise<BoardCategory> {
    const response = await authorizedFetch<ApiResponse<BoardCategory>>(`/api/v1/admin/board-categories/${categoryUuid}`, {
      method: 'PUT',
      body: toRequestBody(payload)
    })

    return toCategory(response.data)
  }

  async function removeBoardCategory(categoryUuid: string): Promise<void> {
    await authorizedFetch<ApiResponse<null>>(`/api/v1/admin/board-categories/${categoryUuid}`, {
      method: 'DELETE'
    })
  }

  return {
    fetchAdminBoardCategoryList,
    createBoardCategory,
    updateBoardCategory,
    removeBoardCategory
  }
}
