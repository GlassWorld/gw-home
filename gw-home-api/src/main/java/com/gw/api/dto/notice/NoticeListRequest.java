package com.gw.api.dto.notice;

public record NoticeListRequest(
        String keyword,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {
}
