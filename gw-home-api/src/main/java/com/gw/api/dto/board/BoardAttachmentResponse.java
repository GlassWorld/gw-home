package com.gw.api.dto.board;

import java.time.OffsetDateTime;

public record BoardAttachmentResponse(
        String fileUuid,
        String originalName,
        String fileUrl,
        String mimeType,
        long fileSize,
        String uploaderType,
        OffsetDateTime createdAt
) {
}
