package com.gw.api.dto.admin;

public record AdminMemberListRequest(
        String keyword,
        String role,
        Boolean deleted,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {
}
