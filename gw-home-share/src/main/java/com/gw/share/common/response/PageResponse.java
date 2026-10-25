package com.gw.share.common.response;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalCount,
        int totalPages
) {
    // 전체 건수로 페이지 수를 계산하고 기존 페이지 응답 계약을 유지한다.
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalCount) {
        if (page < 1 || size < 1 || totalCount < 0) {
            throw new IllegalArgumentException("페이지 번호·크기는 양수이고 전체 건수는 0 이상이어야 합니다.");
        }
        long totalPages = totalCount / size + (totalCount % size == 0 ? 0 : 1);
        return new PageResponse<>(content, page, size, totalCount,
                (int) Math.min(totalPages, Integer.MAX_VALUE));
    }
}
