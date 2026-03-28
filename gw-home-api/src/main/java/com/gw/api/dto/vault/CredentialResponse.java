package com.gw.api.dto.vault;

import java.time.OffsetDateTime;
import java.util.List;

public record CredentialResponse(
        String credentialUuid,
        String title,
        List<CredentialCategoryResponse> categories,
        String loginId,
        String password,
        String memo,
        OffsetDateTime createdAt
) {
}
