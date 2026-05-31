package com.gw.api.convert.auth;

import com.gw.api.dto.auth.LoginResponse;
import com.gw.api.dto.auth.OtpSetupResponse;
import com.gw.api.dto.auth.OtpStatusResponse;
import com.gw.api.dto.auth.TokenResponse;
import com.gw.api.dto.auth.GoogleAuthorizationUrlResponse;
import com.gw.api.dto.auth.GoogleLinkStatusResponse;
import com.gw.share.common.policy.AuthPolicy;
import com.gw.share.vo.account.AcctVo;

public final class AuthConvert {

    private AuthConvert() {
    }

    // OTP 추가 인증이 필요한 로그인 응답으로 변환한다.
    public static LoginResponse toOtpRequiredLoginResponse(String otpTempToken) {
        return new LoginResponse(AuthPolicy.LOGIN_STATUS_OTP_REQUIRED, null, otpTempToken);
    }

    // OTP 설정이 필요한 로그인 응답으로 변환한다.
    public static LoginResponse toOtpSetupRequiredLoginResponse(TokenResponse tokenResponse) {
        return new LoginResponse(AuthPolicy.LOGIN_STATUS_OTP_SETUP_REQUIRED, tokenResponse, null);
    }

    // 로그인 성공 응답으로 변환한다.
    public static LoginResponse toSuccessLoginResponse(TokenResponse tokenResponse) {
        return new LoginResponse(AuthPolicy.LOGIN_STATUS_SUCCESS, tokenResponse, null);
    }

    // Google 인증 URL 응답으로 변환한다.
    public static GoogleAuthorizationUrlResponse toGoogleAuthorizationUrlResponse(String authorizationUrl, String state) {
        return new GoogleAuthorizationUrlResponse(authorizationUrl, state);
    }

    // Google 계정 연동 상태 응답으로 변환한다.
    public static GoogleLinkStatusResponse toGoogleLinkStatusResponse(AcctVo account) {
        return new GoogleLinkStatusResponse(
                account.getGoogleSub() != null && !account.getGoogleSub().isBlank(),
                account.getGoogleEmail(),
                account.getGoogleLinkedAt()
        );
    }

    // 토큰 발급 결과를 응답으로 변환한다.
    public static TokenResponse toTokenResponse(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, refreshToken, AuthPolicy.TOKEN_TYPE_BEARER, expiresInSeconds);
    }

    // OTP 설정 URL을 응답으로 변환한다.
    public static OtpSetupResponse toOtpSetupResponse(String otpAuthUrl) {
        return new OtpSetupResponse(otpAuthUrl);
    }

    // OTP 활성화 상태를 응답으로 변환한다.
    public static OtpStatusResponse toOtpStatusResponse(boolean otpEnabled) {
        return new OtpStatusResponse(otpEnabled);
    }
}
