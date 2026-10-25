package com.gw.share.jvo.life;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LifeTodoSummaryJvo {
    private long totalCount;
    private long todayCount;
    private long upcomingCount;
    private long overdueCount;
    private long completedCount;
    private long unscheduledCount;
}
