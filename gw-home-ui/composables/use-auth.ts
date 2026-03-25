import type { ApiResponse } from '~/types/api/common'
import type { LoginRequestBody, RefreshRequestBody, TokenApiResponse } from '~/types/api/auth'
import type { AccountMeApiResponse, ProfileApiResponse, UserProfile } from '~/types/api/user'

interface AuthorizedFetchOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: BodyInit | Record<string, unknown> | null
  query?: Record<string, string | number | undefined>
}

function resolveApiBaseUrl(apiBase: string): string {
  if (import.meta.client || apiBase.startsWith('http://') || apiBase.startsWith('https://')) {
    return apiBase
  }

  const headers = useRequestHeaders(['host', 'x-forwarded-proto'])
  const host = headers.host

  if (!host) {
    return apiBase
  }

  const protocol = headers['x-forwarded-proto'] ?? 'http'
  return `${protocol}://${host}`
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
  const apiBaseUrl = resolveApiBaseUrl(runtimeConfig.public.apiBase)
  const authStore = useAuthStore()
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

  async function fetchCurrentUser(accessTokenOverride?: string): Promise<UserProfile> {
    const authorizationHeader = buildAuthorizationHeader(accessTokenOverride ?? authStore.accessToken)

    const [accountResponse, profileResponse] = await Promise.all([
      $fetch<ApiResponse<AccountMeApiResponse>>('/api/v1/accounts/me', {
        baseURL: apiBaseUrl,
        headers: authorizationHeader
      }),
      $fetch<ApiResponse<ProfileApiResponse>>('/api/v1/profiles/me', {
        baseURL: apiBaseUrl,
        headers: authorizationHeader
      })
    ])

    return {
      memberAccountUuid: accountResponse.data.member_account_uuid,
      memberProfileUuid: profileResponse.data.member_profile_uuid,
      loginId: accountResponse.data.login_id,
      email: accountResponse.data.email,
      role: accountResponse.data.role,
      nickname: profileResponse.data.nickname,
      introduction: profileResponse.data.introduction,
      profileImageUrl: profileResponse.data.profile_image_url,
      createdAt: profileResponse.data.created_at
    }
  }

  async function login(loginId: string, password: string): Promise<void> {
    const requestBody: LoginRequestBody = {
      login_id: loginId,
      password
    }

    const response = await $fetch<ApiResponse<TokenApiResponse>>('/api/v1/auth/login', {
      method: 'POST',
      baseURL: apiBaseUrl,
      body: requestBody
    })

    authStore.setToken(response.data.access_token)
    accessTokenCookie.value = response.data.access_token
    refreshTokenCookie.value = response.data.refresh_token

    const currentUser = await fetchCurrentUser(response.data.access_token)
    authStore.setUser(currentUser)
  }

  async function logout(): Promise<void> {
    const refreshToken = refreshTokenCookie.value

    try {
      if (refreshToken && authStore.accessToken) {
        const requestBody: RefreshRequestBody = { refresh_token: refreshToken }
        await $fetch<ApiResponse<null>>('/api/v1/auth/logout', {
          method: 'POST',
          baseURL: apiBaseUrl,
          headers: buildAuthorizationHeader(authStore.accessToken),
          body: requestBody
        })
      }
    } finally {
      accessTokenCookie.value = null
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

    const requestBody: RefreshRequestBody = {
      refresh_token: savedRefreshToken
    }

    const response = await $fetch<ApiResponse<TokenApiResponse>>('/api/v1/auth/refresh', {
      method: 'POST',
      baseURL: apiBaseUrl,
      body: requestBody
    })

    authStore.setToken(response.data.access_token)
    accessTokenCookie.value = response.data.access_token
    refreshTokenCookie.value = response.data.refresh_token

    const currentUser = await fetchCurrentUser(response.data.access_token)
    authStore.setUser(currentUser)

    return response.data.access_token
  }

  async function ensureAuthenticated(): Promise<boolean> {
    if (authStore.isAuthenticated) {
      return true
    }

    if (accessTokenCookie.value) {
      try {
        authStore.setToken(accessTokenCookie.value)
        const currentUser = await fetchCurrentUser(accessTokenCookie.value)
        authStore.setUser(currentUser)
        return true
      } catch {
        accessTokenCookie.value = null
        authStore.clearAuth()
      }
    }

    if (!refreshTokenCookie.value) {
      accessTokenCookie.value = null
      authStore.clearAuth()
      return false
    }

    try {
      await refreshToken()
      return true
    } catch {
      accessTokenCookie.value = null
      refreshTokenCookie.value = null
      authStore.clearAuth()
      return false
    }
  }

  async function authorizedFetch<T>(path: string, options: AuthorizedFetchOptions = {}): Promise<T> {
    let accessToken = authStore.accessToken ?? accessTokenCookie.value

    if (!accessToken) {
      accessToken = await refreshToken()
    }

    try {
      return await $fetch<T>(path, {
        ...options,
        baseURL: apiBaseUrl,
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
        baseURL: apiBaseUrl,
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
