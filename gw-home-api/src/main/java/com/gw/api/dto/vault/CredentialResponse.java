package com.gw.api.dto.vault;

import java.time.OffsetDateTime;

public record CredentialResponse(
        String credentialUuid,
        String title,
        String categoryUuid,
        String categoryName,
        String categoryColor,
        String loginId,
        String password,
        String memo,
        OffsetDateTime createdAt
) {
}
