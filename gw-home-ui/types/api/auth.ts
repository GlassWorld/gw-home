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

export interface LoginApiResponse {
  login_status: 'SUCCESS' | 'OTP_REQUIRED'
  token_response: TokenApiResponse | null
  otp_temp_token: string | null
}

export interface OtpSetupApiResponse {
  otp_auth_url: string
}

export interface OtpStatusApiResponse {
  otp_enabled: boolean
}
