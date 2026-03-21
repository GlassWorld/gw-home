package com.gw.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "loginId는 필수입니다.")
        @JsonAlias("loginId")
        String loginId,
        @NotBlank(message = "password는 필수입니다.")
        String password
) {
}
