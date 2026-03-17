package com.gw.api.dto.board;

public record BoardPostListRequest(
        String categoryUuid,
        String keyword,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {
    public int pageOrDefault() {
        return page == null || page < 1 ? 1 : page;
    }

    public int sizeOrDefault() {
        return size == null || size < 1 ? 20 : size;
    }
}
