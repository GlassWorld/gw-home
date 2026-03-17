package com.gw.api.dto.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateAccountRequest(
        @NotBlank(message = "email은 필수입니다.")
        @Email(message = "email 형식이 올바르지 않습니다.")
        String email
) {
}
