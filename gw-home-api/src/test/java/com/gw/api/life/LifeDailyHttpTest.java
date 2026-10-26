package com.gw.api.life;

import com.gw.api.dto.life.LifeDailyDtos.*;
import com.gw.api.jwt.JwtProvider;
import com.gw.api.service.life.LifeDailyService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class LifeDailyHttpTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwt;
    @MockBean LifeDailyService service;
    private String token() { return "Bearer " + jwt.generateAccessToken("tester", "USER"); }

    @Test void allOperationsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/life-daily").param("month", "2024-02")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/life-daily")).andExpect(status().isForbidden());
        for (String path : List.of("/item", "/item/end", "/item/checks/2024-02-29")) {
            mvc.perform(put("/api/v1/life-daily" + path)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test void monthAndCreateUsePrincipalAndExternalIdentifiers() throws Exception {
        var date = LocalDate.of(2024, 2, 29);
        var item = new Item("uuid", "물", date, null);
        when(service.getMonth("tester", "2024-02", "UTC")).thenReturn(new Month("2024-02", date, "UTC", List.of(item), List.of(new Check("uuid", date))));
        mvc.perform(get("/api/v1/life-daily").header("Authorization", token()).param("month", "2024-02").param("timeZone", "UTC").param("owner", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].item_uuid").value("uuid"))
                .andExpect(jsonPath("$.data.items[0].mbr_acct_idx").doesNotExist()).andExpect(jsonPath("$.data.checks[0].date").value("2024-02-29"));
        when(service.createItem(eq("tester"), any(), anyString())).thenReturn(item);
        mvc.perform(post("/api/v1/life-daily").header("Authorization", token()).contentType("application/json").content("{\"title\":\"물\",\"owner\":999}"))
                .andExpect(status().isOk());
        verify(service).createItem("tester", new SaveItem("물"), "Asia/Seoul");
    }

    @Test void invalidJsonDatesAndMissingCompletionAreBadRequests() throws Exception {
        mvc.perform(post("/api/v1/life-daily").header("Authorization", token()).contentType("application/json").content("{\"title\":\" \"}"))
                .andExpect(status().isBadRequest());
        for (String date : List.of("bad", "2024-02-30")) {
            mvc.perform(put("/api/v1/life-daily/item/checks/" + date).header("Authorization", token()).contentType("application/json").content("{\"completed\":true}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/v1/life-daily/item/checks/2024-02-29").header("Authorization", token()).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
