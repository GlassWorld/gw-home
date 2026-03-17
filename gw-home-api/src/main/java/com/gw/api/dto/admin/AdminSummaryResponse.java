package com.gw.api.dto.admin;

public record AdminSummaryResponse(
        long totalMemberCount,
        long activeMemberCount,
        long totalBoardPostCount,
        long totalCommentCount,
        long totalFileCount
) {
}
