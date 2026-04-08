package com.gw.api.dto.board;

import java.time.OffsetDateTime;

public record BoardShareAttachmentResponse(
        String originalName,
        String mimeType,
        long fileSize,
        OffsetDateTime createdAt
) {
}
