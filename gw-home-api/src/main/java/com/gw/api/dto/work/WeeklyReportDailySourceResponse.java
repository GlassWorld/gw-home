package com.gw.api.dto.work;

import java.time.LocalDate;
import java.util.List;

public record WeeklyReportDailySourceResponse(
        String uuid,
        LocalDate reportDate,
        List<DailyReportWorkUnitResponse> workUnits,
        String content,
        String note
) {
}
