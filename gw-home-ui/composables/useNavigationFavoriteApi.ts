import type { ApiResponse } from '~/types/api/common'
import type { NavigationFavoriteApiResponse, SaveNavigationFavoritePayload } from '~/types/api/navigation-favorite'

export function useNavigationFavoriteApi() {
  const { authorizedFetch } = useAuth()

  async function fetchNavigationFavorites(): Promise<string[]> {
    const response = await authorizedFetch<ApiResponse<NavigationFavoriteApiResponse>>('/api/v1/profiles/me/navigation-favorites')

    if (!response.success || response.data === null) {
      throw new Error(response.message ?? '즐겨찾기 메뉴를 불러오지 못했습니다.')
    }

    return response.data.favorite_menus ?? []
  }

  async function saveNavigationFavorites(favoriteMenus: string[]): Promise<void> {
    const payload: Record<string, string[]> = {
      favorite_menus: favoriteMenus
    }

    const response = await authorizedFetch<ApiResponse<NavigationFavoriteApiResponse>>('/api/v1/profiles/me/navigation-favorites', {
      method: 'PUT',
      body: payload
    })

    if (!response.success) {
      throw new Error(response.message ?? '즐겨찾기 메뉴를 저장하지 못했습니다.')
    }
  }

  return {
    fetchNavigationFavorites,
    saveNavigationFavorites
  }
}
