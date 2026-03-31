package com.gw.api.dto.work;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WorkGitAccountRequest(
        @JsonAlias({"accountLabel", "account_label"})
        @NotBlank(message = "accountLabel은 필수입니다.")
        @Size(max = 100, message = "accountLabel은 100자 이하여야 합니다.")
        String accountLabel,
        @Pattern(regexp = "GITLAB", message = "provider는 GITLAB 이어야 합니다.")
        String provider,
        @JsonAlias({"authorName", "author_name"})
        @NotBlank(message = "authorName은 필수입니다.")
        @Size(max = 200, message = "authorName은 200자 이하여야 합니다.")
        String authorName,
        @JsonAlias({"accessToken", "access_token"})
        @Size(max = 2000, message = "accessToken은 2000자 이하여야 합니다.")
        String accessToken,
        String useYn
) {
}
