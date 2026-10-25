package com.gw.share.jvo.work;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter @Setter @NoArgsConstructor @SuperBuilder
public class WorkGanttRowJvo extends WorkTodoListJvo {
    private boolean leaf;
    private boolean contextOnly;
}
