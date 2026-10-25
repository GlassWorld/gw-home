package com.gw.share.common.query;

import java.util.Map;

public final class PageSortSupport {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageSortSupport() {
    }

    // 누락되거나 잘못된 페이지 번호를 첫 페이지로 보정한다.
    public static int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    // 기본 페이지 크기를 유지하고 과도한 조회 요청은 공통 상한으로 보정한다.
    public static int normalizeSize(Integer size) {
        return size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }

    // 큰 페이지 번호에서도 오버플로 없이 조회 시작 위치를 계산한다.
    public static long calculateOffset(int page, int size) {
        return (normalizePage(page) - 1L) * normalizeSize(size);
    }

    // 허용된 컬럼과 정렬 방향만 SQL 정렬절에 반영한다.
    public static String resolveOrderByClause(
            String sortBy,
            String sortDirection,
            Map<String, String> sortFieldMap,
            String defaultSortBy,
            SortDirection defaultDirection
    ) {
        String resolvedSortBy = sortBy != null && sortFieldMap.containsKey(sortBy) ? sortBy : defaultSortBy;
        SortDirection resolvedDirection = SortDirection.from(sortDirection, defaultDirection);
        return sortFieldMap.get(resolvedSortBy) + " " + resolvedDirection.name();
    }
}
