package com.gw.api.dto.work;

public record WorkUnitGitConnectionResponse(
        String gitConnectionUuid,
        String provider,
        String repositoryUrl,
        String authorName,
        boolean hasAccessToken
) {
}
