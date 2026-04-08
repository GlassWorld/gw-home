package com.gw.api.dto.board;

import java.time.OffsetDateTime;

public record BoardShareSettingsResponse(
        boolean shareEnabled,
        String status,
        String shareToken,
        boolean passwordEnabled,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt,
        OffsetDateTime createdAt
) {
}
