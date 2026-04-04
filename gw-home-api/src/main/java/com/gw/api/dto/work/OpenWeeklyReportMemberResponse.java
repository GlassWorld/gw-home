package com.gw.api.dto.work;

import java.time.OffsetDateTime;

public record OpenWeeklyReportMemberResponse(
        String memberUuid,
        String loginId,
        String nickname,
        long openReportCount,
        OffsetDateTime lastPublishedAt
) {
}
