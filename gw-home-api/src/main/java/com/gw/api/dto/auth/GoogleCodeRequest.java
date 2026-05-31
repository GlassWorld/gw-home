package com.gw.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleCodeRequest(
        @NotBlank String code,
        @NotBlank String redirectUri
) {
}
