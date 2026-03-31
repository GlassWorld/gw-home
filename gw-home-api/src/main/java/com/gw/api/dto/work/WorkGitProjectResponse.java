package com.gw.api.dto.work;

import java.time.OffsetDateTime;

public record WorkGitProjectResponse(
        String gitProjectUuid,
        String gitAccountUuid,
        String gitAccountLabel,
        String provider,
        String projectName,
        String repositoryUrl,
        String useYn,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
