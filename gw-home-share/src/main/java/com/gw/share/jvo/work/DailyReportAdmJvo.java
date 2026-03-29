package com.gw.share.jvo.work;

import com.gw.share.vo.work.DailyReportVo;
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
public class DailyReportAdmJvo extends DailyReportVo {

    // 회원 계정 UUID
    private String mbrAcctUuid;

    // 로그인 ID
    private String lgnId;

    // 닉네임
    private String nickNm;

    // 최근 작성 일자
    private LocalDate lastRptDt;
}
