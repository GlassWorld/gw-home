package com.gw.share.vo.common;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseVo {

    // PK
    private Long idx;

    // UUID
    private String uuid;

    // 생성자 로그인 ID
    private String createdBy;

    // 수정자 로그인 ID
    private String updatedBy;

    // 생성 일시
    private OffsetDateTime createdAt;

    // 수정 일시
    private OffsetDateTime updatedAt;

    // 삭제 일시
    private OffsetDateTime delAt;
}
