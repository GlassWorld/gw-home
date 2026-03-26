package com.gw.api.dto.auth;

public record LoginResponse(
        String loginStatus,
        TokenResponse tokenResponse,
        String otpTempToken
) {
}
