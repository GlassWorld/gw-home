package com.gw.api.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank(message = "loginId는 필수입니다.")
        @Size(min = 4, max = 30, message = "loginId는 4자 이상 30자 이하여야 합니다.")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "loginId는 영문, 숫자, 언더스코어만 사용할 수 있습니다.")
        String loginId,

        @NotBlank(message = "password는 필수입니다.")
        @Size(min = 8, max = 100, message = "password는 8자 이상 100자 이하여야 합니다.")
        String password,

        @NotBlank(message = "email은 필수입니다.")
        @Email(message = "email 형식이 올바르지 않습니다.")
        String email
) {
}
