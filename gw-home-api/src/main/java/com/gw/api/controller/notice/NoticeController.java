package com.gw.api.controller.notice;

import com.gw.api.dto.notice.NoticeDetailResponse;
import com.gw.api.dto.notice.NoticeListRequest;
import com.gw.api.dto.notice.NoticeSummaryResponse;
import com.gw.api.service.notice.NoticeService;
import com.gw.share.common.response.ApiResponse;
import com.gw.share.common.response.PageResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NoticeSummaryResponse>> getNotices(NoticeListRequest request) {
        return ApiResponse.ok(noticeService.getNoticeList(request));
    }

    @GetMapping("/dashboard")
    public ApiResponse<List<NoticeSummaryResponse>> getDashboardNotices(
            @RequestParam(required = false, defaultValue = "5") Integer limit
    ) {
        return ApiResponse.ok(noticeService.getDashboardNotices(limit));
    }

    @GetMapping("/{noticeUuid}")
    public ApiResponse<NoticeDetailResponse> getNotice(@PathVariable String noticeUuid) {
        return ApiResponse.ok(noticeService.getNotice(noticeUuid));
    }
}
