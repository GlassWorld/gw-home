package com.gw.api.convert.work;

import com.gw.api.dto.work.WeeklyReportDailySourceResponse;
import com.gw.api.dto.work.WeeklyReportAiDraftResponse;
import com.gw.api.dto.work.OpenWeeklyReportMemberResponse;
import com.gw.api.dto.work.OpenWeeklyReportResponse;
import com.gw.api.dto.work.WeeklyReportResponse;
import com.gw.share.jvo.work.OpenWeeklyReportJvo;
import com.gw.share.vo.work.DailyReportVo;
import com.gw.share.vo.work.WeeklyReportVo;

public final class WeeklyReportConvert {

    private WeeklyReportConvert() {
    }

    // 주간보고 VO를 응답 DTO로 변환한다.
    public static WeeklyReportResponse toResponse(WeeklyReportVo weeklyReport) {
        return new WeeklyReportResponse(
                weeklyReport.getUuid(),
                weeklyReport.getWkStrtDt(),
                weeklyReport.getWkEndDt(),
                weeklyReport.getTtl(),
                weeklyReport.getCntn(),
                weeklyReport.getOpnYn(),
                weeklyReport.getPblsAt(),
                weeklyReport.getGenType(),
                weeklyReport.getCreatedAt(),
                weeklyReport.getUpdatedAt()
        );
    }

    // 공개 주간보고 조회용 회원 요약 응답으로 변환한다.
    public static OpenWeeklyReportMemberResponse toOpenMemberResponse(OpenWeeklyReportJvo weeklyReport) {
        return new OpenWeeklyReportMemberResponse(
                weeklyReport.getMbrAcctUuid(),
                weeklyReport.getLgnId(),
                weeklyReport.getNickNm(),
                weeklyReport.getOpenRptCnt(),
                weeklyReport.getLastPblsAt()
        );
    }

    // 공개 주간보고 조회용 응답으로 변환한다.
    public static OpenWeeklyReportResponse toOpenResponse(OpenWeeklyReportJvo weeklyReport) {
        return new OpenWeeklyReportResponse(
                weeklyReport.getUuid(),
                weeklyReport.getMbrAcctUuid(),
                weeklyReport.getLgnId(),
                weeklyReport.getNickNm(),
                weeklyReport.getWkStrtDt(),
                weeklyReport.getWkEndDt(),
                weeklyReport.getTtl(),
                weeklyReport.getCntn(),
                weeklyReport.getOpnYn(),
                weeklyReport.getPblsAt(),
                weeklyReport.getGenType(),
                weeklyReport.getCreatedAt(),
                weeklyReport.getUpdatedAt()
        );
    }

    // 주간보고 소스용 일일보고 VO를 응답 DTO로 변환한다.
    public static WeeklyReportDailySourceResponse toDailySourceResponse(DailyReportVo dailyReport) {
        return new WeeklyReportDailySourceResponse(
                dailyReport.getUuid(),
                dailyReport.getRptDt(),
                WorkUnitConvert.toDailyReportWorkUnitResponses(dailyReport.getWorkUnits()),
                dailyReport.getCntn(),
                dailyReport.getSpclNote()
        );
    }

    // 주간보고 AI 초안 결과를 응답으로 변환한다.
    public static WeeklyReportAiDraftResponse toAiDraftResponse(
            String title,
            String content,
            String generationType,
            int sourceCount,
            String modelName
    ) {
        return new WeeklyReportAiDraftResponse(title, content, generationType, sourceCount, modelName);
    }
}
