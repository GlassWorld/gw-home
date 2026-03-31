package com.gw.api.dto.work;

import java.time.OffsetDateTime;

public record WorkGitAccountResponse(
        String gitAccountUuid,
        String provider,
        String accountLabel,
        String authorName,
        boolean hasAccessToken,
        String useYn,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
