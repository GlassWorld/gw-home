package com.gw.api.dto.work;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DailyReportResponse(
        String uuid,
        LocalDate reportDate,
        String content,
        String status,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
