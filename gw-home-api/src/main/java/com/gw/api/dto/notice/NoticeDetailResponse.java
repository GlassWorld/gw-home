package com.gw.api.dto.notice;

import java.time.OffsetDateTime;

public record NoticeDetailResponse(
        String noticeUuid,
        String title,
        String content,
        int viewCount,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
