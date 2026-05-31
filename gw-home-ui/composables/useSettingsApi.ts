import type { ApiResponse } from '~/types/api/common'
import type { GoogleAuthorizationUrlApiResponse, GoogleCodeRequestBody, GoogleLinkStatusApiResponse } from '~/types/api/auth'
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

  async function fetchGoogleLinkStatus(): Promise<GoogleLinkStatusApiResponse> {
    const response = await authorizedFetch<ApiResponse<GoogleLinkStatusApiResponse>>('/api/v1/auth/google/link/status')

    if (!response.success || response.data === null) {
      throw new Error(response.message ?? 'Google 계정 연동 상태를 불러오지 못했습니다.')
    }

    return response.data
  }

  async function getGoogleLinkAuthorizationUrl(redirectUri: string): Promise<GoogleAuthorizationUrlApiResponse> {
    const response = await authorizedFetch<ApiResponse<GoogleAuthorizationUrlApiResponse>>('/api/v1/auth/google/link-url', {
      method: 'GET',
      query: {
        redirect_uri: redirectUri
      }
    })

    if (!response.success || response.data === null) {
      throw new Error(response.message ?? 'Google 계정 연동 URL을 만들지 못했습니다.')
    }

    return response.data
  }

  async function linkGoogleAccount(code: string, redirectUri: string): Promise<GoogleLinkStatusApiResponse> {
    const payload: GoogleCodeRequestBody = {
      code,
      redirect_uri: redirectUri
    }

    const response = await authorizedFetch<ApiResponse<GoogleLinkStatusApiResponse>>('/api/v1/auth/google/link', {
      method: 'POST',
      body: payload
    })

    if (!response.success || response.data === null) {
      throw new Error(response.message ?? 'Google 계정 연동에 실패했습니다.')
    }

    return response.data
  }

  async function unlinkGoogleAccount(): Promise<void> {
    const response = await authorizedFetch<ApiResponse<null>>('/api/v1/auth/google/link', {
      method: 'DELETE'
    })

    if (!response.success) {
      throw new Error(response.message ?? 'Google 계정 연동 해제에 실패했습니다.')
    }
  }

  return {
    changeNickname,
    changePassword,
    fetchGoogleLinkStatus,
    getGoogleLinkAuthorizationUrl,
    linkGoogleAccount,
    unlinkGoogleAccount
  }
}
