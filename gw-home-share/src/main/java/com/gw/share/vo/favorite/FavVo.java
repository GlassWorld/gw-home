package com.gw.share.vo.favorite;

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
public class FavVo extends BaseVo {

    // 대상 타입
    private String trgtType;

    // 대상 PK
    private Long trgtIdx;

    // 회원 계정 PK
    private Long mbrAcctIdx;
}
