package com.gw.share.vo.work;

import com.gw.share.vo.common.BaseVo;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

// 알림 테이블의 저장 필드를 표현한다. 공통 식별·감사 필드는 BaseVo를 사용한다.
@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
public class WrkTodoRmdVo extends BaseVo {
    private Long mbrAcctIdx;
    private Long wrkTodoIdx;
    private OffsetDateTime sendAt;
    private Boolean enabled;
    private Long ver;
    private LocalDate strtDt;
    private LocalDate dueDt;
}
