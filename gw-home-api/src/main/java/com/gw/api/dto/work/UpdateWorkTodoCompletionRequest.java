package com.gw.api.dto.work;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdateWorkTodoCompletionRequest(
        @JsonAlias({"completed", "is_completed"})
        boolean completed
) {
}
