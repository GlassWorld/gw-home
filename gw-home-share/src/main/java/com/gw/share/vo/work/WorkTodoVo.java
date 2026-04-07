package com.gw.share.vo.work;

import com.gw.share.vo.common.BaseVo;
import java.time.LocalDate;
import java.util.List;
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
public class WorkTodoVo extends BaseVo {

    // 업무 PK
    private Long workUnitIdx;

    // 회원 계정 PK
    private Long mbrAcctIdx;

    // 부모 TODO PK
    private Long prntWrkTodoIdx;

    // 제목
    private String ttl;

    // 설명
    private String dscr;

    // 상태
    private String sts;

    // 진행률
    private Integer prgsRt;

    // 시작일
    private LocalDate strtDt;

    // 마감일
    private LocalDate dueDt;

    // 정렬 순서
    private Integer sortOrd;

    // 자식 TODO 목록
    private List<WorkTodoVo> children;
}
