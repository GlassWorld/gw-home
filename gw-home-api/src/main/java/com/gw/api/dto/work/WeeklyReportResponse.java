package com.gw.api.dto.work;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record WeeklyReportResponse(
        String uuid,
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
