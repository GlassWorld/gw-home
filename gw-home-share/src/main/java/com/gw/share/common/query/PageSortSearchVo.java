package com.gw.share.common.query;

import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PageSortSearchVo {

    @Default
    private int page = 1;

    @Default
    private int size = 20;

    private String sortBy;

    private String sortDirection;

    private String orderByClause;

    public int getOffset() {
        return (Math.max(page, 1) - 1) * size;
    }
}
