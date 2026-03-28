package com.gw.api.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateRoleRequest(
        @NotBlank(message = "role은 필수입니다.")
        @Pattern(regexp = "^(ADMIN|USER|GUEST)$", message = "role은 ADMIN, USER, GUEST만 허용됩니다.")
        String role
) {
}
