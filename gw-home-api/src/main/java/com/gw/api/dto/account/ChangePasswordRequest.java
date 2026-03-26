package com.gw.api.dto.account;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "currentPassword는 필수입니다.")
        @JsonAlias("currentPassword")
        String currentPassword,
        @NotBlank(message = "newPassword는 필수입니다.")
        @Size(min = 8, max = 100, message = "newPassword는 8자 이상 100자 이하여야 합니다.")
        @JsonAlias("newPassword")
        String newPassword
) {
}
