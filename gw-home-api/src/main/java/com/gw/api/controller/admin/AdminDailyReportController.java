package com.gw.api.controller.admin;

import com.gw.api.dto.work.AdminDailyReportMissingRequest;
import com.gw.api.dto.work.AdminDailyReportMissingResponse;
import com.gw.api.dto.work.AdminDailyReportResponse;
import com.gw.api.dto.work.DailyReportListRequest;
import com.gw.api.service.work.DailyReportService;
import com.gw.share.common.response.ApiResponse;
import com.gw.share.common.response.PageResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/daily-reports")
public class AdminDailyReportController {

    private final DailyReportService dailyReportService;

    public AdminDailyReportController(DailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    // 관리자용 일일보고 목록을 조회한다.
    @GetMapping
    public ApiResponse<PageResponse<AdminDailyReportResponse>> getAdminDailyReports(DailyReportListRequest request) {
        return ApiResponse.ok(dailyReportService.getAdminDailyReports(request));
    }

    // 관리자용 누락 일일보고 현황을 조회한다.
    @GetMapping("/missing")
    public ApiResponse<List<AdminDailyReportMissingResponse>> getAdminMissingDailyReports(
            AdminDailyReportMissingRequest request
    ) {
        return ApiResponse.ok(dailyReportService.getAdminMissingDailyReports(request));
    }
}
