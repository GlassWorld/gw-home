package com.gw.share.vo.work;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkTodoListSearchVo {
    private Long mbrAcctIdx;
    private String workUnitUuid;
    private String keyword;
    private String group;
    private boolean includeInactive;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate today;
    private int size;
    private long offset;
}
