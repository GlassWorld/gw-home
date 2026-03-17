package com.gw.api.dto.profile;

import java.time.OffsetDateTime;

public record ProfileResponse(
        String memberProfileUuid,
        String nickname,
        String introduction,
        String profileImageUrl,
        OffsetDateTime createdAt
) {
}
