package com.gw.share.jvo.work;

import com.gw.share.vo.work.WorkTodoVo;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WorkTodoListJvo extends WorkTodoVo {
    private String workUnitUuid;
    private String workUnitTtl;
    private String workUnitUseYn;
    private String prntTodoUuid;
    private String prntTodoTtl;
    private String todoGroup;
}
