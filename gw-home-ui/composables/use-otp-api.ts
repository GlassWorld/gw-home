import type { ApiResponse } from '~/types/api/common'
import type { OtpStatusApiResponse, OtpSetupApiResponse, TokenApiResponse } from '~/types/api/auth'

export function useOtpApi() {
  const runtimeConfig = useRuntimeConfig()
  const authStore = useAuthStore()
  const { authorizedFetch, fetchCurrentUser } = useAuth()
  const accessTokenCookie = useCookie<string | null>('gw-home-access-token', {
    default: () => null,
    sameSite: 'lax',
    path: '/'
  })
  const refreshTokenCookie = useCookie<string | null>('gw-home-refresh-token', {
    default: () => null,
    sameSite: 'lax',
    path: '/'
  })

  async function fetchOtpStatus(): Promise<{ otpEnabled: boolean }> {
    const response = await authorizedFetch<ApiResponse<OtpStatusApiResponse>>('/api/v1/auth/otp/status')

    if (!response.success || response.data === null) {
      throw new Error(response.message ?? 'OTP 상태를 불러오지 못했습니다.')
    }

    return {
      otpEnabled: response.data.otp_enabled
    }
  }

  async function setupOtp(): Promise<{ otpAuthUrl: string }> {
    const response = await authorizedFetch<ApiResponse<OtpSetupApiResponse>>('/api/v1/auth/otp/setup', {
      method: 'POST'
    })

    if (!response.success || response.data === null) {
      throw new Error(response.message ?? 'OTP 설정을 시작하지 못했습니다.')
    }

    return {
      otpAuthUrl: response.data.otp_auth_url
    }
  }

  async function activateOtp(otpCode: string): Promise<void> {
    const response = await authorizedFetch<ApiResponse<null>>('/api/v1/auth/otp/activate', {
      method: 'POST',
      body: { otp_code: otpCode }
    })

    if (!response.success) {
      throw new Error(response.message ?? 'OTP 활성화에 실패했습니다.')
    }
  }

  async function verifyOtp(otpTempToken: string, otpCode: string): Promise<void> {
    const response = await $fetch<ApiResponse<TokenApiResponse>>('/api/v1/auth/otp/verify', {
      method: 'POST',
      baseURL: runtimeConfig.public.apiBase,
      body: {
        otp_code: otpCode,
        otp_temp_token: otpTempToken
      }
    })

    if (!response.success || response.data === null) {
      throw new Error(response.message ?? 'OTP 인증에 실패했습니다.')
    }

    authStore.setToken(response.data.access_token)
    accessTokenCookie.value = response.data.access_token
    refreshTokenCookie.value = response.data.refresh_token

    const currentUser = await fetchCurrentUser(response.data.access_token)
    authStore.setUser(currentUser)
  }

  async function disableOtp(otpCode: string): Promise<void> {
    const response = await authorizedFetch<ApiResponse<null>>('/api/v1/auth/otp/disable', {
      method: 'POST',
      body: { otp_code: otpCode }
    })

    if (!response.success) {
      throw new Error(response.message ?? 'OTP 해제에 실패했습니다.')
    }
  }

  return {
    fetchOtpStatus,
    setupOtp,
    activateOtp,
    verifyOtp,
    disableOtp
  }
}
