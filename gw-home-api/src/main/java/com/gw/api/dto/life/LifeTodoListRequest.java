package com.gw.api.dto.life;

import com.gw.share.common.policy.LifeTodoPolicy;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LifeTodoListRequest {
    private String categoryUuid;
    private boolean uncategorized;
    @Size(max = 200) private String keyword;
    private String group = "ALL";
    private String timeZone = LifeTodoPolicy.DEFAULT_TIME_ZONE;
    @Min(1) @Max(1000000) private int page = 1;
    @Min(1) @Max(100) private int size = 20;
}
