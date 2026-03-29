package com.gw.api.dto.work;

import java.time.LocalDate;
import java.util.List;

public record AdminDailyReportMissingResponse(
        String memberUuid,
        String loginId,
        String nickname,
        List<LocalDate> missingDates,
        int missingCount,
        LocalDate lastWrittenDate
) {
}
