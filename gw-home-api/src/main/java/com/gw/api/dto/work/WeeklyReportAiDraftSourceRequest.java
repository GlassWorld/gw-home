package com.gw.api.dto.work;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record WeeklyReportAiDraftSourceRequest(
        String uuid,
        @JsonAlias("report_date")
        LocalDate reportDate,
        @JsonAlias("work_units")
        List<@Valid WeeklyReportAiDraftWorkUnitRequest> workUnits,
        @Size(max = 4000, message = "content는 4000자 이하여야 합니다.")
        String content,
        @Size(max = 2000, message = "note는 2000자 이하여야 합니다.")
        String note
) {
}
