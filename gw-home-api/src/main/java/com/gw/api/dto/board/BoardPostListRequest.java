package com.gw.api.dto.board;

import com.gw.share.common.query.PageSortSupport;

public record BoardPostListRequest(
        String categoryUuid,
        String searchType,
        String keyword,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {
    public int pageOrDefault() {
        return PageSortSupport.normalizePage(page);
    }

    public int sizeOrDefault() {
        return PageSortSupport.normalizeSize(size);
    }
}
