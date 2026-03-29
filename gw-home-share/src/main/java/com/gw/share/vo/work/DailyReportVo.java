package com.gw.share.vo.work;

import com.gw.share.vo.common.BaseVo;
import java.time.LocalDate;
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

    // 업무내용
    private String cntn;

    // 진행상태
    private String sts;

    // 특이사항
    private String spclNote;
}
