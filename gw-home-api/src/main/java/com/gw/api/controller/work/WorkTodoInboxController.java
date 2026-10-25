package com.gw.api.controller.work;

import com.gw.api.dto.work.WorkTodoListRequest;
import com.gw.api.dto.work.WorkTodoListResponse;
import com.gw.api.dto.work.WorkTodoInboxSummaryResponse;
import com.gw.api.service.work.WorkTodoInboxService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import com.gw.share.common.response.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/work-todos")
public class WorkTodoInboxController {
    private final WorkTodoInboxService workTodoInboxService;

    // 인증 사용자의 여러 업무에 걸친 내 할 일 목록을 요청한다.
    @GetMapping
    public ApiResponse<PageResponse<WorkTodoListResponse>> getMyWorkTodos(Principal principal, @Valid @ModelAttribute WorkTodoListRequest request) {
        return ApiResponse.ok(workTodoInboxService.getMyWorkTodos(loginId(principal), request));
    }

    // 인증 사용자의 내 할 일 분류별 요약을 요청한다.
    @GetMapping("/summary")
    public ApiResponse<WorkTodoInboxSummaryResponse> getMyWorkTodoSummary(Principal principal, @Valid @ModelAttribute WorkTodoListRequest request) {
        return ApiResponse.ok(workTodoInboxService.getMyWorkTodoSummary(loginId(principal), request));
    }

    // 인증되지 않은 요청의 사용자 조회를 차단한다.
    private String loginId(Principal principal) {
        if (principal == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return principal.getName();
    }
}
