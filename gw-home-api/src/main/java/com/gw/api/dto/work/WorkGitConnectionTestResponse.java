package com.gw.api.dto.work;

import java.time.OffsetDateTime;

public record WorkGitConnectionTestResponse(
        String gitProjectUuid,
        String provider,
        String projectName,
        String repositoryUrl,
        boolean connected,
        String message,
        OffsetDateTime checkedAt
) {
}
