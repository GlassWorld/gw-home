package com.gw.api.dto.vault;

public record CredentialCategoryResponse(
        String categoryUuid,
        String name,
        String color
) {
}
