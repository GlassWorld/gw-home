package com.gw.api.dto.work;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateWeeklyReportRequest(
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 200, message = "title은 200자 이하여야 합니다.")
        String title,
        @NotBlank(message = "content는 필수입니다.")
        String content,
        @JsonAlias("wkStrtDt")
        LocalDate weekStartDate,
        @JsonAlias("wkEndDt")
        LocalDate weekEndDate,
        @Pattern(regexp = "Y|N", message = "openYn은 Y 또는 N 이어야 합니다.")
        String openYn,
        @Pattern(regexp = "MANUAL|OPENAI|RULE_BASED", message = "generationType이 올바르지 않습니다.")
        String generationType
) {
}
