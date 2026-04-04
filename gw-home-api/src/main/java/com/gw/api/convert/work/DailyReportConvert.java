package com.gw.api.convert.work;

import com.gw.api.dto.work.AdminDailyReportResponse;
import com.gw.api.dto.work.AdminDailyReportMissingResponse;
import com.gw.api.dto.work.DailyReportResponse;
import com.gw.share.jvo.work.DailyReportAdmJvo;
import java.time.LocalDate;
import java.util.List;
import com.gw.share.vo.work.DailyReportVo;

public final class DailyReportConvert {

    private DailyReportConvert() {
    }

    // 일일보고 VO를 사용자 응답으로 변환한다.
    public static DailyReportResponse toResponse(DailyReportVo dailyReport) {
        return new DailyReportResponse(
                dailyReport.getUuid(),
                dailyReport.getRptDt(),
                WorkUnitConvert.toDailyReportWorkUnitResponses(dailyReport.getWorkUnits()),
                dailyReport.getCntn(),
                dailyReport.getSpclNote(),
                dailyReport.getCreatedAt(),
                dailyReport.getUpdatedAt()
        );
    }

    // 관리자 조회용 JVO를 관리자 응답으로 변환한다.
    public static AdminDailyReportResponse toAdminResponse(DailyReportAdmJvo dailyReport) {
        return new AdminDailyReportResponse(
                dailyReport.getUuid(),
                dailyReport.getMbrAcctUuid(),
                dailyReport.getLgnId(),
                dailyReport.getNickNm(),
                dailyReport.getRptDt(),
                WorkUnitConvert.toDailyReportWorkUnitResponses(dailyReport.getWorkUnits()),
                dailyReport.getCntn(),
                dailyReport.getSpclNote(),
                dailyReport.getCreatedAt(),
                dailyReport.getUpdatedAt()
        );
    }

    // 관리자 누락 일일보고 현황을 응답으로 변환한다.
    public static AdminDailyReportMissingResponse toAdminMissingResponse(
            String memberUuid,
            String loginId,
            String nickname,
            List<LocalDate> missingDates,
            LocalDate lastWrittenDate
    ) {
        return new AdminDailyReportMissingResponse(
                memberUuid,
                loginId,
                nickname,
                missingDates,
                missingDates.size(),
                lastWrittenDate
        );
    }
}
