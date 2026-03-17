package com.gw.api.dto.admin;

import java.time.OffsetDateTime;

public record AdminBoardPostResponse(
        String boardPostUuid,
        String categoryName,
        String title,
        String author,
        String loginId,
        long favoriteCount,
        long commentCount,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
}
