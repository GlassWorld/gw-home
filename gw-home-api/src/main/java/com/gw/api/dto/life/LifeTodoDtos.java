package com.gw.api.dto.life;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class LifeTodoDtos {
    private LifeTodoDtos() { }

    public record SaveTodo(@NotBlank @Size(max = 200) String title,
                           @Size(max = 4000) String memo, String categoryUuid, LocalDate dueDate) { }
    public record Completion(@NotNull Boolean completed) { }
    public record SaveCategory(@NotBlank @Size(max = 50) String name) { }
    public record Category(String categoryUuid, String name) { }
    public record Todo(String todoUuid, String title, String memo, String categoryUuid, String categoryName,
                       LocalDate dueDate, boolean completed, OffsetDateTime completedAt,
                       String group, OffsetDateTime createdAt, OffsetDateTime updatedAt) { }
    public record Summary(long totalCount, long todayCount, long upcomingCount, long overdueCount,
                          long completedCount, long unscheduledCount, int progressRate, LocalDate asOfDate) { }
}
