package com.gw.api.controller.admin;

import com.gw.api.dto.account.AdminAccountDetailResponse;
import com.gw.api.dto.account.AdminAccountListResponse;
import com.gw.api.dto.account.AdminPasswordResetResponse;
import com.gw.api.dto.account.AdminCreateAccountRequest;
import com.gw.api.dto.account.UpdateOtpRequiredRequest;
import com.gw.api.dto.account.UpdateRoleRequest;
import com.gw.api.dto.account.UpdateStatusRequest;
import com.gw.api.service.account.AdminAccountService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import com.gw.share.common.response.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    // 관리자용 회원 목록을 조회한다.
    @GetMapping
    public ApiResponse<PageResponse<AdminAccountListResponse>> getAccounts(
            @RequestParam(required = false) String loginId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false, name = "status") String acctStat,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(adminAccountService.getAccounts(loginId, role, acctStat, page, size));
    }

    // 관리자용 회원 상세 정보를 조회한다.
    @GetMapping("/{uuid}")
    public ApiResponse<AdminAccountDetailResponse> getAccount(@PathVariable String uuid) {
        return ApiResponse.ok(adminAccountService.getAccount(uuid));
    }

    // 관리자가 회원 계정을 생성한다.
    @PostMapping
    public ApiResponse<AdminAccountDetailResponse> createAccount(
            Principal principal,
            @Valid @RequestBody AdminCreateAccountRequest request
    ) {
        return ApiResponse.ok(adminAccountService.createAccount(request, getLoginId(principal)));
    }

    // 관리자가 회원 권한을 변경한다.
    @PutMapping("/{uuid}/role")
    public ApiResponse<AdminAccountDetailResponse> updateRole(
            Principal principal,
            @PathVariable String uuid,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ApiResponse.ok(adminAccountService.updateRole(uuid, request, getLoginId(principal)));
    }

    // 관리자가 회원 상태를 변경한다.
    @PutMapping("/{uuid}/status")
    public ApiResponse<AdminAccountDetailResponse> updateStatus(
            Principal principal,
            @PathVariable String uuid,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ApiResponse.ok(adminAccountService.updateStatus(uuid, request, getLoginId(principal)));
    }

    // 관리자가 회원의 OTP 요구 여부를 변경한다.
    @PutMapping("/{uuid}/otp-required")
    public ApiResponse<AdminAccountDetailResponse> updateOtpRequired(
            Principal principal,
            @PathVariable String uuid,
            @Valid @RequestBody UpdateOtpRequiredRequest request
    ) {
        return ApiResponse.ok(adminAccountService.updateOtpRequired(uuid, request, getLoginId(principal)));
    }

    // 관리자가 회원 계정을 삭제한다.
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deleteAccount(Principal principal, @PathVariable String uuid) {
        adminAccountService.deleteAccount(uuid, getLoginId(principal));
        return ApiResponse.ok();
    }

    // 관리자가 회원 계정 잠금을 해제한다.
    @PutMapping("/{uuid}/unlock")
    public ApiResponse<AdminAccountDetailResponse> unlockAccount(Principal principal, @PathVariable String uuid) {
        return ApiResponse.ok(adminAccountService.unlockAccount(uuid, getLoginId(principal)));
    }

    // 관리자가 회원의 OTP 실패 횟수를 초기화한다.
    @PutMapping("/{uuid}/otp-failure/reset")
    public ApiResponse<AdminAccountDetailResponse> resetOtpFailure(Principal principal, @PathVariable String uuid) {
        return ApiResponse.ok(adminAccountService.resetOtpFailure(uuid, getLoginId(principal)));
    }

    // 관리자가 회원의 OTP 설정을 초기화한다.
    @PutMapping("/{uuid}/otp/reset")
    public ApiResponse<AdminAccountDetailResponse> resetOtp(Principal principal, @PathVariable String uuid) {
        return ApiResponse.ok(adminAccountService.resetOtp(uuid, getLoginId(principal)));
    }

    // 관리자가 회원 비밀번호를 초기화한다.
    @PutMapping("/{uuid}/password/reset")
    public ApiResponse<AdminPasswordResetResponse> resetPassword(Principal principal, @PathVariable String uuid) {
        return ApiResponse.ok(adminAccountService.resetPassword(uuid, getLoginId(principal)));
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
