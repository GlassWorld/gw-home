package com.gw.api.service.account;

import com.gw.api.convert.account.AdminAccountConvert;
import com.gw.api.dto.account.AdminAccountDetailResponse;
import com.gw.api.dto.account.AdminAccountListResponse;
import com.gw.api.dto.account.AdminPasswordResetResponse;
import com.gw.api.dto.account.AdminCreateAccountRequest;
import com.gw.api.dto.account.UpdateOtpRequiredRequest;
import com.gw.api.dto.account.UpdateRoleRequest;
import com.gw.api.dto.account.UpdateStatusRequest;
import com.gw.api.service.profile.ProfileService;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.support.PageSortSupport;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.AdminAccountPolicy;
import com.gw.share.common.response.PageResponse;
import com.gw.share.vo.account.AcctVo;
import java.security.SecureRandom;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class AdminAccountService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountMapper accountMapper;
    private final AccountLookupService accountLookupService;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;

    public AdminAccountService(
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

    @Transactional(readOnly = true)
    public PageResponse<AdminAccountListResponse> getAccounts(
            String loginId,
            String role,
            String acctStat,
            Integer page,
            Integer size
    ) {
        log.info("getAccounts 시작 - loginId: {}, role: {}, acctStat: {}, page: {}, size: {}",
                loginId, role, acctStat, page, size);
        String normalizedLoginId = trimToNull(loginId);
        String normalizedRole = trimToNull(role);
        String normalizedAcctStat = trimToNull(acctStat);
        int normalizedPage = PageSortSupport.normalizePage(page);
        int normalizedSize = PageSortSupport.normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;

        List<AdminAccountListResponse> content = accountMapper.selectAllAccounts(
                        normalizedLoginId,
                        normalizedRole,
                        normalizedAcctStat,
                        offset,
                        normalizedSize
                ).stream()
                .map(AdminAccountConvert::toListResponse)
                .toList();
        long totalCount = accountMapper.countAllAccounts(normalizedLoginId, normalizedRole, normalizedAcctStat);
        int totalPages = (int) Math.ceil((double) totalCount / normalizedSize);

        log.info("getAccounts 완료 - 조회건수: {}", totalCount);
        return new PageResponse<>(content, normalizedPage, normalizedSize, totalCount, totalPages);
    }

    @Transactional(readOnly = true)
    // 관리자 계정 상세를 조회한다.
    public AdminAccountDetailResponse getAccount(String uuid) {
        log.info("getAccount 시작 - uuid: {}", uuid);
        AdminAccountDetailResponse response = AdminAccountConvert.toDetailResponse(getAccountByUuid(uuid));
        log.info("getAccount 완료 - uuid: {}", uuid);
        return response;
    }

    // 관리자 권한으로 계정을 생성한다.
    public AdminAccountDetailResponse createAccount(AdminCreateAccountRequest request, String adminLoginId) {
        log.info("createAccount 시작 - loginId: {}, role: {}, adminLoginId: {}",
                request.loginId(), request.role(), adminLoginId);
        String loginId = request.loginId().trim();
        String email = request.email().trim();
        validateDuplicate(loginId, email);

        AcctVo account = AcctVo.builder()
                .lgnId(loginId)
                .pwd(passwordEncoder.encode(request.password()))
                .email(email)
                .role(request.role())
                .acctStat("ACTIVE")
                .createdBy(adminLoginId)
                .build();

        accountMapper.insertAccount(account);
        AcctVo savedAccount = getAccountByLoginId(loginId);
        profileService.createDefaultProfile(savedAccount);
        log.info("createAccount 완료 - createdLoginId: {}", loginId);
        return AdminAccountConvert.toDetailResponse(savedAccount);
    }

    // 관리자 권한으로 계정 권한을 변경한다.
    public AdminAccountDetailResponse updateRole(String uuid, UpdateRoleRequest request, String adminLoginId) {
        log.info("updateRole 시작 - uuid: {}, role: {}, adminLoginId: {}", uuid, request.role(), adminLoginId);
        AcctVo account = getAccountByUuid(uuid);
        validateSelfAction(adminLoginId, account, "자기 자신의 권한은 변경할 수 없습니다.");

        int updatedCount = accountMapper.updateRole(uuid, request.role(), adminLoginId);
        if (updatedCount == 0) {
            log.error("updateRole 실패 - 원인: 계정을 찾을 수 없습니다. uuid={}", uuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        log.info("updateRole 완료 - uuid: {}, role: {}", uuid, request.role());
        return AdminAccountConvert.toDetailResponse(getAccountByUuid(uuid));
    }

    // 관리자 권한으로 계정 상태를 변경한다.
    public AdminAccountDetailResponse updateStatus(String uuid, UpdateStatusRequest request, String adminLoginId) {
        log.info("updateStatus 시작 - uuid: {}, status: {}, adminLoginId: {}", uuid, request.status(), adminLoginId);
        AcctVo account = getAccountByUuid(uuid);
        validateSelfAction(adminLoginId, account, "자기 자신의 상태는 변경할 수 없습니다.");

        int updatedCount = accountMapper.updateStatus(uuid, request.status(), adminLoginId);
        if (updatedCount == 0) {
            log.error("updateStatus 실패 - 원인: 계정을 찾을 수 없습니다. uuid={}", uuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        log.info("updateStatus 완료 - uuid: {}, status: {}", uuid, request.status());
        return AdminAccountConvert.toDetailResponse(getAccountByUuid(uuid));
    }

    // 관리자 권한으로 계정의 OTP 요구 여부를 변경한다.
    public AdminAccountDetailResponse updateOtpRequired(String uuid, UpdateOtpRequiredRequest request, String adminLoginId) {
        log.info("updateOtpRequired 시작 - uuid: {}, otpRequired: {}, adminLoginId: {}", uuid, request.otpRequired(), adminLoginId);
        AcctVo account = getAccountByUuid(uuid);
        validateSelfAction(adminLoginId, account, "자기 자신의 OTP 요구 여부는 변경할 수 없습니다.");

        int updatedCount = accountMapper.updateOtpRequired(uuid, request.otpRequired(), adminLoginId);
        if (updatedCount == 0) {
            log.error("updateOtpRequired 실패 - 원인: 계정을 찾을 수 없습니다. uuid={}", uuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        log.info("updateOtpRequired 완료 - uuid: {}, otpRequired: {}", uuid, request.otpRequired());
        return AdminAccountConvert.toDetailResponse(getAccountByUuid(uuid));
    }

    // 관리자 권한으로 계정을 삭제한다.
    public void deleteAccount(String uuid, String adminLoginId) {
        log.info("deleteAccount 시작 - uuid: {}, adminLoginId: {}", uuid, adminLoginId);
        AcctVo account = getAccountByUuid(uuid);
        validateSelfAction(adminLoginId, account, "자기 자신의 계정은 삭제할 수 없습니다.");

        int updatedCount = accountMapper.deleteAccount(uuid);
        if (updatedCount == 0) {
            log.error("deleteAccount 실패 - 원인: 계정을 찾을 수 없습니다. uuid={}", uuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        log.info("deleteAccount 완료 - uuid: {}", uuid);
    }

    // 관리자 권한으로 잠긴 계정을 해제한다.
    public AdminAccountDetailResponse unlockAccount(String uuid, String adminLoginId) {
        log.info("unlockAccount 시작 - uuid: {}, adminLoginId: {}", uuid, adminLoginId);
        int updatedCount = accountMapper.unlockAccountByUuid(uuid, adminLoginId);

        if (updatedCount == 0) {
            log.error("unlockAccount 실패 - 원인: 계정을 찾을 수 없습니다. uuid={}", uuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        log.info("unlockAccount 완료 - uuid: {}", uuid);
        return AdminAccountConvert.toDetailResponse(getAccountByUuid(uuid));
    }

    // 관리자 권한으로 OTP 실패 횟수를 초기화한다.
    public AdminAccountDetailResponse resetOtpFailure(String uuid, String adminLoginId) {
        log.info("resetOtpFailure 시작 - uuid: {}, adminLoginId: {}", uuid, adminLoginId);
        int updatedCount = accountMapper.resetOtpFailureByUuid(uuid, adminLoginId);

        if (updatedCount == 0) {
            log.error("resetOtpFailure 실패 - 원인: 계정을 찾을 수 없습니다. uuid={}", uuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        log.info("resetOtpFailure 완료 - uuid: {}", uuid);
        return AdminAccountConvert.toDetailResponse(getAccountByUuid(uuid));
    }

    // 관리자 권한으로 OTP 설정을 초기화한다.
    public AdminAccountDetailResponse resetOtp(String uuid, String adminLoginId) {
        log.info("resetOtp 시작 - uuid: {}, adminLoginId: {}", uuid, adminLoginId);
        int updatedCount = accountMapper.resetOtpByUuid(uuid, adminLoginId);

        if (updatedCount == 0) {
            log.error("resetOtp 실패 - 원인: 계정을 찾을 수 없습니다. uuid={}", uuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        log.info("resetOtp 완료 - uuid: {}", uuid);
        return AdminAccountConvert.toDetailResponse(getAccountByUuid(uuid));
    }

    // 관리자 권한으로 임시 비밀번호를 발급한다.
    public AdminPasswordResetResponse resetPassword(String uuid, String adminLoginId) {
        log.info("resetPassword 시작 - uuid: {}, adminLoginId: {}", uuid, adminLoginId);
        getAccountByUuid(uuid);
        String temporaryPassword = generateTemporaryPassword();
        int updatedCount = accountMapper.updatePasswordByUuid(uuid, passwordEncoder.encode(temporaryPassword), adminLoginId);

        if (updatedCount == 0) {
            log.error("resetPassword 실패 - 원인: 계정을 찾을 수 없습니다. uuid={}", uuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        log.info("resetPassword 완료 - uuid: {}", uuid);
        return AdminAccountConvert.toPasswordResetResponse(temporaryPassword);
    }

    private AcctVo getAccountByUuid(String uuid) {
        return accountLookupService.getAccountByUuid(uuid);
    }

    private AcctVo getAccountByLoginId(String loginId) {
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

    private void validateSelfAction(String adminLoginId, AcctVo account, String message) {
        // 관리자 자기 자신에 대한 권한/상태/삭제 변경은 막는다.
        if (adminLoginId.equals(account.getLgnId())) {
            log.error("validateSelfAction 실패 - 원인: 자기 자신의 계정은 변경할 수 없습니다. adminLoginId={}", adminLoginId);
            throw new BusinessException(ErrorCode.FORBIDDEN, message);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder(AdminAccountPolicy.TEMP_PASSWORD_LENGTH);

        for (int index = 0; index < AdminAccountPolicy.TEMP_PASSWORD_LENGTH; index++) {
            int randomIndex = SECURE_RANDOM.nextInt(AdminAccountPolicy.TEMP_PASSWORD_CHARACTERS.length());
            builder.append(AdminAccountPolicy.TEMP_PASSWORD_CHARACTERS.charAt(randomIndex));
        }

        return builder.toString();
    }

}
