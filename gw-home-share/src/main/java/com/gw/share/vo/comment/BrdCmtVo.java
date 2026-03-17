package com.gw.share.vo.comment;

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
public class BrdCmtVo extends BaseVo {

    // 게시글 PK
    private Long brdPstIdx;

    // 부모 댓글 PK
    private Long prntBrdCmtIdx;

    // 회원 계정 PK
    private Long mbrAcctIdx;

    // 댓글 내용
    private String cntnt;
}
