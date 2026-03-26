package com.gw.share.vo.vault;

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
public class CatVo extends BaseVo {

    // 카테고리명
    private String nm;

    // 카테고리 설명
    private String dsc;

    // 카테고리 색상
    private String color;

    // 정렬 순서
    private Integer sortOrd;
}
