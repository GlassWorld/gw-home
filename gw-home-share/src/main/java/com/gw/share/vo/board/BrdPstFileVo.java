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
public class BrdPstFileVo extends BaseVo {

    // 게시글 PK
    private Long brdPstIdx;

    // 파일 PK
    private Long fileIdx;

    // 첨부 역할
    private String fileRole;

    // 정렬 순서
    private Integer sortOrd;
}
