package com.gw.api.controller.auth;

import com.gw.api.dto.auth.GoogleAuthorizationUrlResponse;
import com.gw.api.dto.auth.GoogleCodeRequest;
import com.gw.api.dto.auth.GoogleLinkStatusResponse;
import com.gw.api.dto.auth.LoginRequest;
import com.gw.api.dto.auth.LoginResponse;
import com.gw.api.dto.auth.OtpActivateRequest;
import com.gw.api.dto.auth.OtpDisableRequest;
import com.gw.api.dto.auth.OtpSetupResponse;
import com.gw.api.dto.auth.OtpStatusResponse;
import com.gw.api.dto.auth.OtpVerifyRequest;
import com.gw.api.dto.auth.RefreshRequest;
import com.gw.api.dto.auth.TokenResponse;
import com.gw.api.service.auth.AuthService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 로그인 요청을 처리한다.
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    // Google 로그인 인증 URL을 발급한다.
    @GetMapping("/google/login-url")
    public ApiResponse<GoogleAuthorizationUrlResponse> getGoogleLoginUrl(@RequestParam("redirect_uri") String redirectUri) {
        return ApiResponse.ok(authService.getGoogleLoginAuthorizationUrl(redirectUri));
    }

    // Google 로그인 콜백 코드를 검증하고 로그인 요청을 처리한다.
    @PostMapping("/google/login")
    public ApiResponse<LoginResponse> loginWithGoogle(@Valid @RequestBody GoogleCodeRequest request) {
        return ApiResponse.ok(authService.loginWithGoogle(request.code(), request.redirectUri()));
    }

    // 로그인 사용자의 Google 계정 연동 인증 URL을 발급한다.
    @GetMapping("/google/link-url")
    public ApiResponse<GoogleAuthorizationUrlResponse> getGoogleLinkUrl(
            Principal principal,
            @RequestParam("redirect_uri") String redirectUri
    ) {
        getLoginId(principal);
        return ApiResponse.ok(authService.getGoogleLinkAuthorizationUrl(redirectUri));
    }

    // 로그인 사용자의 Google 계정 연동 콜백 코드를 처리한다.
    @PostMapping("/google/link")
    public ApiResponse<GoogleLinkStatusResponse> linkGoogleAccount(
            Principal principal,
            @Valid @RequestBody GoogleCodeRequest request
    ) {
        return ApiResponse.ok(authService.linkGoogleAccount(getLoginId(principal), request.code(), request.redirectUri()));
    }

    // 로그인 사용자의 Google 계정 연동 상태를 조회한다.
    @GetMapping("/google/link/status")
    public ApiResponse<GoogleLinkStatusResponse> getGoogleLinkStatus(Principal principal) {
        return ApiResponse.ok(authService.getGoogleLinkStatus(getLoginId(principal)));
    }

    // 로그인 사용자의 Google 계정 연동을 해제한다.
    @DeleteMapping("/google/link")
    public ApiResponse<Void> unlinkGoogleAccount(Principal principal) {
        authService.unlinkGoogleAccount(getLoginId(principal));
        return ApiResponse.ok();
    }

    // 로그인 사용자의 로그아웃 요청을 처리한다.
    @PostMapping("/logout")
    public ApiResponse<Void> logout(Principal principal, @Valid @RequestBody RefreshRequest request) {
        authService.logout(getLoginId(principal), request.refreshToken());
        return ApiResponse.ok();
    }

    // 리프레시 토큰으로 액세스 토큰을 재발급한다.
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    // 로그인 사용자의 OTP 설정 정보를 생성한다.
    @PostMapping("/otp/setup")
    public ApiResponse<OtpSetupResponse> setupOtp(Principal principal) {
        return ApiResponse.ok(authService.otpSetup(getLoginId(principal)));
    }

    // 로그인 사용자의 OTP를 활성화한다.
    @PostMapping("/otp/activate")
    public ApiResponse<Void> activateOtp(Principal principal, @Valid @RequestBody OtpActivateRequest request) {
        authService.otpActivate(getLoginId(principal), request.otpCode());
        return ApiResponse.ok();
    }

    // OTP 추가 인증 요청을 처리한다.
    @PostMapping("/otp/verify")
    public ApiResponse<TokenResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ApiResponse.ok(authService.otpVerify(request.otpTempToken(), request.otpCode()));
    }

    // 로그인 사용자의 OTP를 비활성화한다.
    @PostMapping("/otp/disable")
    public ApiResponse<Void> disableOtp(Principal principal, @Valid @RequestBody OtpDisableRequest request) {
        authService.otpDisable(getLoginId(principal), request.otpCode());
        return ApiResponse.ok();
    }

    // 로그인 사용자의 OTP 활성화 상태를 조회한다.
    @GetMapping("/otp/status")
    public ApiResponse<OtpStatusResponse> getOtpStatus(Principal principal) {
        return ApiResponse.ok(authService.otpStatus(getLoginId(principal)));
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
