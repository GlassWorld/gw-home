package com.gw.api.account.service;

import com.gw.api.account.dto.AccountDto;
import com.gw.api.account.dto.AccountResponse;
import com.gw.api.account.dto.SignUpRequest;
import com.gw.api.account.dto.UpdateAccountRequest;
import com.gw.api.account.mapper.AccountMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AccountService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    public AccountResponse signUp(SignUpRequest request) {
        validateDuplicate(request.loginId(), request.email());

        AccountDto account = new AccountDto();
        account.setLoginId(request.loginId());
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setEmail(request.email());
        account.setRole("USER");
        account.setCreatedBy(request.loginId());

        accountMapper.insertAccount(account);
        AccountDto savedAccount = accountMapper.selectAccountByLoginId(request.loginId());

        return toResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getMyAccount(String loginId) {
        return toResponse(getAccountByLoginId(loginId));
    }

    public AccountResponse updateMyAccount(String loginId, UpdateAccountRequest request) {
        AccountDto account = getAccountByLoginId(loginId);

        if (!account.getEmail().equals(request.email()) && accountMapper.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 이메일입니다.");
        }

        account.setEmail(request.email());
        account.setUpdatedBy(loginId);
        accountMapper.updateAccount(account);

        return toResponse(getAccountByLoginId(loginId));
    }

    public void deleteMyAccount(String loginId) {
        AccountDto account = getAccountByLoginId(loginId);
        int updatedCount = accountMapper.deleteAccount(account.getMemberAccountUuid());

        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountByLoginId(String loginId) {
        AccountDto account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        return account;
    }

    private void validateDuplicate(String loginId, String email) {
        if (accountMapper.existsByLoginId(loginId)) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 로그인 아이디입니다.");
        }

        if (accountMapper.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 이메일입니다.");
        }
    }

    private AccountResponse toResponse(AccountDto account) {
        return new AccountResponse(
                account.getMemberAccountUuid(),
                account.getLoginId(),
                account.getEmail(),
                account.getRole(),
                account.getCreatedAt()
        );
    }
}
