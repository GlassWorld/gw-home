package com.gw.api.dto.vault;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveVaultCategoryRequest(
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        String name,
        String description,
        @JsonAlias("sortOrder")
        Integer sortOrder
) {
}
