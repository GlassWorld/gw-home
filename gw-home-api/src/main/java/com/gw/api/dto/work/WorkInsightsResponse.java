package com.gw.api.dto.work;

import java.time.LocalDate;
import java.util.List;

public final class WorkInsightsResponse {
    private WorkInsightsResponse() { }
    public record Period(LocalDate fromDate, LocalDate toDate, String timeZone, LocalDate asOfDate,
            boolean includeInactive, String workUnitUuid) { }
    public record Unit(String workUnitUuid, String title, String status, boolean active,
            LocalDate startDate, LocalDate dueDate, boolean partialSchedule,
            WorkTodoInboxSummaryResponse summary, long periodCompletedCount) { }
    public record GanttRow(String todoUuid, String workUnitUuid, String parentTodoUuid, String title,
            LocalDate startDate, LocalDate dueDate, String status, int progressRate,
            boolean leaf, boolean completed, boolean delayed, boolean contextOnly, int depth) { }
    public record Gantt(Period period, List<Unit> units, List<GanttRow> rows) { }
    public record CompletionBucket(LocalDate startDate, LocalDate endDate, long completedCount) { }
    public record Statistics(Period period, String bucket, WorkTodoInboxSummaryResponse summary,
            long periodCompletedCount, List<CompletionBucket> trend, List<Unit> units) { }
}
