package com.gw.share.jvo.board;

import com.gw.share.vo.common.BaseVo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BrdPstJvo extends BaseVo {

    // 게시판 카테고리 PK
    private Long brdCtgrIdx;

    // 게시판 카테고리 UUID
    private String brdCtgrUuid;

    // 회원 계정 PK
    private Long mbrAcctIdx;

    // 카테고리명
    private String ctgrNm;

    // 제목
    private String ttl;

    // 본문
    private String cntnt;

    // 조회 수
    private Integer viewCnt;

    // 작성자 닉네임
    private String athrNickNm;

    // 좋아요 수
    private Long favCnt;

    // 댓글 수
    private Long cmtCnt;
}
