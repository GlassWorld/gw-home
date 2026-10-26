package com.gw.share.jvo.life;

import java.time.LocalDate;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LifeDailyCheckJvo {
    private String itemUuid;
    private LocalDate checkDt;
}
