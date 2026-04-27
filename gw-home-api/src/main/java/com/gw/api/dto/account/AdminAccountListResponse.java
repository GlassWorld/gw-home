package com.gw.api.dto.account;

import java.time.OffsetDateTime;

public record AdminAccountListResponse(
        String uuid,
        String loginId,
        String email,
        String role,
        String acctStat,
        boolean lckYn,
        boolean otpRequired,
        boolean otpEnabled,
        OffsetDateTime createdAt
) {
}
