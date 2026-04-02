package com.gw.api.controller.account;

import com.gw.api.dto.account.AccountResponse;
import com.gw.api.dto.account.ChangePasswordRequest;
import com.gw.api.dto.account.SignUpRequest;
import com.gw.api.dto.account.UpdateAccountRequest;
import com.gw.api.service.account.AccountService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // 회원가입 요청을 처리한다.
    @PostMapping
    public ApiResponse<AccountResponse> createAccount(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.ok(accountService.signUp(request));
    }

    // 로그인 사용자의 계정 정보를 조회한다.
    @GetMapping("/me")
    public ApiResponse<AccountResponse> getAccount(Principal principal) {
        return ApiResponse.ok(accountService.getMyAccount(getLoginId(principal)));
    }

    // 로그인 사용자의 계정 정보를 수정한다.
    @PutMapping("/me")
    public ApiResponse<AccountResponse> updateAccount(
            Principal principal,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        return ApiResponse.ok(accountService.updateMyAccount(getLoginId(principal), request));
    }

    // 로그인 사용자의 비밀번호를 변경한다.
    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        accountService.changePassword(getLoginId(principal), request);
        return ApiResponse.ok();
    }

    // 로그인 사용자의 계정을 삭제한다.
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount(Principal principal) {
        accountService.deleteMyAccount(getLoginId(principal));
        return ApiResponse.ok();
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
