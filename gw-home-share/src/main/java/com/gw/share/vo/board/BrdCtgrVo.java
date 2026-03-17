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
public class BrdCtgrVo extends BaseVo {

    // 카테고리명
    private String nm;

    // 정렬 순서
    private Integer sortOrd;
}
