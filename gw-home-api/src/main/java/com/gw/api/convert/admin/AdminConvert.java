package com.gw.api.convert.admin;

import com.gw.api.dto.admin.AdminBoardPostResponse;
import com.gw.api.dto.admin.AdminMemberResponse;
import com.gw.api.dto.admin.AdminSummaryResponse;
import com.gw.share.jvo.admin.AdminBrdPstJvo;
import com.gw.share.jvo.admin.AdminMbrJvo;
import com.gw.share.jvo.admin.AdminSmryJvo;

public final class AdminConvert {

    private AdminConvert() {
    }

    // 관리자 회원 목록 조회 결과를 응답으로 변환한다.
    public static AdminMemberResponse toMemberResponse(AdminMbrJvo member) {
        return new AdminMemberResponse(
                member.getUuid(),
                member.getLgnId(),
                member.getEmail(),
                member.getRole(),
                member.getNickNm(),
                member.getIntro(),
                member.getCreatedAt(),
                member.getDelAt()
        );
    }

    // 관리자 게시글 목록 조회 결과를 응답으로 변환한다.
    public static AdminBoardPostResponse toBoardPostResponse(AdminBrdPstJvo boardPost) {
        return new AdminBoardPostResponse(
                boardPost.getUuid(),
                boardPost.getCtgrNm(),
                boardPost.getTtl(),
                boardPost.getAthrNickNm(),
                boardPost.getLgnId(),
                nullSafe(boardPost.getFavCnt()),
                nullSafe(boardPost.getCmtCnt()),
                boardPost.getCreatedAt(),
                boardPost.getDelAt()
        );
    }

    // 관리자 대시보드 집계 결과를 응답으로 변환한다.
    public static AdminSummaryResponse toSummaryResponse(AdminSmryJvo summary) {
        return new AdminSummaryResponse(
                nullSafe(summary.getTotalMbrCnt()),
                nullSafe(summary.getActvMbrCnt()),
                nullSafe(summary.getTotalBrdPstCnt()),
                nullSafe(summary.getTotalBrdCmtCnt()),
                nullSafe(summary.getTotalFileCnt())
        );
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
