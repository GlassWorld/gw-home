package com.gw.api.dto.work;

import java.time.OffsetDateTime;

public record WorkUnitGitCommitResponse(
        String gitConnectionUuid,
        String provider,
        String repositoryUrl,
        String repositoryName,
        String commitSha,
        String message,
        String authorName,
        OffsetDateTime authoredAt,
        String commitUrl
) {
}
