package com.gw.share.vo.work;

import com.gw.share.vo.common.BaseVo;
import java.time.LocalDate;
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
public class WeeklyReportVo extends BaseVo {

    // 회원 계정 PK
    private Long mbrAcctIdx;

    // 주 시작일
    private LocalDate wkStrtDt;

    // 주 종료일
    private LocalDate wkEndDt;

    // 제목
    private String ttl;

    // 본문
    private String cntn;

    // 공개 여부
    private String opnYn;

    // 게시 일시
    private OffsetDateTime pblsAt;

    // 생성 유형
    private String genType;
}
