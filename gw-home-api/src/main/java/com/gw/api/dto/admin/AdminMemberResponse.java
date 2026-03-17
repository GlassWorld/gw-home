package com.gw.api.dto.admin;

import java.time.OffsetDateTime;

public record AdminMemberResponse(
        String memberAccountUuid,
        String loginId,
        String email,
        String role,
        String nickname,
        String intro,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
}
