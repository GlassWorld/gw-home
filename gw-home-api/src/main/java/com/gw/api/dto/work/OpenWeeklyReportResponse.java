package com.gw.api.dto.work;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record OpenWeeklyReportResponse(
        String uuid,
        String memberUuid,
        String loginId,
        String nickname,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        String title,
        String content,
        String openYn,
        OffsetDateTime publishedAt,
        String generationType,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
