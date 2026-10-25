package com.gw.api.dto.work;

import com.gw.share.common.policy.WorkTodoPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class WorkTodoListRequest {
    private String workUnitUuid;
    @Size(max = 200)
    private String keyword;
    private String group = "ALL";
    private boolean includeInactive;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;
    private String timeZone = WorkTodoPolicy.DEFAULT_TIME_ZONE;
    @Min(1) @Max(1000000)
    private int page = 1;
    @Min(1) @Max(100)
    private int size = 20;
}
