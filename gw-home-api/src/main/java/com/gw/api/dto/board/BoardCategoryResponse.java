package com.gw.api.dto.board;

public record BoardCategoryResponse(
        String categoryUuid,
        String categoryName,
        int sortOrder
) {
}
