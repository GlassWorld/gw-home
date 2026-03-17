package com.gw.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken
) {
}
