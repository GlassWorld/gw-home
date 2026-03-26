import type { ApiResponse } from '~/types/api/common'
import type { MemoApiResponse, SaveMemoPayload } from '~/types/api/memo'

export function useMemoApi() {
  const { authorizedFetch } = useAuth()

  async function fetchMemo(): Promise<string> {
    const response = await authorizedFetch<ApiResponse<MemoApiResponse>>('/api/v1/profiles/me/memo')

    if (!response.success || response.data === null) {
      throw new Error(response.message ?? '메모를 불러오지 못했습니다.')
    }

    return response.data.memo ?? ''
  }

  async function saveMemo(memo: string): Promise<void> {
    const payload: Record<string, string> = { memo }
    const response = await authorizedFetch<ApiResponse<MemoApiResponse>>('/api/v1/profiles/me/memo', {
      method: 'PUT',
      body: payload
    })

    if (!response.success) {
      throw new Error(response.message ?? '메모를 저장하지 못했습니다.')
    }
  }

  return {
    fetchMemo,
    saveMemo
  }
}
