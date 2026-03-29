package com.gw.api.dto.work;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminDailyReportResponse(
        String uuid,
        String memberUuid,
        String loginId,
        String nickname,
        LocalDate reportDate,
        List<DailyReportWorkUnitResponse> workUnits,
        String content,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
