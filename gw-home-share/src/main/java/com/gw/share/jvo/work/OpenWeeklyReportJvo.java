package com.gw.share.jvo.work;

import com.gw.share.vo.work.WeeklyReportVo;
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
public class OpenWeeklyReportJvo extends WeeklyReportVo {

    // 회원 계정 UUID
    private String mbrAcctUuid;

    // 로그인 ID
    private String lgnId;

    // 닉네임
    private String nickNm;

    // 공개 주간보고 수
    private long openRptCnt;

    // 최근 공개 시각
    private OffsetDateTime lastPblsAt;
}
