package com.gw.api.dto.work;

import java.time.LocalDate;

public record AdminDailyReportMissingRequest(
        String memberUuid,
        LocalDate dateFrom,
        LocalDate dateTo
) {
}
