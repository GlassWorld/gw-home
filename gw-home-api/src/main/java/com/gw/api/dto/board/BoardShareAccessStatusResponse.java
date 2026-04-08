package com.gw.api.dto.board;

import java.time.OffsetDateTime;

public record BoardShareAccessStatusResponse(
        String status,
        boolean passwordRequired,
        OffsetDateTime expiresAt,
        String title
) {
}
