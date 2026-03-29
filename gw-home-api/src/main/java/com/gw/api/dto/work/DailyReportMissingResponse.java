package com.gw.api.dto.work;

import java.time.LocalDate;

public record DailyReportMissingResponse(
        LocalDate reportDate
) {
}
