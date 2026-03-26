package com.gw.api.dto.profile;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

public record SaveMemoRequest(
        @JsonAlias("memo")
        @Size(max = 2000, message = "memo는 2000자 이하여야 합니다.")
        String memo
) {
}
