package com.gw.api.work;

import com.gw.api.dto.work.WorkNotificationDtos.*;
import com.gw.api.jwt.JwtProvider;
import com.gw.api.service.work.WorkNotificationService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkNotificationHttpTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwt;
    @MockBean WorkNotificationService service;
    private String token() { return "Bearer "+jwt.generateAccessToken("tester","USER"); }
    @Test void anonymousRequestsRemainProtected() throws Exception {
        mvc.perform(get("/api/v1/work-notifications/settings")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/work-todos/11111111-1111-4111-8111-111111111111/reminder")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void settingsHaveSnakeCaseAndAreOwnedByAuthenticatedPrincipal() throws Exception {
        when(service.getSettings("tester")).thenReturn(new SettingsResponse("tester@example.test",true,"Asia/Seoul","09:00","DAILY",true,true,true,null));
        mvc.perform(get("/api/v1/work-notifications/settings").header("Authorization",token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.time_zone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.data.digest_enabled").value(true)).andExpect(jsonPath("$.data.mbr_acct_idx").doesNotExist())
                .andExpect(jsonPath("$.data.vrfy_hash").doesNotExist());
        mvc.perform(put("/api/v1/work-notifications/settings").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"tester@example.test","time_zone":"America/New_York","digest_time":"08:30","digest_cycle":"WEEKDAYS","digest_enabled":true,"reminder_enabled":false,"owner":"other"}
                    """)).andExpect(status().isOk());
        var request=ArgumentCaptor.forClass(SettingsRequest.class);
        verify(service).saveSettings(eq("tester"),request.capture());
        assertEquals("America/New_York",request.getValue().timeZone()); assertTrue(request.getValue().digestEnabled());
    }
    @Test void invalidClockAndMissingInstantAreRejectedBeforeService() throws Exception {
        mvc.perform(put("/api/v1/work-notifications/settings").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"tester@example.test","time_zone":"Asia/Seoul","digest_time":"25:61","digest_cycle":"DAILY"}
                    """)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/work-todos/todo/reminder").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void explicitOffsetReachesServiceAsTheSameInstant() throws Exception {
        mvc.perform(put("/api/v1/work-todos/todo/reminder").header("Authorization",token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"send_at\":\"2026-11-01T01:30:00-04:00\"}")).andExpect(status().isOk());
        var instant=ArgumentCaptor.forClass(OffsetDateTime.class); verify(service).saveReminder(eq("tester"),eq("todo"),instant.capture());
        assertEquals("2026-11-01T05:30:00Z",instant.getValue().toInstant().toString());
    }
}
