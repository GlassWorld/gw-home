package com.gw.api.convert.work;

import com.gw.api.dto.work.WorkTodoResponse;
import com.gw.api.dto.work.WorkTodoSummaryResponse;
import com.gw.api.dto.work.WorkTodoTreeResponse;
import java.util.List;

public final class WorkTodoConvert {

    private WorkTodoConvert() {
    }

    public static WorkTodoTreeResponse toTreeResponse(
            String workUnitUuid,
            WorkTodoSummaryResponse summary,
            List<WorkTodoResponse> todos
    ) {
        return new WorkTodoTreeResponse(workUnitUuid, summary, todos);
    }
}
