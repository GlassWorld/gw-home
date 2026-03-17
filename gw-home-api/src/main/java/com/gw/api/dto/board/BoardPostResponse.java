package com.gw.api.dto.board;

import com.gw.api.dto.tag.TagResponse;
import java.time.OffsetDateTime;
import java.util.List;

public record BoardPostResponse(
        String boardPostUuid,
        String categoryName,
        String title,
        String content,
        int viewCount,
        String author,
        long favoriteCount,
        long commentCount,
        List<TagResponse> tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
