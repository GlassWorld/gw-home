package com.gw.api.dto.work;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DailyReportResponse(
        String uuid,
        LocalDate reportDate,
        List<DailyReportWorkUnitResponse> workUnits,
        String content,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
