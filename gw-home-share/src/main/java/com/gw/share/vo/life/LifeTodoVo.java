package com.gw.share.vo.life;

import com.gw.share.vo.common.BaseVo;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
public class LifeTodoVo extends BaseVo {
    private Long mbrAcctIdx;
    private Long lfTodoCatIdx;
    private String ttl;
    private String memo;
    private LocalDate dueDt;
    private OffsetDateTime cmplAt;
}
