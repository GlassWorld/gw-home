package com.gw.api.dto.work;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkGitProjectRequest(
        @JsonAlias({"gitAccountUuid", "git_account_uuid"})
        @NotBlank(message = "gitAccountUuid는 필수입니다.")
        String gitAccountUuid,
        @JsonAlias({"projectName", "project_name"})
        @NotBlank(message = "projectName은 필수입니다.")
        @Size(max = 200, message = "projectName은 200자 이하여야 합니다.")
        String projectName,
        @JsonAlias({"repositoryUrl", "repository_url"})
        @NotBlank(message = "repositoryUrl은 필수입니다.")
        @Size(max = 500, message = "repositoryUrl은 500자 이하여야 합니다.")
        String repositoryUrl,
        String useYn
) {
}
