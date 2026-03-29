package com.gw.api.dto.work;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AdminDailyReportResponse(
        String uuid,
        String memberUuid,
        String loginId,
        String nickname,
        LocalDate reportDate,
        String content,
        String status,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
