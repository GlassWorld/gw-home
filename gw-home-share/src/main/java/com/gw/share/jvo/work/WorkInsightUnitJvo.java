package com.gw.share.jvo.work;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter @Setter @NoArgsConstructor @SuperBuilder
public class WorkInsightUnitJvo extends WorkTodoInboxSummaryJvo {
    private String workUnitUuid;
    private String ttl;
    private String sts;
    private String useYn;
    private LocalDate strtDt;
    private LocalDate dueDt;
    private long partialCount;
    private long periodCompletedCount;
}
