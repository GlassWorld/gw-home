package com.gw.api.dto.work;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record WorkTodoListResponse(
        String todoUuid, String workUnitUuid, String workUnitTitle, boolean workUnitActive,
        String parentTodoUuid, String parentTitle, String title, String description,
        LocalDate startDate, LocalDate dueDate, Integer sortOrder,
        boolean completed, OffsetDateTime completedAt, String group
) { }
