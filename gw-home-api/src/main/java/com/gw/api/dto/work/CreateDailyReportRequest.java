package com.gw.api.dto.work;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateDailyReportRequest(
        @JsonAlias("rptDt")
        LocalDate reportDate,
        @NotBlank(message = "content는 필수입니다.")
        String content,
        @Pattern(
                regexp = "PLANNED|IN_PROGRESS|DONE",
                message = "status는 PLANNED, IN_PROGRESS, DONE 중 하나여야 합니다."
        )
        String status,
        @Size(max = 2000, message = "note는 2000자 이하여야 합니다.")
        String note
) {
}
