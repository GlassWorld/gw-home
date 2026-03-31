package com.gw.api.controller.admin;

import com.gw.api.dto.notice.CreateNoticeRequest;
import com.gw.api.dto.notice.NoticeDetailResponse;
import com.gw.api.dto.notice.NoticeListRequest;
import com.gw.api.dto.notice.NoticeSummaryResponse;
import com.gw.api.dto.notice.UpdateNoticeRequest;
import com.gw.api.service.notice.NoticeService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import com.gw.share.common.response.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/notices")
public class AdminNoticeController {

    private final NoticeService noticeService;

    public AdminNoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NoticeSummaryResponse>> getAdminNotices(NoticeListRequest request) {
        return ApiResponse.ok(noticeService.getAdminNoticeList(request));
    }

    @GetMapping("/{noticeUuid}")
    public ApiResponse<NoticeDetailResponse> getAdminNotice(@PathVariable String noticeUuid) {
        return ApiResponse.ok(noticeService.getAdminNotice(noticeUuid));
    }

    @PostMapping
    public ApiResponse<NoticeDetailResponse> createNotice(
            Principal principal,
            @Valid @RequestBody CreateNoticeRequest request
    ) {
        return ApiResponse.ok(noticeService.createNotice(getLoginId(principal), request));
    }

    @PutMapping("/{noticeUuid}")
    public ApiResponse<NoticeDetailResponse> updateNotice(
            Principal principal,
            @PathVariable String noticeUuid,
            @Valid @RequestBody UpdateNoticeRequest request
    ) {
        return ApiResponse.ok(noticeService.updateNotice(getLoginId(principal), noticeUuid, request));
    }

    @DeleteMapping("/{noticeUuid}")
    public ApiResponse<Void> deleteNotice(Principal principal, @PathVariable String noticeUuid) {
        noticeService.deleteNotice(getLoginId(principal), noticeUuid);
        return ApiResponse.ok();
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
