package com.gw.share.vo.board;

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
public class BrdPstVo extends BaseVo {

    // 게시판 카테고리 PK
    private Long brdCtgrIdx;

    // 회원 계정 PK
    private Long mbrAcctIdx;

    // 제목
    private String ttl;

    // 본문
    private String cntnt;

    // 조회 수
    private Integer viewCnt;
}
