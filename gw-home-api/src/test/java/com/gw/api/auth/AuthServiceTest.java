package com.gw.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gw.api.dto.auth.GoogleLinkStatusResponse;
import com.gw.api.dto.auth.GoogleTokenInfoResponse;
import com.gw.api.dto.auth.LoginRequest;
import com.gw.api.dto.auth.LoginResponse;
import com.gw.api.dto.auth.TokenResponse;
import com.gw.api.jwt.JwtProvider;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.auth.AuthService;
import com.gw.api.service.auth.GoogleOAuthClient;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.auth.AuthMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.util.OtpSecretEncryptor;
import com.gw.share.common.util.OtpTotpUtil;
import com.gw.share.vo.account.AcctVo;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthMapper authMapper;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private AccountLookupService accountLookupService;

    @Mock
    private OtpSecretEncryptor otpSecretEncryptor;

    @Mock
    private OtpTotpUtil otpTotpUtil;

    @Mock
    private GoogleOAuthClient googleOAuthClient;

    private final JwtProvider jwtProvider = new JwtProvider(
            "Z3ctaG9tZS1kZWZhdWx0LXNlY3JldC1rZXktZm9yLWRldmVsb3BtZW50LWFuZC10ZXN0aW5nLTEyMw==",
            1800,
            604800,
            300
    );

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authMapper,
                accountLookupService,
                accountMapper,
                jwtProvider,
                passwordEncoder,
                otpSecretEncryptor,
                otpTotpUtil,
                googleOAuthClient
        );
    }

    @Test
    void loginReturnsTokensAndStoresRefreshToken() {
        when(accountMapper.selectAccountByLoginId("tester_01")).thenReturn(createAccountVo(true, false));

        LoginResponse response = authService.login(new LoginRequest("tester_01", "password1234"));
        TokenResponse tokenResponse = response.tokenResponse();

        assertEquals("OTP_SETUP_REQUIRED", response.loginStatus());
        assertNotNull(tokenResponse);
        assertNotNull(tokenResponse.accessToken());
        assertNotNull(tokenResponse.refreshToken());
        assertEquals("Bearer", tokenResponse.tokenType());
        assertTrue(tokenResponse.expiresIn() > 0);
        verify(accountMapper).resetLoginFailCount(1L);
        verify(authMapper).insertRefreshToken(any());
    }

    @Test
    void adminLoginReturnsAccessTokenWithAdminRole() {
        when(accountMapper.selectAccountByLoginId("admin")).thenReturn(
                AcctVo.builder()
                        .idx(99L)
                        .uuid("admin-account-uuid")
                        .lgnId("admin")
                        .pwd(passwordEncoder.encode("admin!@34"))
                        .email("admin@gw-home.local")
                        .role("ADMIN")
                        .otpRequired(false)
                        .createdAt(OffsetDateTime.parse("2026-03-22T01:00:00+09:00"))
                        .build()
        );

        LoginResponse response = authService.login(new LoginRequest("admin", "admin!@34"));

        assertNotNull(response.tokenResponse());
        assertEquals("admin", jwtProvider.extractLoginId(response.tokenResponse().accessToken()));
        assertEquals("ADMIN", jwtProvider.extractRole(response.tokenResponse().accessToken()));
        verify(accountMapper).resetLoginFailCount(99L);
        verify(authMapper).insertRefreshToken(any());
    }

    @Test
    void loginFailureIncrementsCountAndReturnsRemainingAttempts() {
        when(accountMapper.selectAccountByLoginId("tester_01")).thenReturn(
                createAccountVo(2, false)
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(new LoginRequest("tester_01", "wrong-password"))
        );

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), exception.getMessage());
        verify(accountMapper).incrementLoginFailCount(1L);
        verify(accountMapper, never()).lockAccount(1L);
        verify(accountMapper, never()).resetLoginFailCount(1L);
    }

    @Test
    void lockedAccountReturnsAccountLocked() {
        when(accountMapper.selectAccountByLoginId("tester_01")).thenReturn(
                createAccountVo(0, true)
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(new LoginRequest("tester_01", "password1234"))
        );

        assertEquals(ErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());
        assertEquals(ErrorCode.ACCOUNT_LOCKED.getMessage(), exception.getMessage());
        verify(accountMapper, never()).incrementLoginFailCount(1L);
        verify(accountMapper, never()).resetLoginFailCount(1L);
    }

    @Test
    void fifthLoginFailureLocksAccount() {
        when(accountMapper.selectAccountByLoginId("tester_01")).thenReturn(
                createAccountVo(4, false)
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(new LoginRequest("tester_01", "wrong-password"))
        );

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), exception.getMessage());
        verify(accountMapper).incrementLoginFailCount(1L);
        verify(accountMapper).lockAccount(1L);
        verify(accountMapper, never()).resetLoginFailCount(1L);
    }

    private AcctVo createAccountVo() {
        return createAccountVo(0, false, true, false);
    }

    private AcctVo createAccountVo(int loginFailCount, boolean isLocked) {
        return createAccountVo(loginFailCount, isLocked, true, false);
    }

    private AcctVo createAccountVo(boolean otpRequired, boolean otpEnabled) {
        return createAccountVo(0, false, otpRequired, otpEnabled);
    }

    @Test
    void loginReturnsSuccessWhenOtpIsOptional() {
        when(accountMapper.selectAccountByLoginId("tester_01")).thenReturn(createAccountVo(false, false));

        LoginResponse response = authService.login(new LoginRequest("tester_01", "password1234"));

        assertEquals("SUCCESS", response.loginStatus());
        assertNotNull(response.tokenResponse());
        verify(accountMapper).resetLoginFailCount(1L);
        verify(authMapper).insertRefreshToken(any());
    }

    @Test
    void loginReturnsOtpRequiredWhenOtpPolicyAndSecretAreReady() {
        when(accountMapper.selectAccountByLoginId("tester_01")).thenReturn(createAccountVo(0, false, true, true));

        LoginResponse response = authService.login(new LoginRequest("tester_01", "password1234"));

        assertEquals("OTP_REQUIRED", response.loginStatus());
        assertNotNull(response.otpTempToken());
        verify(accountMapper).resetLoginFailCount(1L);
        verify(authMapper, never()).insertRefreshToken(any());
    }

    @Test
    void googleLoginReturnsSuccessWhenLinkedAccountExists() {
        GoogleTokenInfoResponse googleAccount = new GoogleTokenInfoResponse(
                "client-id",
                "google-sub-1",
                "tester@gmail.com",
                true
        );
        when(googleOAuthClient.fetchVerifiedGoogleAccount("code-1", "http://localhost:3000/auth/google/callback"))
                .thenReturn(googleAccount);
        when(accountMapper.selectAccountByGoogleSub("google-sub-1")).thenReturn(createAccountVo(false, false));

        LoginResponse response = authService.loginWithGoogle("code-1", "http://localhost:3000/auth/google/callback");

        assertEquals("SUCCESS", response.loginStatus());
        assertNotNull(response.tokenResponse());
        verify(accountMapper).resetLoginFailCount(1L);
        verify(authMapper).insertRefreshToken(any());
    }

    @Test
    void googleLoginSkipsOtpWhenAccountRequiresOtp() {
        GoogleTokenInfoResponse googleAccount = new GoogleTokenInfoResponse(
                "client-id",
                "google-sub-1",
                "tester@gmail.com",
                true
        );
        when(googleOAuthClient.fetchVerifiedGoogleAccount("code-1", "http://localhost:3000/auth/google/callback"))
                .thenReturn(googleAccount);
        when(accountMapper.selectAccountByGoogleSub("google-sub-1")).thenReturn(createAccountVo(0, false, true, true));

        LoginResponse response = authService.loginWithGoogle("code-1", "http://localhost:3000/auth/google/callback");

        assertEquals("SUCCESS", response.loginStatus());
        assertNotNull(response.tokenResponse());
        verify(accountMapper).resetLoginFailCount(1L);
        verify(authMapper).insertRefreshToken(any());
    }

    @Test
    void linkGoogleAccountStoresGoogleAccountWhenNotLinkedElsewhere() {
        GoogleTokenInfoResponse googleAccount = new GoogleTokenInfoResponse(
                "client-id",
                "google-sub-1",
                "tester@gmail.com",
                true
        );
        AcctVo account = createAccountVo(false, false);
        AcctVo linkedAccount = createAccountVo(false, false);
        linkedAccount.setGoogleSub("google-sub-1");
        linkedAccount.setGoogleEmail("tester@gmail.com");
        linkedAccount.setGoogleLinkedAt(OffsetDateTime.parse("2026-03-17T22:00:00+09:00"));

        when(accountLookupService.getAccountByLoginId("tester_01")).thenReturn(account, linkedAccount);
        when(googleOAuthClient.fetchVerifiedGoogleAccount("code-1", "http://localhost:3000/auth/google/callback"))
                .thenReturn(googleAccount);

        GoogleLinkStatusResponse response = authService.linkGoogleAccount(
                "tester_01",
                "code-1",
                "http://localhost:3000/auth/google/callback"
        );

        assertTrue(response.linked());
        assertEquals("tester@gmail.com", response.googleEmail());
        verify(accountMapper).updateGoogleLink(eq(1L), eq("google-sub-1"), eq("tester@gmail.com"), eq("tester_01"));
    }

    private AcctVo createAccountVo(int loginFailCount, boolean isLocked, boolean otpRequired, boolean otpEnabled) {
        return AcctVo.builder()
                .idx(1L)
                .uuid("account-uuid")
                .lgnId("tester_01")
                .pwd(passwordEncoder.encode("password1234"))
                .email("tester@example.com")
                .role("USER")
                .lgnFailCnt(loginFailCount)
                .lckYn(isLocked)
                .otpRequired(otpRequired)
                .otpEnabled(otpEnabled)
                .otpSecret(otpEnabled ? "encrypted-secret" : null)
                .lckAt(null)
                .createdAt(OffsetDateTime.parse("2026-03-17T21:00:00+09:00"))
                .build();
    }
}
