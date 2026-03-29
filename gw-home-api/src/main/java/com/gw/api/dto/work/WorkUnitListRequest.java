package com.gw.api.dto.work;

public record WorkUnitListRequest(
        String keyword,
        String category,
        String status,
        String useYn,
        String sort
) {
}
