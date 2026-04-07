package com.gw.api.dto.work;

import java.util.List;

public record WorkTodoTreeResponse(
        String workUnitUuid,
        WorkTodoSummaryResponse summary,
        List<WorkTodoResponse> todos
) {
}
