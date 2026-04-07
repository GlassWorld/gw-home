package com.gw.api.dto.work;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateWorkTodoRequest(
        @JsonAlias({"parentTodoUuid", "parent_todo_uuid"})
        String parentTodoUuid,
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 200, message = "title은 200자 이하여야 합니다.")
        String title,
        String description,
        @JsonAlias({"startDate", "start_date"})
        LocalDate startDate,
        @JsonAlias({"dueDate", "due_date"})
        LocalDate dueDate,
        @JsonAlias({"sortOrder", "sort_order"})
        @Min(value = 1, message = "sortOrder는 1 이상이어야 합니다.")
        @Max(value = 9999, message = "sortOrder는 9999 이하여야 합니다.")
        Integer sortOrder
) {
}
