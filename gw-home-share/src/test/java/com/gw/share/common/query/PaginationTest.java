package com.gw.share.common.query;

import com.gw.share.common.response.PageResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaginationTest {
    @Test
    void defaultsAndLargeOffsetsRemainValid() {
        assertEquals(1, PageSortSupport.normalizePage(null));
        assertEquals(1, PageSortSupport.normalizePage(-1));
        assertEquals(20, PageSortSupport.normalizeSize(null));
        assertEquals(20, PageSortSupport.normalizeSize(0));
        assertEquals(20L, PageSortSupport.calculateOffset(2, 20));
        assertEquals(0L, PageSortSupport.calculateOffset(0, 20));
        assertEquals(42_949_672_920L,
                PageSortSearchVo.builder().page(Integer.MAX_VALUE).size(20).build().getOffset());
        assertEquals(214_748_364_600L,
                PageSortSupport.calculateOffset(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void pageSizeIsCappedWithoutChangingDefaultsOrValidSizes() {
        assertEquals(100, PageSortSupport.normalizeSize(100));
        assertEquals(100, PageSortSupport.normalizeSize(101));
        assertEquals(100, PageSortSupport.normalizeSize(Integer.MAX_VALUE));
        assertEquals(1, PageSortSupport.normalizeSize(1));
        assertEquals(20, PageSortSupport.normalizeSize(-1));
        assertEquals(100L, PageSortSupport.calculateOffset(2, 101));
    }

    @Test
    void sortingAcceptsOnlyKnownColumnsAndDirections() {
        var fields = Map.of("createdAt", "bp.created_at", "title", "bp.ttl");
        assertEquals("bp.ttl ASC", PageSortSupport.resolveOrderByClause(
                "title", "asc", fields, "createdAt", SortDirection.DESC));
        assertEquals("bp.created_at DESC", PageSortSupport.resolveOrderByClause(
                "ttl; DELETE FROM tb_brd_pst", "ASC NULLS FIRST", fields, "createdAt", SortDirection.DESC));
    }

    @Test
    void pageCountCoversEmptyExactPartialAndOutOfRangePages() {
        assertEquals(0, PageResponse.of(List.of(), 1, 20, 0).totalPages());
        assertEquals(1, PageResponse.of(List.of(), 1, 20, 20).totalPages());
        var response = PageResponse.of(List.of(), 9, 20, 21);
        assertEquals(2, response.totalPages());
        assertEquals(9, response.page());
        assertEquals(21, response.totalCount());
        assertEquals(Integer.MAX_VALUE, PageResponse.of(List.of(), 1, 1, Long.MAX_VALUE).totalPages());
        assertEquals(Integer.MAX_VALUE, PageResponse.of(List.of(), 1, Integer.MAX_VALUE, Long.MAX_VALUE).totalPages());
        assertEquals(536_870_913, PageResponse.of(List.of(), 1, 16_777_216, 9_007_199_254_740_993L).totalPages());
    }

    @Test
    void invalidPageMetadataIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PageResponse.of(List.of(), 0, 20, 0));
        assertThrows(IllegalArgumentException.class, () -> PageResponse.of(List.of(), 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> PageResponse.of(List.of(), 1, 20, -1));
    }
}
