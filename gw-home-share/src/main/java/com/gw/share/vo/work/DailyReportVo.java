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
public class DailyReportVo extends BaseVo {

    // 회원 계정 PK
    private Long mbrAcctIdx;

    // 보고 일자
    private LocalDate rptDt;

    // 본문 내용
    private String cntn;

    // 진행 상태
    private String sts;

    // 특이사항
    private String spclNote;

    // 연결된 업무 목록
    private List<WorkUnitVo> workUnits;
}
