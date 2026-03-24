package com.gw.api.service.auth;

import com.gw.api.dto.auth.LoginRequest;
import com.gw.api.dto.auth.TokenResponse;
import com.gw.api.jwt.JwtProvider;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.auth.AuthMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
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

    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    private final AuthMapper authMapper;
    private final AccountMapper accountMapper;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthMapper authMapper,
            AccountMapper accountMapper,
            JwtProvider jwtProvider,
            PasswordEncoder passwordEncoder
    ) {
        this.authMapper = authMapper;
        this.accountMapper = accountMapper;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public TokenResponse login(LoginRequest request) {
        log.info("login 시작 - loginId: {}", request.loginId());
        AcctVo account = accountMapper.selectAccountByLoginId(request.loginId());

        if (account == null) {
            log.error("login 실패 - 원인: 계정을 찾을 수 없습니다. loginId={}", request.loginId());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다.");
        }

        if (account.isLckYn()) {
            log.error("login 실패 - 원인: 잠긴 계정입니다. loginId={}", request.loginId());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "계정이 잠금되었습니다. 관리자에게 문의하세요.");
        }

        if (!passwordEncoder.matches(request.password(), account.getPwd())) {
            accountMapper.incrementLoginFailCount(account.getIdx());

            int currentLoginFailCount = account.getLgnFailCnt() + 1;
            if (currentLoginFailCount >= MAX_LOGIN_FAIL_COUNT) {
                accountMapper.lockAccount(account.getIdx());
            }

            int remainingCount = Math.max(0, MAX_LOGIN_FAIL_COUNT - currentLoginFailCount);
            log.error("login 실패 - 원인: 비밀번호 불일치. loginId={}, 남은횟수={}", request.loginId(), remainingCount);
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "로그인 정보가 올바르지 않습니다. (잔여 " + remainingCount + "회)"
            );
        }

        accountMapper.resetLoginFailCount(account.getIdx());

        String accessToken = jwtProvider.generateAccessToken(account.getLgnId(), account.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(account.getLgnId());

        AuthRfshTknVo refreshTokenVo = AuthRfshTknVo.builder()
                .mbrAcctIdx(account.getIdx())
                .tknHash(hashToken(refreshToken))
                .exprAt(OffsetDateTime.ofInstant(jwtProvider.getRefreshTokenExpirationInstant(), ZoneOffset.UTC))
                .createdBy(account.getLgnId())
                .build();
        authMapper.insertRefreshToken(refreshTokenVo);

        log.info("login 완료");
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtProvider.getAccessTokenExpiresInSeconds());
    }

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
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtProvider.getAccessTokenExpiresInSeconds());
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
