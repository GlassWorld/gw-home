package com.gw.api.dto.account;

import java.time.OffsetDateTime;

public record AccountResponse(
        String memberAccountUuid,
        String loginId,
        String email,
        String role,
        OffsetDateTime createdAt
) {
}
