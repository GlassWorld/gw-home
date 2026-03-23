import type { ApiResponse } from '~/types/api/common'
import type { BoardDetail, BoardListParams, BoardListResponse } from '~/types/api/board'

export function useBoard() {
  const runtimeConfig = useRuntimeConfig()
  const { authorizedFetch } = useAuth()

  async function fetchBoardList(params: BoardListParams = {}): Promise<BoardListResponse> {
    const response = await $fetch<ApiResponse<BoardListResponse>>('/api/v1/boards', {
      baseURL: runtimeConfig.public.apiBase,
      query: {
        categoryUuid: params.categoryUuid,
        keyword: params.keyword,
        page: params.page,
        size: params.size,
        sortBy: params.sortBy ?? 'createdAt',
        sortDirection: params.sortDirection ?? 'DESC'
      }
    })

    return response.data
  }

  async function fetchBoard(boardUuid: string): Promise<BoardDetail> {
    const response = await $fetch<ApiResponse<BoardDetail>>(`/api/v1/boards/${boardUuid}`, {
      baseURL: runtimeConfig.public.apiBase
    })

    return response.data
  }

  async function deleteBoard(boardUuid: string): Promise<void> {
    await authorizedFetch<ApiResponse<null>>(`/api/v1/boards/${boardUuid}`, {
      method: 'DELETE'
    })
  }

  return {
    fetchBoardList,
    fetchBoard,
    deleteBoard
  }
}
