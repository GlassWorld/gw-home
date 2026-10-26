package com.gw.share.vo.life;

import com.gw.share.vo.common.BaseVo;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
public class LifeDailyVo extends BaseVo {
    private Long mbrAcctIdx;
    private String ttl;
    private LocalDate startDt;
    private LocalDate endDt;
}
