package com.gw.api.dto.work;

public record WeeklyReportAiDraftResponse(
        String title,
        String content,
        String generationType,
        int sourceCount,
        String modelName
) {
}
