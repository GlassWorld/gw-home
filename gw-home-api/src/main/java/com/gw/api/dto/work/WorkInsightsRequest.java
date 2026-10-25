package com.gw.api.dto.work;

import com.gw.share.common.policy.WorkTodoPolicy;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter @Setter
public class WorkInsightsRequest {
    private String workUnitUuid;
    private Boolean includeInactive;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;
    private String timeZone = WorkTodoPolicy.DEFAULT_TIME_ZONE;
    private String bucket = "day";
}
