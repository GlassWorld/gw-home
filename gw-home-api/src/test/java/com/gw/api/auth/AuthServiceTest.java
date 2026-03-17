package com.gw.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gw.api.dto.auth.LoginRequest;
import com.gw.api.dto.auth.TokenResponse;
import com.gw.api.jwt.JwtProvider;
import com.gw.api.service.auth.AuthService;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.auth.AuthMapper;
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

    private final JwtProvider jwtProvider = new JwtProvider(
            "Z3ctaG9tZS1kZWZhdWx0LXNlY3JldC1rZXktZm9yLWRldmVsb3BtZW50LWFuZC10ZXN0aW5nLTEyMw==",
            1800,
            604800
    );

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authMapper, accountMapper, jwtProvider, passwordEncoder);
    }

    @Test
    void loginReturnsTokensAndStoresRefreshToken() {
        when(accountMapper.selectAccountByLoginId("tester_01")).thenReturn(createAccountVo());

        TokenResponse response = authService.login(new LoginRequest("tester_01", "password1234"));

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertTrue(response.expiresIn() > 0);
        verify(authMapper).insertRefreshToken(any());
    }

    private AcctVo createAccountVo() {
        return AcctVo.builder()
                .idx(1L)
                .uuid("account-uuid")
                .lgnId("tester_01")
                .pwd(passwordEncoder.encode("password1234"))
                .email("tester@example.com")
                .role("USER")
                .createdAt(OffsetDateTime.parse("2026-03-17T21:00:00+09:00"))
                .build();
    }
}
