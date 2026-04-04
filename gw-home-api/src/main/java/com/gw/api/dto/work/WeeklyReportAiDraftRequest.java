package com.gw.api.dto.work;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record WeeklyReportAiDraftRequest(
        @JsonAlias("wkStrtDt")
        LocalDate weekStartDate,
        @JsonAlias("wkEndDt")
        LocalDate weekEndDate,
        @Size(max = 1000, message = "additionalPrompt는 1000자 이하여야 합니다.")
        String additionalPrompt,
        @JsonAlias("source_daily_reports")
        @Size(max = 5, message = "sourceDailyReports는 최대 5건까지 전달할 수 있습니다.")
        List<@Valid WeeklyReportAiDraftSourceRequest> sourceDailyReports
) {
}
