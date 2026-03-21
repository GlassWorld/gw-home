export interface AccountMeApiResponse {
  member_account_uuid: string
  login_id: string
  email: string
  role: string
  created_at: string
}

export interface ProfileApiResponse {
  member_profile_uuid: string
  nickname: string
  introduction: string | null
  profile_image_url: string | null
  created_at: string
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
