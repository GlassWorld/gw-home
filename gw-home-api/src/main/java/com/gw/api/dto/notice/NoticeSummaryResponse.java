package com.gw.api.dto.notice;

import java.time.OffsetDateTime;

public record NoticeSummaryResponse(
        String noticeUuid,
        String title,
        int viewCount,
        String createdBy,
        OffsetDateTime createdAt
) {
}
