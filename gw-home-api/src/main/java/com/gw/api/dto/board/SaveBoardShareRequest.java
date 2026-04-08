package com.gw.api.dto.board;

import java.time.OffsetDateTime;

public record SaveBoardShareRequest(
        Boolean shareEnabled,
        Integer expirationDays,
        OffsetDateTime expiresAt,
        Boolean passwordEnabled,
        String password
) {
}
