package com.gw.api.controller.account;

import com.gw.api.dto.account.AccountResponse;
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

    @PostMapping
    public ApiResponse<AccountResponse> createAccount(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.ok(accountService.signUp(request));
    }

    @GetMapping("/me")
    public ApiResponse<AccountResponse> getAccount(Principal principal) {
        return ApiResponse.ok(accountService.getMyAccount(getLoginId(principal)));
    }

    @PutMapping("/me")
    public ApiResponse<AccountResponse> updateAccount(
            Principal principal,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        return ApiResponse.ok(accountService.updateMyAccount(getLoginId(principal), request));
    }

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
