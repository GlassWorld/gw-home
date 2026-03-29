package com.gw.api.dto.work;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateWorkUnitUseRequest(
        @NotBlank(message = "useYn은 필수입니다.")
        @Pattern(regexp = "Y|N", message = "useYn은 Y 또는 N 이어야 합니다.")
        String useYn
) {
}
