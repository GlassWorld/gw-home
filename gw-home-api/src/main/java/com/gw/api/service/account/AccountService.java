package com.gw.api.service.account;

import com.gw.api.convert.account.AccountConvert;
import com.gw.api.dto.account.AccountResponse;
import com.gw.api.dto.account.ChangePasswordRequest;
import com.gw.api.dto.account.SignUpRequest;
import com.gw.api.dto.account.UpdateAccountRequest;
import com.gw.api.service.profile.ProfileService;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.RolePolicy;
import com.gw.share.vo.account.AcctVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class AccountService {

    private final AccountMapper accountMapper;
    private final AccountLookupService accountLookupService;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;

    public AccountService(
            AccountMapper accountMapper,
            AccountLookupService accountLookupService,
            PasswordEncoder passwordEncoder,
            ProfileService profileService
    ) {
        this.accountMapper = accountMapper;
        this.accountLookupService = accountLookupService;
        this.passwordEncoder = passwordEncoder;
        this.profileService = profileService;
    }

    public AccountResponse signUp(SignUpRequest request) {
        log.info("signUp 시작 - loginId: {}", request.loginId());
        validateDuplicate(request.loginId(), request.email());

        AcctVo account = AcctVo.builder()
                .lgnId(request.loginId())
                .pwd(passwordEncoder.encode(request.password()))
                .email(request.email())
                .role(RolePolicy.USER)
                .createdBy(request.loginId())
                .build();

        accountMapper.insertAccount(account);
        AcctVo savedAccount = accountMapper.selectAccountByLoginId(request.loginId());
        profileService.createDefaultProfile(savedAccount);

        log.info("signUp 완료");
        return AccountConvert.toResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getMyAccount(String loginId) {
        log.info("getMyAccount 시작 - loginId: {}", loginId);
        AccountResponse response = AccountConvert.toResponse(getAccountByLoginId(loginId));
        log.info("getMyAccount 완료");
        return response;
    }

    public AccountResponse updateMyAccount(String loginId, UpdateAccountRequest request) {
        log.info("updateMyAccount 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);

        if (!account.getEmail().equals(request.email()) && accountMapper.existsByEmail(request.email())) {
            log.error("updateMyAccount 실패 - 원인: 이미 사용 중인 이메일입니다. email={}", request.email());
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 이메일입니다.");
        }

        account.setEmail(request.email());
        account.setUpdatedBy(loginId);
        accountMapper.updateAccount(account);

        log.info("updateMyAccount 완료");
        return AccountConvert.toResponse(getAccountByLoginId(loginId));
    }

    public void deleteMyAccount(String loginId) {
        log.info("deleteMyAccount 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        int updatedCount = accountMapper.deleteAccount(account.getUuid());

        if (updatedCount == 0) {
            log.error("deleteMyAccount 실패 - 원인: 계정을 찾을 수 없습니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        log.info("deleteMyAccount 완료");
    }

    public void changePassword(String loginId, ChangePasswordRequest request) {
        log.info("changePassword 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);

        if (!passwordEncoder.matches(request.currentPassword(), account.getPwd())) {
            log.error("changePassword 실패 - 원인: 현재 비밀번호가 일치하지 않습니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다.");
        }

        if (passwordEncoder.matches(request.newPassword(), account.getPwd())) {
            log.error("changePassword 실패 - 원인: 현재 비밀번호와 동일합니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.DUPLICATE, "현재 비밀번호와 동일합니다.");
        }

        accountMapper.updatePassword(account.getIdx(), passwordEncoder.encode(request.newPassword()), loginId);
        log.info("changePassword 완료");
    }

    @Transactional(readOnly = true)
    public AcctVo getAccountByLoginId(String loginId) {
        return accountLookupService.getAccountByLoginId(loginId);
    }

    private void validateDuplicate(String loginId, String email) {
        if (accountMapper.existsByLoginId(loginId)) {
            log.error("validateDuplicate 실패 - 원인: 이미 사용 중인 로그인 아이디입니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 로그인 아이디입니다.");
        }

        if (accountMapper.existsByEmail(email)) {
            log.error("validateDuplicate 실패 - 원인: 이미 사용 중인 이메일입니다. email={}", email);
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 이메일입니다.");
        }
    }
}
