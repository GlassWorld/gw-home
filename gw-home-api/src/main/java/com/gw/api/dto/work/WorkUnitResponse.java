package com.gw.api.dto.work;

import java.time.OffsetDateTime;

public record WorkUnitResponse(
        String workUnitUuid,
        String title,
        String description,
        String category,
        String status,
        String useYn,
        Integer useCount,
        OffsetDateTime lastUsedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
