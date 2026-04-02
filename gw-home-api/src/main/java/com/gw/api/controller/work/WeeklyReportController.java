package com.gw.api.controller.work;

import com.gw.api.dto.work.CreateWeeklyReportRequest;
import com.gw.api.dto.work.UpdateWeeklyReportRequest;
import com.gw.api.dto.work.WeeklyReportAiDraftRequest;
import com.gw.api.dto.work.WeeklyReportAiDraftResponse;
import com.gw.api.dto.work.WeeklyReportDailySourceResponse;
import com.gw.api.dto.work.WeeklyReportResponse;
import com.gw.api.service.work.WeeklyReportService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weekly-reports")
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    public WeeklyReportController(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }

    // 로그인 사용자의 주간보고 목록을 조회한다.
    @GetMapping
    public ApiResponse<List<WeeklyReportResponse>> getWeeklyReports(Principal principal) {
        return ApiResponse.ok(weeklyReportService.getWeeklyReportList(getLoginId(principal)));
    }

    // 로그인 사용자의 주간보고 상세 정보를 조회한다.
    @GetMapping("/{uuid}")
    public ApiResponse<WeeklyReportResponse> getWeeklyReport(Principal principal, @PathVariable String uuid) {
        return ApiResponse.ok(weeklyReportService.getWeeklyReport(getLoginId(principal), uuid));
    }

    // 로그인 사용자의 주간보고 작성용 일일보고 원본을 조회한다.
    @GetMapping("/daily-sources")
    public ApiResponse<List<WeeklyReportDailySourceResponse>> getWeeklyDailySources(
            Principal principal,
            @RequestParam LocalDate weekStartDate,
            @RequestParam LocalDate weekEndDate
    ) {
        return ApiResponse.ok(weeklyReportService.getWeeklySourceDailyReports(getLoginId(principal), weekStartDate, weekEndDate));
    }

    // 로그인 사용자가 주간보고를 생성한다.
    @PostMapping
    public ApiResponse<WeeklyReportResponse> createWeeklyReport(
            Principal principal,
            @Valid @RequestBody CreateWeeklyReportRequest request
    ) {
        return ApiResponse.ok(weeklyReportService.createWeeklyReport(getLoginId(principal), request));
    }

    // 로그인 사용자가 주간보고를 수정한다.
    @PutMapping("/{uuid}")
    public ApiResponse<WeeklyReportResponse> updateWeeklyReport(
            Principal principal,
            @PathVariable String uuid,
            @Valid @RequestBody UpdateWeeklyReportRequest request
    ) {
        return ApiResponse.ok(weeklyReportService.updateWeeklyReport(getLoginId(principal), uuid, request));
    }

    // 로그인 사용자가 AI 주간보고 초안을 생성한다.
    @PostMapping("/ai-draft")
    public ApiResponse<WeeklyReportAiDraftResponse> generateWeeklyReportAiDraft(
            Principal principal,
            @Valid @RequestBody WeeklyReportAiDraftRequest request
    ) {
        return ApiResponse.ok(weeklyReportService.generateWeeklyReportDraft(getLoginId(principal), request));
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
