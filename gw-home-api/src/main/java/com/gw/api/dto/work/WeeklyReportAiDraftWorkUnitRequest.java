package com.gw.api.dto.work;

import com.fasterxml.jackson.annotation.JsonAlias;

public record WeeklyReportAiDraftWorkUnitRequest(
        @JsonAlias("work_unit_uuid")
        String workUnitUuid,
        String title,
        String category
) {
}
