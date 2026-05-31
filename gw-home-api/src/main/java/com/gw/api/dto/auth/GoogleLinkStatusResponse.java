package com.gw.api.dto.auth;

import java.time.OffsetDateTime;

public record GoogleLinkStatusResponse(
        boolean linked,
        String googleEmail,
        OffsetDateTime linkedAt
) {
}
