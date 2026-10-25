package com.gw.share.jvo.life;

import com.gw.share.vo.life.LifeTodoVo;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
public class LifeTodoJvo extends LifeTodoVo {
    private String categoryUuid;
    private String categoryNm;
    private String todoGroup;
}
