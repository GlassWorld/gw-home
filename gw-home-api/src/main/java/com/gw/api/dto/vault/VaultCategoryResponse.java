package com.gw.api.dto.vault;

public record VaultCategoryResponse(
        String categoryUuid,
        String name,
        String description,
        String color,
        Integer sortOrder
) {
}
