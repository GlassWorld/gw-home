package com.gw.api.service.account;

import com.gw.api.dto.account.AccountResponse;
import com.gw.api.dto.account.SignUpRequest;
import com.gw.api.dto.account.UpdateAccountRequest;
import com.gw.api.service.profile.ProfileService;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.account.AcctVo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;

    public AccountService(
            AccountMapper accountMapper,
            PasswordEncoder passwordEncoder,
            ProfileService profileService
    ) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
        this.profileService = profileService;
    }

    public AccountResponse signUp(SignUpRequest request) {
        validateDuplicate(request.loginId(), request.email());

        AcctVo account = AcctVo.builder()
                .lgnId(request.loginId())
                .pwd(passwordEncoder.encode(request.password()))
                .email(request.email())
                .role("USER")
                .createdBy(request.loginId())
                .build();

        accountMapper.insertAccount(account);
        AcctVo savedAccount = accountMapper.selectAccountByLoginId(request.loginId());
        profileService.createDefaultProfile(savedAccount);

        return toResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getMyAccount(String loginId) {
        return toResponse(getAccountByLoginId(loginId));
    }

    public AccountResponse updateMyAccount(String loginId, UpdateAccountRequest request) {
        AcctVo account = getAccountByLoginId(loginId);

        if (!account.getEmail().equals(request.email()) && accountMapper.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 이메일입니다.");
        }

        account.setEmail(request.email());
        account.setUpdatedBy(loginId);
        accountMapper.updateAccount(account);

        return toResponse(getAccountByLoginId(loginId));
    }

    public void deleteMyAccount(String loginId) {
        AcctVo account = getAccountByLoginId(loginId);
        int updatedCount = accountMapper.deleteAccount(account.getUuid());
        
        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public AcctVo getAccountByLoginId(String loginId) {
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

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

    private AccountResponse toResponse(AcctVo account) {
        return new AccountResponse(
                account.getUuid(),
                account.getLgnId(),
                account.getEmail(),
                account.getRole(),
                account.getCreatedAt()
        );
    }
}
