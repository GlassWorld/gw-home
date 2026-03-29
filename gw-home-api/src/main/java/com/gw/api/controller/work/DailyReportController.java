package com.gw.api.controller.work;

import com.gw.api.dto.work.CreateDailyReportRequest;
import com.gw.api.dto.work.DailyReportListRequest;
import com.gw.api.dto.work.DailyReportMissingRequest;
import com.gw.api.dto.work.DailyReportMissingResponse;
import com.gw.api.dto.work.DailyReportResponse;
import com.gw.api.dto.work.UpdateDailyReportRequest;
import com.gw.api.service.work.DailyReportService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import com.gw.share.common.response.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/daily-reports")
public class DailyReportController {

    private final DailyReportService dailyReportService;

    public DailyReportController(DailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    @GetMapping
    public ApiResponse<PageResponse<DailyReportResponse>> getDailyReports(Principal principal, DailyReportListRequest request) {
        return ApiResponse.ok(dailyReportService.getDailyReportList(getLoginId(principal), request));
    }

    @GetMapping("/{uuid}")
    public ApiResponse<DailyReportResponse> getDailyReport(Principal principal, @PathVariable String uuid) {
        return ApiResponse.ok(dailyReportService.getDailyReport(getLoginId(principal), uuid));
    }

    @PostMapping
    public ApiResponse<DailyReportResponse> createDailyReport(
            Principal principal,
            @Valid @RequestBody CreateDailyReportRequest request
    ) {
        return ApiResponse.ok(dailyReportService.createDailyReport(getLoginId(principal), request));
    }

    @PutMapping("/{uuid}")
    public ApiResponse<DailyReportResponse> updateDailyReport(
            Principal principal,
            @PathVariable String uuid,
            @Valid @RequestBody UpdateDailyReportRequest request
    ) {
        return ApiResponse.ok(dailyReportService.updateDailyReport(getLoginId(principal), uuid, request));
    }

    @GetMapping("/missing")
    public ApiResponse<List<DailyReportMissingResponse>> getMissingDailyReports(
            Principal principal,
            DailyReportMissingRequest request
    ) {
        return ApiResponse.ok(dailyReportService.getMissingDailyReports(getLoginId(principal), request));
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
