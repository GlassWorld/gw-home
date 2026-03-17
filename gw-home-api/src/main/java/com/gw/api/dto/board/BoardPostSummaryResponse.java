package com.gw.api.dto.board;

import java.time.OffsetDateTime;

public record BoardPostSummaryResponse(
        String boardPostUuid,
        String categoryName,
        String title,
        int viewCount,
        String author,
        long favoriteCount,
        long commentCount,
        OffsetDateTime createdAt
) {
}
