package com.gw.api.dto.file;

public record FileUploadResponse(
        String fileUuid,
        String fileUrl,
        String originalName,
        String mimeType,
        long fileSize
) {
}
