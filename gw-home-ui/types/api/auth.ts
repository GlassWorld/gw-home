export interface LoginRequestBody {
  login_id: string
  password: string
}

export interface RefreshRequestBody {
  refresh_token: string
}

export interface TokenApiResponse {
  access_token: string
  refresh_token: string
  token_type: string
  expires_in: number
}
