package com.gw.api.dto.work;

import java.time.LocalDate;

public record DailyReportListRequest(
        String memberUuid,
        LocalDate dateFrom,
        LocalDate dateTo,
        String keyword,
        Integer page,
        Integer size
) {
}
