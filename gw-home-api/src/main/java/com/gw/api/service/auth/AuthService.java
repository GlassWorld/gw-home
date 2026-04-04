package com.gw.api.service.auth;

import com.gw.api.convert.auth.AuthConvert;
import com.gw.api.dto.auth.LoginRequest;
import com.gw.api.dto.auth.LoginResponse;
import com.gw.api.dto.auth.OtpSetupResponse;
import com.gw.api.dto.auth.OtpStatusResponse;
import com.gw.api.dto.auth.TokenResponse;
import com.gw.api.jwt.JwtProvider;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.auth.AuthMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.AuthPolicy;
import com.gw.share.common.util.OtpSecretEncryptor;
import com.gw.share.common.util.OtpTotpUtil;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.auth.AuthRfshTknVo;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class AuthService {

    private final AuthMapper authMapper;
    private final AccountLookupService accountLookupService;
    private final AccountMapper accountMapper;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final OtpSecretEncryptor otpSecretEncryptor;
    private final OtpTotpUtil otpTotpUtil;

    public AuthService(
            AuthMapper authMapper,
            AccountLookupService accountLookupService,
            AccountMapper accountMapper,
            JwtProvider jwtProvider,
            PasswordEncoder passwordEncoder,
            OtpSecretEncryptor otpSecretEncryptor,
            OtpTotpUtil otpTotpUtil
    ) {
        this.authMapper = authMapper;
        this.accountLookupService = accountLookupService;
        this.accountMapper = accountMapper;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.otpSecretEncryptor = otpSecretEncryptor;
        this.otpTotpUtil = otpTotpUtil;
    }

    // 로그인 요청을 검증하고 토큰 또는 OTP 진행 상태를 반환한다.
    public LoginResponse login(LoginRequest request) {
        log.info("login 시작 - loginId: {}", request.loginId());
        AcctVo account = accountMapper.selectAccountByLoginId(request.loginId());

        if (account == null) {
            log.error("login 실패 - 원인: 계정을 찾을 수 없습니다. loginId={}", request.loginId());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다.");
        }

        if ("INACTIVE".equals(account.getAcctStat())) {
            log.error("login 실패 - 원인: 비활성화된 계정입니다. loginId={}", request.loginId());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "비활성화된 계정입니다. 관리자에게 문의하세요.");
        }

        if (account.isLckYn()) {
            log.error("login 실패 - 원인: 잠긴 계정입니다. loginId={}", request.loginId());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "계정이 잠금되었습니다. 관리자에게 문의하세요.");
        }

        if (!passwordEncoder.matches(request.password(), account.getPwd())) {
            accountMapper.incrementLoginFailCount(account.getIdx());

            int currentLoginFailCount = account.getLgnFailCnt() + 1;
            if (currentLoginFailCount >= AuthPolicy.MAX_LOGIN_FAIL_COUNT) {
                accountMapper.lockAccount(account.getIdx());
            }

            int remainingCount = Math.max(0, AuthPolicy.MAX_LOGIN_FAIL_COUNT - currentLoginFailCount);
            log.error("login 실패 - 원인: 비밀번호 불일치. loginId={}, 남은횟수={}", request.loginId(), remainingCount);
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "로그인 정보가 올바르지 않습니다. (잔여 " + remainingCount + "회)"
            );
        }

        accountMapper.resetLoginFailCount(account.getIdx());

        if (account.isOtpEnabled() && account.getOtpSecret() != null && !account.getOtpSecret().isBlank()) {
            log.info("login 완료 - OTP 추가 인증 필요");
            return AuthConvert.toOtpRequiredLoginResponse(jwtProvider.generateOtpTempToken(account.getLgnId()));
        }

        if (!account.isOtpEnabled()) {
            log.info("login 완료 - OTP 설정 필요");
            return AuthConvert.toOtpSetupRequiredLoginResponse(issueTokenResponse(account));
        }

        log.info("login 완료");
        return AuthConvert.toSuccessLoginResponse(issueTokenResponse(account));
    }

    // 로그인 사용자의 리프레시 토큰을 만료 처리한다.
    public void logout(String loginId, String refreshToken) {
        log.info("logout 시작 - loginId: {}", loginId);
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
            log.error("logout 실패 - 원인: 계정을 찾을 수 없습니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        int updatedCount = authMapper.deleteRefreshToken(hashToken(refreshToken), account.getIdx());

        if (updatedCount == 0) {
            log.error("logout 실패 - 원인: 리프레시 토큰을 찾을 수 없습니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "리프레시 토큰을 찾을 수 없습니다.");
        }

        log.info("logout 완료");
    }

    @Transactional(readOnly = true)
    // 리프레시 토큰으로 액세스 토큰을 재발급한다.
    public TokenResponse refresh(String refreshToken) {
        log.info("refresh 시작");
        if (!jwtProvider.validate(refreshToken) || !"refresh".equals(jwtProvider.extractTokenType(refreshToken))) {
            log.error("refresh 실패 - 원인: 유효하지 않은 리프레시 토큰입니다.");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        AuthRfshTknVo savedRefreshToken = authMapper.selectActiveRefreshTokenByTokenHash(hashToken(refreshToken));

        if (savedRefreshToken == null || savedRefreshToken.getExprAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            log.error("refresh 실패 - 원인: 만료되었거나 존재하지 않는 리프레시 토큰입니다.");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "만료되었거나 존재하지 않는 리프레시 토큰입니다.");
        }

        AcctVo account = accountMapper.selectAccountByIdx(savedRefreshToken.getMbrAcctIdx());

        if (account == null) {
            log.error("refresh 실패 - 원인: 계정을 찾을 수 없습니다. accountIdx={}", savedRefreshToken.getMbrAcctIdx());
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        String accessToken = jwtProvider.generateAccessToken(account.getLgnId(), account.getRole());
        log.info("refresh 완료");
        return AuthConvert.toTokenResponse(accessToken, refreshToken, jwtProvider.getAccessTokenExpiresInSeconds());
    }

    // 로그인 사용자의 OTP 설정 정보를 생성한다.
    public OtpSetupResponse otpSetup(String loginId) {
        log.info("otpSetup 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        String secret = otpTotpUtil.generateSecret();
        String encryptedSecret = otpSecretEncryptor.encrypt(secret);

        accountMapper.updateOtpSecret(account.getIdx(), encryptedSecret);

        log.info("otpSetup 완료");
        return AuthConvert.toOtpSetupResponse(otpTotpUtil.buildOtpAuthUrl(account.getLgnId(), secret));
    }

    // 로그인 사용자의 OTP를 활성화한다.
    public void otpActivate(String loginId, String otpCode) {
        log.info("otpActivate 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        String secret = getDecryptedOtpSecret(account);

        if (!otpTotpUtil.verify(secret, otpCode)) {
            log.error("otpActivate 실패 - 원인: OTP 코드 검증 실패. loginId={}", loginId);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "OTP 코드가 올바르지 않습니다.");
        }

        accountMapper.enableOtp(account.getIdx());
        log.info("otpActivate 완료");
    }

    // OTP 추가 인증 코드를 검증하고 로그인 토큰을 발급한다.
    public TokenResponse otpVerify(String otpTempToken, String otpCode) {
        log.info("otpVerify 시작");
        if (!jwtProvider.isOtpTempToken(otpTempToken)) {
            log.error("otpVerify 실패 - 원인: 유효하지 않은 OTP 임시 토큰입니다.");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 OTP 임시 토큰입니다.");
        }

        AcctVo account = getAccountByLoginId(jwtProvider.extractLoginId(otpTempToken));

        if (isOtpLocked(account)) {
            log.error("otpVerify 실패 - 원인: OTP 인증이 일시 차단되었습니다. loginId={}", account.getLgnId());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "OTP 인증이 30분간 차단되었습니다.");
        }

        if (isOtpLockExpired(account)) {
            accountMapper.resetOtpFailCount(account.getIdx());
            account.setOtpFailCnt(0);
            account.setOtpLastFailedAt(null);
        }

        String secret = getDecryptedOtpSecret(account);
        if (!otpTotpUtil.verify(secret, otpCode)) {
            accountMapper.incrementOtpFailCount(account.getIdx());
            int currentOtpFailCount = account.getOtpFailCnt() + 1;
            int remainingCount = Math.max(0, AuthPolicy.MAX_OTP_FAIL_COUNT - currentOtpFailCount);
            log.error("otpVerify 실패 - 원인: OTP 코드 불일치. loginId={}, 남은횟수={}", account.getLgnId(), remainingCount);

            if (currentOtpFailCount >= AuthPolicy.MAX_OTP_FAIL_COUNT) {
                throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "OTP 인증이 30분간 차단되었습니다.");
            }

            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "OTP 코드가 올바르지 않습니다. (잔여 " + remainingCount + "회)"
            );
        }

        accountMapper.resetOtpFailCount(account.getIdx());
        log.info("otpVerify 완료");
        return issueTokenResponse(account);
    }

    // 로그인 사용자의 OTP를 비활성화한다.
    public void otpDisable(String loginId, String otpCode) {
        log.info("otpDisable 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        String secret = getDecryptedOtpSecret(account);

        if (!otpTotpUtil.verify(secret, otpCode)) {
            log.error("otpDisable 실패 - 원인: OTP 코드 검증 실패. loginId={}", loginId);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "OTP 코드가 올바르지 않습니다.");
        }

        accountMapper.disableOtp(account.getIdx());
        log.info("otpDisable 완료");
    }

    @Transactional(readOnly = true)
    // 로그인 사용자의 OTP 활성화 상태를 조회한다.
    public OtpStatusResponse otpStatus(String loginId) {
        log.info("otpStatus 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        log.info("otpStatus 완료");
        return AuthConvert.toOtpStatusResponse(account.isOtpEnabled());
    }

    private AcctVo getAccountByLoginId(String loginId) {
        return accountLookupService.getAccountByLoginId(loginId);
    }

    private String getDecryptedOtpSecret(AcctVo account) {
        if (account.getOtpSecret() == null || account.getOtpSecret().isBlank()) {
            log.error("getDecryptedOtpSecret 실패 - 원인: 설정된 OTP 시크릿이 없습니다. loginId={}", account.getLgnId());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "설정된 OTP 시크릿이 없습니다.");
        }

        return otpSecretEncryptor.decrypt(account.getOtpSecret());
    }

    private boolean isOtpLocked(AcctVo account) {
        return account.getOtpLastFailedAt() != null
                && account.getOtpFailCnt() >= AuthPolicy.MAX_OTP_FAIL_COUNT
                && account.getOtpLastFailedAt().plusMinutes(AuthPolicy.OTP_LOCK_MINUTES).isAfter(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private boolean isOtpLockExpired(AcctVo account) {
        return account.getOtpLastFailedAt() != null
                && account.getOtpFailCnt() >= AuthPolicy.MAX_OTP_FAIL_COUNT
                && !account.getOtpLastFailedAt().plusMinutes(AuthPolicy.OTP_LOCK_MINUTES).isAfter(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private TokenResponse issueTokenResponse(AcctVo account) {
        String accessToken = jwtProvider.generateAccessToken(account.getLgnId(), account.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(account.getLgnId());

        AuthRfshTknVo refreshTokenVo = AuthRfshTknVo.builder()
                .mbrAcctIdx(account.getIdx())
                .tknHash(hashToken(refreshToken))
                .exprAt(OffsetDateTime.ofInstant(jwtProvider.getRefreshTokenExpirationInstant(), ZoneOffset.UTC))
                .createdBy(account.getLgnId())
                .build();
        authMapper.insertRefreshToken(refreshTokenVo);

        return AuthConvert.toTokenResponse(accessToken, refreshToken, jwtProvider.getAccessTokenExpiresInSeconds());
    }

    private String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();

            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }

            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "토큰 해시 생성에 실패했습니다.");
        }
    }
}
