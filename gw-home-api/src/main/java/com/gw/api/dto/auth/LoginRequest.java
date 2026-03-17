package com.gw.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "loginId는 필수입니다.")
        String loginId,
        @NotBlank(message = "password는 필수입니다.")
        String password
) {
}
