package com.gw.api.dto.comment;

import java.time.OffsetDateTime;
import java.util.List;

public record CommentResponse(
        String boardCommentUuid,
        String content,
        String author,
        String parentCommentUuid,
        List<CommentResponse> replies,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
