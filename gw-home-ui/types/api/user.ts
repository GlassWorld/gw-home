export interface AccountMeResponse {
  memberAccountUuid: string
  loginId: string
  email: string
  role: string
  createdAt: string
}

export interface ProfileResponse {
  memberProfileUuid: string
  nickname: string
  introduction: string | null
  profileImageUrl: string | null
  createdAt: string
}

export interface UserProfile {
  memberAccountUuid: string
  memberProfileUuid: string
  loginId: string
  email: string
  role: string
  nickname: string
  introduction: string | null
  profileImageUrl: string | null
  createdAt: string
}
