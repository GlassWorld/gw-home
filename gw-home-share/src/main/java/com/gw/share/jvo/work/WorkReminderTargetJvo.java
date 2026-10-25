package com.gw.share.jvo.work;

import com.gw.share.vo.work.WorkTodoVo;
import lombok.*;
import lombok.experimental.SuperBuilder;

// 소유 TODO와 업무의 예약 가능 상태를 함께 조회한다.
@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
public class WorkReminderTargetJvo extends WorkTodoVo {
    private String workUnitUuid;
    private String workUnitTtl;
    private Boolean active;
    private Boolean leaf;
}
