package com.gw.api.dto.work;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWorkUnitRequest(
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 200, message = "title은 200자 이하여야 합니다.")
        String title,
        @Size(max = 100, message = "category는 100자 이하여야 합니다.")
        String category,
        String description,
        @JsonAlias("workStatus")
        @Pattern(
                regexp = "IN_PROGRESS|DONE|ON_HOLD",
                message = "status는 IN_PROGRESS, DONE, ON_HOLD 중 하나여야 합니다."
        )
        String status
) {
}
