package com.gw.api.dto.account;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

public record UpdateOtpRequiredRequest(
        @NotNull(message = "otpRequired는 필수입니다.")
        @JsonAlias({"otpRequired", "otp_required"})
        Boolean otpRequired
) {
}
