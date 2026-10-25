package com.gw.share.jvo.work;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WorkTodoInboxSummaryJvo {
    private long totalCount;
    private long todayCount;
    private long upcomingCount;
    private long overdueCount;
    private long completedCount;
    private long unscheduledCount;
    private long unknownCompletionCount;
}
