package com.gw.api.dto.work;

public record DailyReportWorkUnitResponse(
        String workUnitUuid,
        String title,
        String category
) {
}
