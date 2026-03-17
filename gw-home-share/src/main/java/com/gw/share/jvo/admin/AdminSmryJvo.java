package com.gw.share.jvo.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSmryJvo {

    // 전체 회원 수
    private Long totalMbrCnt;

    // 활성 회원 수
    private Long actvMbrCnt;

    // 전체 게시글 수
    private Long totalBrdPstCnt;

    // 전체 댓글 수
    private Long totalBrdCmtCnt;

    // 전체 파일 수
    private Long totalFileCnt;
}
