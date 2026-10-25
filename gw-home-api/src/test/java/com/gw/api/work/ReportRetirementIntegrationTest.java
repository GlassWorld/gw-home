package com.gw.api.work;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gw.api.jwt.JwtProvider;
import com.gw.api.service.work.WorkGitService;
import com.gw.api.service.work.WorkUnitService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReportRetirementIntegrationTest {
    private static final String WORK_UUID = "10000000-0000-0000-0000-000000000001";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @MockBean private WorkUnitService workUnitService;
    @MockBean private WorkGitService workGitService;

    @ParameterizedTest
    @CsvSource({
            "GET, /api/v1/daily-reports",
            "GET, /api/v1/daily-reports/10000000-0000-0000-0000-000000000001",
            "GET, /api/v1/daily-reports/missing",
            "POST, /api/v1/daily-reports",
            "PUT, /api/v1/daily-reports/10000000-0000-0000-0000-000000000001",
            "GET, /api/v1/weekly-reports",
            "GET, /api/v1/weekly-reports/10000000-0000-0000-0000-000000000001",
            "GET, /api/v1/weekly-reports/open",
            "GET, /api/v1/weekly-reports/open/members",
            "GET, /api/v1/weekly-reports/open/10000000-0000-0000-0000-000000000001",
            "GET, /api/v1/weekly-reports/daily-sources",
            "POST, /api/v1/weekly-reports",
            "PUT, /api/v1/weekly-reports/10000000-0000-0000-0000-000000000001",
            "POST, /api/v1/weekly-reports/ai-draft",
            "GET, /api/v1/admin/daily-reports",
            "GET, /api/v1/admin/daily-reports/missing"
    })
    void retiredRoutesReturnCommonNotFound(String method, String path) throws Exception {
        String role = path.contains("/admin/") ? "ADMIN" : "USER";
        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .header("Authorization", "Bearer " + jwtProvider.generateAccessToken("tester", role))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("리소스를 찾을 수 없습니다"));
    }

    @Test
    void removedAdminRouteStillRespectsRoleBoundary() throws Exception {
        mockMvc.perform(get("/api/v1/admin/daily-reports")
                        .header("Authorization", "Bearer " + jwtProvider.generateAccessToken("tester", "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserCannotAccessProtectedWork() throws Exception {
        mockMvc.perform(get("/api/v1/work-units")).andExpect(status().isForbidden());
    }

    @Test
    void workGitCommitEndpointRemainsAvailable() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 25);
        when(workUnitService.getWorkUnitGitCommits("tester", WORK_UUID, date)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/work-units/" + WORK_UUID + "/git-commits")
                        .param("reportDate", date.toString())
                        .header("Authorization", "Bearer " + jwtProvider.generateAccessToken("tester", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(workUnitService).getWorkUnitGitCommits("tester", WORK_UUID, date);
    }

    @Test
    void gitProjectConnectionTestStillDelegatesToGitService() throws Exception {
        mockMvc.perform(post("/api/v1/work-git/projects/" + WORK_UUID + "/connection-test")
                        .header("Authorization", "Bearer " + jwtProvider.generateAccessToken("tester", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(workGitService).testGitProjectConnection("tester", WORK_UUID);
    }
}
