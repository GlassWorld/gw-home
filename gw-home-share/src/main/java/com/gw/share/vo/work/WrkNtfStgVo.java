package com.gw.share.vo.work;

import com.gw.share.vo.common.BaseVo;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

// 알림 테이블의 저장 필드를 표현한다. 공통 식별·감사 필드는 BaseVo를 사용한다.
@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
public class WrkNtfStgVo extends BaseVo {
    private Long mbrAcctIdx;
    private String rcptEmail;
    private OffsetDateTime vrfdAt;
    private String vrfyHash;
    private OffsetDateTime vrfyExpiresAt;
    private OffsetDateTime vrfySentAt;
    private Integer vrfyTries;
    private OffsetDateTime vrfyWindowAt;
    private Integer vrfyCount;
    private Boolean dgstEnabled;
    private Boolean rmdEnabled;
    private String tmZone;
    private String dgstTime;
    private String dgstCycle;
    private OffsetDateTime nextDgstAt;
    private Long ver;
}
