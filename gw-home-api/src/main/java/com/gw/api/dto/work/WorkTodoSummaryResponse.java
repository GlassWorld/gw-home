package com.gw.api.dto.work;

public record WorkTodoSummaryResponse(
        String status,
        Integer progressRate,
        Integer itemCount,
        Integer leafCount,
        Integer completedLeafCount,
        Integer delayedCount
) {
}
