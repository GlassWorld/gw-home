import type { ApiResponse } from '~/types/api/common'
import type { LoginRequest, RefreshRequest, TokenResponse } from '~/types/api/auth'
import type { AccountMeResponse, ProfileResponse, UserProfile } from '~/types/api/user'

interface AuthorizedFetchOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: BodyInit | Record<string, unknown> | null
  query?: Record<string, string | number | undefined>
}

function buildAuthorizationHeader(accessToken: string | null): Record<string, string> {
  if (!accessToken) {
    return {}
  }

  return {
    Authorization: `Bearer ${accessToken}`
  }
}

export function useAuth() {
  const runtimeConfig = useRuntimeConfig()
  const authStore = useAuthStore()
  const refreshTokenCookie = useCookie<string | null>('gw-home-refresh-token', {
    default: () => null,
    sameSite: 'lax'
  })

  async function fetchCurrentUser(accessTokenOverride?: string): Promise<UserProfile> {
    const authorizationHeader = buildAuthorizationHeader(accessTokenOverride ?? authStore.accessToken)

    const [accountResponse, profileResponse] = await Promise.all([
      $fetch<ApiResponse<AccountMeResponse>>('/api/v1/accounts/me', {
        baseURL: runtimeConfig.public.apiBase,
        headers: authorizationHeader
      }),
      $fetch<ApiResponse<ProfileResponse>>('/api/v1/profiles/me', {
        baseURL: runtimeConfig.public.apiBase,
        headers: authorizationHeader
      })
    ])

    return {
      memberAccountUuid: accountResponse.data.memberAccountUuid,
      memberProfileUuid: profileResponse.data.memberProfileUuid,
      loginId: accountResponse.data.loginId,
      email: accountResponse.data.email,
      role: accountResponse.data.role,
      nickname: profileResponse.data.nickname,
      introduction: profileResponse.data.introduction,
      profileImageUrl: profileResponse.data.profileImageUrl,
      createdAt: profileResponse.data.createdAt
    }
  }

  async function login(loginId: string, password: string): Promise<void> {
    const requestBody: LoginRequest = {
      loginId,
      password
    }

    const response = await $fetch<ApiResponse<TokenResponse>>('/api/v1/auth/login', {
      method: 'POST',
      baseURL: runtimeConfig.public.apiBase,
      body: requestBody
    })

    authStore.setToken(response.data.accessToken)
    refreshTokenCookie.value = response.data.refreshToken

    const currentUser = await fetchCurrentUser(response.data.accessToken)
    authStore.setUser(currentUser)
  }

  async function logout(): Promise<void> {
    const refreshToken = refreshTokenCookie.value

    try {
      if (refreshToken && authStore.accessToken) {
        const requestBody: RefreshRequest = { refreshToken }
        await $fetch<ApiResponse<null>>('/api/v1/auth/logout', {
          method: 'POST',
          baseURL: runtimeConfig.public.apiBase,
          headers: buildAuthorizationHeader(authStore.accessToken),
          body: requestBody
        })
      }
    } finally {
      refreshTokenCookie.value = null
      authStore.clearAuth()
    }
  }

  async function refreshToken(): Promise<string> {
    const savedRefreshToken = refreshTokenCookie.value

    if (!savedRefreshToken) {
      throw createError({
        statusCode: 401,
        statusMessage: '리프레시 토큰이 없습니다.'
      })
    }

    const requestBody: RefreshRequest = {
      refreshToken: savedRefreshToken
    }

    const response = await $fetch<ApiResponse<TokenResponse>>('/api/v1/auth/refresh', {
      method: 'POST',
      baseURL: runtimeConfig.public.apiBase,
      body: requestBody
    })

    authStore.setToken(response.data.accessToken)
    refreshTokenCookie.value = response.data.refreshToken

    const currentUser = await fetchCurrentUser(response.data.accessToken)
    authStore.setUser(currentUser)

    return response.data.accessToken
  }

  async function ensureAuthenticated(): Promise<boolean> {
    if (authStore.isAuthenticated) {
      return true
    }

    if (!refreshTokenCookie.value) {
      authStore.clearAuth()
      return false
    }

    try {
      await refreshToken()
      return true
    } catch {
      refreshTokenCookie.value = null
      authStore.clearAuth()
      return false
    }
  }

  async function authorizedFetch<T>(path: string, options: AuthorizedFetchOptions = {}): Promise<T> {
    let accessToken = authStore.accessToken

    if (!accessToken) {
      accessToken = await refreshToken()
    }

    try {
      return await $fetch<T>(path, {
        ...options,
        baseURL: runtimeConfig.public.apiBase,
        headers: {
          ...buildAuthorizationHeader(accessToken)
        }
      })
    } catch (error) {
      const fetchError = error as { response?: { status?: number } }

      if (fetchError.response?.status !== 401) {
        throw error
      }

      const refreshedToken = await refreshToken()

      return await $fetch<T>(path, {
        ...options,
        baseURL: runtimeConfig.public.apiBase,
        headers: {
          ...buildAuthorizationHeader(refreshedToken)
        }
      })
    }
  }

  return {
    login,
    logout,
    refreshToken,
    ensureAuthenticated,
    fetchCurrentUser,
    authorizedFetch
  }
}
