package com.gw.api.dto.work;

import java.time.LocalDate;

public record DailyReportMissingRequest(
        LocalDate dateFrom,
        LocalDate dateTo
) {
}
