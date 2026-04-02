package com.gw.api.controller.auth;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
