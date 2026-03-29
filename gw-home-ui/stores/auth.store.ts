import type { UserProfile } from '~/types/api/user'

interface AuthState {
  currentUser: UserProfile | null
  accessToken: string | null
}

function toPlainUserProfile(user: UserProfile): UserProfile {
  return {
    memberAccountUuid: user.memberAccountUuid,
    memberProfileUuid: user.memberProfileUuid,
    loginId: user.loginId,
    email: user.email,
    role: user.role,
    nickname: user.nickname,
    introduction: user.introduction,
    profileImageUrl: user.profileImageUrl,
    createdAt: user.createdAt
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    currentUser: null,
    accessToken: null
  }),

  getters: {
    isAuthenticated: (state) => state.currentUser !== null && state.accessToken !== null
  },

  actions: {
    setUser(user: UserProfile | null) {
      this.currentUser = user ? toPlainUserProfile(user) : null
    },

    setToken(token: string | null) {
      this.accessToken = token
    },

    clearAuth() {
      this.currentUser = null
      this.accessToken = null
    }
  }
})
