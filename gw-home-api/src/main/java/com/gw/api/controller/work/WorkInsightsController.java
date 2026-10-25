package com.gw.api.controller.work;

import com.gw.api.dto.work.WorkInsightsRequest;
import com.gw.api.dto.work.WorkInsightsResponse.*;
import com.gw.api.service.work.WorkInsightsService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/work-todos")
public class WorkInsightsController {
    private final WorkInsightsService service;

    // 인증 사용자의 일정과 계층을 조회한다.
    @GetMapping("/gantt")
    public ApiResponse<Gantt> getGantt(Principal principal, @ModelAttribute WorkInsightsRequest request) {
        return ApiResponse.ok(service.getGantt(loginId(principal), request));
    }

    // 인증 사용자의 기간별 완료 실적과 현재 현황을 조회한다.
    @GetMapping("/statistics")
    public ApiResponse<Statistics> getStatistics(Principal principal, @ModelAttribute WorkInsightsRequest request) {
        return ApiResponse.ok(service.getStatistics(loginId(principal), request));
    }

    private String loginId(Principal principal) {
        if (principal == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return principal.getName();
    }
}
