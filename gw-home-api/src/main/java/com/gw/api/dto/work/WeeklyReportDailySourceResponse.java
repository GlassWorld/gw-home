package com.gw.api.dto.work;

import java.time.LocalDate;

public record WeeklyReportDailySourceResponse(
        String uuid,
        LocalDate reportDate,
        String status,
        String content,
        String note
) {
}
