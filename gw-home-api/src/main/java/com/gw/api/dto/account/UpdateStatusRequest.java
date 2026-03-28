package com.gw.api.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateStatusRequest(
        @NotBlank(message = "status는 필수입니다.")
        @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "status는 ACTIVE, INACTIVE만 허용됩니다.")
        String status
) {
}
