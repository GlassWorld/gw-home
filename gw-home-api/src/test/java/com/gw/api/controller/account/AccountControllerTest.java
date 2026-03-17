package com.gw.api.controller.account;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gw.api.dto.account.AccountResponse;
import com.gw.api.service.account.AccountService;
import com.gw.share.common.handler.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AccountControllerTest {

    private final AccountService accountService = Mockito.mock(AccountService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AccountController(accountService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void createAccountReturnsSuccessResponse() throws Exception {
        when(accountService.signUp(any())).thenReturn(
                new AccountResponse(
                        "account-uuid",
                        "tester_01",
                        "tester@example.com",
                        "USER",
                        OffsetDateTime.parse("2026-03-17T21:00:00+09:00")
                )
        );

        mockMvc.perform(
                        post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "loginId": "tester_01",
                                          "password": "password1234",
                                          "email": "tester@example.com"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberAccountUuid").value("account-uuid"))
                .andExpect(jsonPath("$.data.loginId").value("tester_01"))
                .andExpect(jsonPath("$.data.email").value("tester@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        verify(accountService).signUp(any());
    }
}
