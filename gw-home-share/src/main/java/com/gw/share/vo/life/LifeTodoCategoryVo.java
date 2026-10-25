package com.gw.share.vo.life;

import com.gw.share.vo.common.BaseVo;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
public class LifeTodoCategoryVo extends BaseVo {
    private Long mbrAcctIdx;
    private String nm;
}
