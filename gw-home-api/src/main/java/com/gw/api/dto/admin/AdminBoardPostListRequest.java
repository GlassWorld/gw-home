package com.gw.api.dto.admin;

public record AdminBoardPostListRequest(
        String keyword,
        Boolean deleted,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {
}
