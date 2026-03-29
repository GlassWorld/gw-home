package com.gw.api.dto.work;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateDailyReportRequest(
        @JsonAlias("rptDt")
        LocalDate reportDate,
        List<@NotBlank(message = "workUnitUuids 항목은 비어 있을 수 없습니다.") String> workUnitUuids,
        @Size(max = 4000, message = "content는 4000자 이하여야 합니다.")
        String content,
        @Size(max = 2000, message = "note는 2000자 이하여야 합니다.")
        String note
) {
}
