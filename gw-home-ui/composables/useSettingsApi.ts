import type { ApiResponse } from '~/types/api/common'
import type { ProfileApiResponse, UserProfile } from '~/types/api/user'

export function useSettingsApi() {
  const authStore = useAuthStore()
  const { authorizedFetch } = useAuth()

  async function changeNickname(nickname: string): Promise<void> {
    const currentUser = authStore.currentUser

    if (!currentUser) {
      throw new Error('로그인 정보가 없습니다.')
    }

    const payload: Record<string, string | null> = {
      nickname,
      introduction: currentUser.introduction,
      profile_image_url: currentUser.profileImageUrl
    }

    const response = await authorizedFetch<ApiResponse<ProfileApiResponse>>('/api/v1/profiles/me', {
      method: 'PUT',
      body: payload
    })

    if (!response.success || response.data === null) {
      throw new Error(response.message ?? '닉네임 변경에 실패했습니다.')
    }

    const nextUser: UserProfile = {
      ...currentUser,
      nickname: response.data.nickname,
      introduction: response.data.introduction,
      profileImageUrl: response.data.profile_image_url
    }
    authStore.setUser(nextUser)
  }

  async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
    const payload: Record<string, string> = {
      current_password: currentPassword,
      new_password: newPassword
    }

    const response = await authorizedFetch<ApiResponse<null>>('/api/v1/accounts/me/password', {
      method: 'PUT',
      body: payload
    })

    if (!response.success) {
      throw new Error(response.message ?? '비밀번호 변경에 실패했습니다.')
    }
  }

  return {
    changeNickname,
    changePassword
  }
}
