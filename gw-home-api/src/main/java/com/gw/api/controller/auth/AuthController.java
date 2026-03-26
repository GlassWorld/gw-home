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

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(Principal principal, @Valid @RequestBody RefreshRequest request) {
        authService.logout(getLoginId(principal), request.refreshToken());
        return ApiResponse.ok();
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/otp/setup")
    public ApiResponse<OtpSetupResponse> setupOtp(Principal principal) {
        return ApiResponse.ok(authService.otpSetup(getLoginId(principal)));
    }

    @PostMapping("/otp/activate")
    public ApiResponse<Void> activateOtp(Principal principal, @Valid @RequestBody OtpActivateRequest request) {
        authService.otpActivate(getLoginId(principal), request.otpCode());
        return ApiResponse.ok();
    }

    @PostMapping("/otp/verify")
    public ApiResponse<TokenResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ApiResponse.ok(authService.otpVerify(request.otpTempToken(), request.otpCode()));
    }

    @PostMapping("/otp/disable")
    public ApiResponse<Void> disableOtp(Principal principal, @Valid @RequestBody OtpDisableRequest request) {
        authService.otpDisable(getLoginId(principal), request.otpCode());
        return ApiResponse.ok();
    }

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
