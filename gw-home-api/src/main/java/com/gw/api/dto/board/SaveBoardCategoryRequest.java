package com.gw.api.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveBoardCategoryRequest(
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 50, message = "name은 50자 이하여야 합니다.")
        String name,
        @NotNull(message = "sortOrder는 필수입니다.")
        Integer sortOrder
) {
}
