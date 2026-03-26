package com.gw.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotBlank(message = "otpCode는 필수입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "otpCode는 6자리 숫자여야 합니다.")
        @JsonAlias("otpCode")
        String otpCode,
        @NotBlank(message = "otpTempToken은 필수입니다.")
        @JsonAlias("otpTempToken")
        String otpTempToken
) {
}
