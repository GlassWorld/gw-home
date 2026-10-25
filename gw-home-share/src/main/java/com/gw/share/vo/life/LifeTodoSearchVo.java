package com.gw.share.vo.life;

import java.time.LocalDate;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LifeTodoSearchVo {
    private Long mbrAcctIdx;
    private String categoryUuid;
    private boolean uncategorized;
    private String keyword;
    private String group;
    private LocalDate today;
    private int size;
    private long offset;
}
