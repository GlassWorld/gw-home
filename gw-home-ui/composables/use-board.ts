import type { ApiResponse } from '~/types/api/common'
import type { BoardDetail, BoardListParams, BoardListResponse } from '~/types/api/board'

export function useBoard() {
  const runtimeConfig = useRuntimeConfig()
  const apiBaseUrl = import.meta.client || runtimeConfig.public.apiBase.startsWith('http://') || runtimeConfig.public.apiBase.startsWith('https://')
    ? runtimeConfig.public.apiBase
    : (() => {
        const headers = useRequestHeaders(['host', 'x-forwarded-proto'])
        const host = headers.host

        if (!host) {
          return runtimeConfig.public.apiBase
        }

        const protocol = headers['x-forwarded-proto'] ?? 'http'
        return `${protocol}://${host}${runtimeConfig.public.apiBase}`
      })()
  const { authorizedFetch } = useAuth()

  async function fetchBoardList(params: BoardListParams = {}): Promise<BoardListResponse> {
    const response = await $fetch<ApiResponse<BoardListResponse>>('/api/v1/boards', {
      baseURL: apiBaseUrl,
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
      baseURL: apiBaseUrl
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
