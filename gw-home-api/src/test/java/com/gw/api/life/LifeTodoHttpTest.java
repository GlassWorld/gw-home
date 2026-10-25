package com.gw.api.life;

import com.gw.api.dto.life.LifeTodoDtos.*;
import com.gw.api.dto.life.LifeTodoListRequest;
import com.gw.api.jwt.JwtProvider;
import com.gw.api.service.life.LifeTodoService;
import com.gw.share.common.exception.*;
import com.gw.share.common.policy.ProfilePolicy;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class LifeTodoHttpTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwt;
    @MockBean LifeTodoService service;
    private String token() { return "Bearer " + jwt.generateAccessToken("tester", "USER"); }

    @Test void allReadsAndWritesRequireAuthentication() throws Exception {
        for (String path : List.of("", "/summary", "/categories", "/todo")) {
            mvc.perform(get("/api/v1/life-todos" + path)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/life-todos").contentType("application/json").content("{\"title\":\"장보기\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/life-todos/todo")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void summaryUsesPrincipalAndSnakeCaseWithoutInternalKeys() throws Exception {
        when(service.getSummary(eq("tester"), any())).thenReturn(new Summary(5, 1, 1, 1, 1, 1, 20, LocalDate.parse("2026-10-25")));
        mvc.perform(get("/api/v1/life-todos/summary").header("Authorization", token()).param("mbrAcctIdx", "999").param("timeZone", "UTC"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.today_count").value(1))
                .andExpect(jsonPath("$.data.as_of_date").value("2026-10-25")).andExpect(jsonPath("$.data.mbr_acct_idx").doesNotExist());
        var capture = ArgumentCaptor.forClass(LifeTodoListRequest.class);
        verify(service).getSummary(eq("tester"), capture.capture());
        assertEquals("UTC", capture.getValue().getTimeZone());
    }

    @Test void createAcceptsTitleOnlyAndDoesNotAcceptOwnerFromBody() throws Exception {
        when(service.createTodo(eq("tester"), any(), anyString())).thenReturn(new Todo("external-uuid", "장보기", null, null, null,
                null, false, null, "UNSCHEDULED", null, null));
        mvc.perform(post("/api/v1/life-todos").header("Authorization", token()).contentType("application/json")
                .content("{\"title\":\"장보기\",\"mbr_acct_idx\":999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.todo_uuid").value("external-uuid"))
                .andExpect(jsonPath("$.data.idx").doesNotExist()).andExpect(jsonPath("$.data.mbr_acct_idx").doesNotExist());
        verify(service).createTodo(eq("tester"), eq(new SaveTodo("장보기", null, null, null)), eq("Asia/Seoul"));
    }

    @Test void blankTitleMalformedDateMissingCompletionAndOversizedPageAreBadRequests() throws Exception {
        for (String body : List.of("{\"title\":\" \"}", "{\"title\":\"장보기\",\"due_date\":\"invalid\"}")) {
            mvc.perform(post("/api/v1/life-todos").header("Authorization", token()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/v1/life-todos/todo/completion").header("Authorization", token()).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/life-todos").header("Authorization", token()).param("size", "101")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void categoryDeletionReturnsNullEnvelopeAndForeignResourcesReturn404() throws Exception {
        mvc.perform(delete("/api/v1/life-todos/categories/mine").header("Authorization", token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data").isEmpty());
        doThrow(new BusinessException(ErrorCode.NOT_FOUND)).when(service).deleteTodo("tester", "foreign");
        mvc.perform(delete("/api/v1/life-todos/foreign").header("Authorization", token())).andExpect(status().isNotFound());
        verify(service).deleteCategory("tester", "mine");
        assertTrue(ProfilePolicy.USER_NAVIGATION_MENU_PATHS.contains("/life/todos"));
    }
}
