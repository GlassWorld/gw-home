package com.gw.api.dto.work;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record WorkTodoResponse(
        String todoUuid,
        String parentTodoUuid,
        String title,
        String description,
        String status,
        Integer progressRate,
        LocalDate startDate,
        LocalDate dueDate,
        Integer sortOrder,
        Integer depth,
        boolean leaf,
        boolean completed,
        boolean delayed,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<WorkTodoResponse> children
) {
}
