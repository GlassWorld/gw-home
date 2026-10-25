package com.gw.api.dto.work;

import java.time.LocalDate;

public record WorkTodoInboxSummaryResponse(
        long totalCount, long todayCount, long upcomingCount, long overdueCount,
        long completedCount, long unscheduledCount, long unknownCompletionCount,
        int progressRate, LocalDate asOfDate, String timeZone
) { }
