package com.gw.api.dto.account;

import java.time.OffsetDateTime;

public record AdminAccountDetailResponse(
        String uuid,
        String loginId,
        String email,
        String role,
        String acctStat,
        boolean lckYn,
        OffsetDateTime lckAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
