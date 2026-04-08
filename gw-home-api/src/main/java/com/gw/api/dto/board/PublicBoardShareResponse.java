package com.gw.api.dto.board;

import com.gw.api.dto.comment.CommentResponse;
import java.time.OffsetDateTime;
import java.util.List;

public record PublicBoardShareResponse(
        String title,
        String categoryName,
        String content,
        String author,
        List<BoardShareAttachmentResponse> attachments,
        List<CommentResponse> comments,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime expiresAt
) {
}
