package com.gw.api.auth;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gw.api.dto.admin.AdminSummaryResponse;
import com.gw.api.dto.account.AccountResponse;
import com.gw.api.service.account.AccountService;
import com.gw.api.service.admin.AdminService;
import com.gw.api.jwt.JwtProvider;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockBean
    private AccountService accountService;

    @MockBean
    private AdminService adminService;

    @Test
    void accessTokenCanCallProtectedEndpoint() throws Exception {
        when(accountService.getMyAccount("tester_01")).thenReturn(
                new AccountResponse(
                        "account-uuid",
                        "tester_01",
                        "tester@example.com",
                        "USER",
                        OffsetDateTime.parse("2026-03-17T21:00:00+09:00")
                )
        );

        String accessToken = jwtProvider.generateAccessToken("tester_01", "USER");

        mockMvc.perform(
                        get("/api/v1/accounts/me")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.login_id").value("tester_01"))
                .andExpect(jsonPath("$.data.email").value("tester@example.com"));
    }

    @Test
    void adminAccessTokenCanCallAdminEndpoint() throws Exception {
        when(adminService.getSummary()).thenReturn(
                new AdminSummaryResponse(10L, 8L, 15L, 20L, 5L)
        );

        String accessToken = jwtProvider.generateAccessToken("admin", "ADMIN");

        mockMvc.perform(
                        get("/api/v1/admin/summary")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total_member_count").value(10))
                .andExpect(jsonPath("$.data.active_member_count").value(8));
    }

    @Test
    void userAccessTokenCannotCallAdminEndpoint() throws Exception {
        String accessToken = jwtProvider.generateAccessToken("tester_01", "USER");

        mockMvc.perform(
                        get("/api/v1/admin/summary")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isForbidden());
    }
}
