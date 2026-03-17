package com.gw.infra.db.support;

import com.gw.share.common.query.SortDirection;
import java.util.Map;

public final class PageSortSupport {

    private PageSortSupport() {
    }

    public static int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static int normalizeSize(Integer size) {
        return size == null || size < 1 ? 20 : size;
    }

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
