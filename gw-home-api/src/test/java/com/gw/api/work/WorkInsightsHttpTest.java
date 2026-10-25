package com.gw.api.work;

import com.gw.api.dto.work.WorkInsightsRequest;
import com.gw.api.dto.work.WorkInsightsResponse.Gantt;
import com.gw.api.dto.work.WorkInsightsResponse.Period;
import com.gw.api.jwt.JwtProvider;
import com.gw.api.service.work.WorkInsightsService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class WorkInsightsHttpTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwt;
    @MockBean WorkInsightsService service;
    private String token() { return "Bearer " + jwt.generateAccessToken("tester","USER"); }
    @Test void bothEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/work-todos/gantt")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/work-todos/statistics")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void responseUsesSnakeCaseAndRequestOwnerComesOnlyFromPrincipal() throws Exception {
        var date = LocalDate.parse("2026-10-25");
        when(service.getGantt(eq("tester"),any())).thenReturn(new Gantt(new Period(date,date,"UTC",date,false,null),List.of(),List.of()));
        mvc.perform(get("/api/v1/work-todos/gantt").header("Authorization",token())
                .param("fromDate","2026-10-25").param("toDate","2026-10-25").param("timeZone","UTC").param("mbrAcctIdx","999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.period.from_date").value("2026-10-25"))
                .andExpect(jsonPath("$.data.period.time_zone").value("UTC")).andExpect(jsonPath("$.data.mbr_acct_idx").doesNotExist());
        var request = ArgumentCaptor.forClass(WorkInsightsRequest.class);
        verify(service).getGantt(eq("tester"),request.capture());
        assertEquals(date,request.getValue().getFromDate());
    }
    @Test void malformedDatesDoNotReachService() throws Exception {
        mvc.perform(get("/api/v1/work-todos/statistics").header("Authorization",token()).param("fromDate","not-a-date"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
