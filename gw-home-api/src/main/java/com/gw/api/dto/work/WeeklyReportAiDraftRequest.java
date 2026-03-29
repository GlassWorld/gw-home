package com.gw.api.dto.work;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record WeeklyReportAiDraftRequest(
        @JsonAlias("wkStrtDt")
        LocalDate weekStartDate,
        @JsonAlias("wkEndDt")
        LocalDate weekEndDate,
        @Size(max = 1000, message = "additionalPrompt는 1000자 이하여야 합니다.")
        String additionalPrompt
) {
}
