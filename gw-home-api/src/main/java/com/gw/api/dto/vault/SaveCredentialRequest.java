package com.gw.api.dto.vault;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveCredentialRequest(
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 200, message = "title은 200자 이하여야 합니다.")
        String title,
        @JsonAlias("categoryUuid")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        List<String> categoryUuids,
        @JsonAlias("loginId")
        @Size(max = 200, message = "loginId는 200자 이하여야 합니다.")
        String loginId,
        @NotBlank(message = "password는 필수입니다.")
        String password,
        String memo
) {
}
