package com.gw.infra.db.mapper.work;

import com.gw.share.jvo.work.DailyReportAdmJvo;
import com.gw.share.jvo.work.OpenWeeklyReportJvo;
import com.gw.share.vo.work.DailyReportListSearchVo;
import com.gw.share.vo.work.DailyReportVo;
import com.gw.share.vo.work.WorkUnitVo;
import com.gw.share.vo.work.WeeklyReportVo;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DailyReportMapper {

    List<DailyReportVo> selectDailyReportList(@Param("req") DailyReportListSearchVo req);

    long countDailyReportList(@Param("req") DailyReportListSearchVo req);

    DailyReportVo selectDailyReport(@Param("uuid") String uuid, @Param("mbrAcctIdx") Long mbrAcctIdx);

    DailyReportVo selectDailyReportByIdx(@Param("idx") Long idx);

    void insertDailyReport(DailyReportVo dailyReport);

    int updateDailyReport(DailyReportVo dailyReport);

    List<WorkUnitVo> selectDailyReportWorkUnits(@Param("dailyReportIdx") Long dailyReportIdx);

    void insertDailyReportWorkUnit(
            @Param("dailyReportIdx") Long dailyReportIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("workUnitIdx") Long workUnitIdx,
            @Param("createdBy") String createdBy
    );

    int countDailyReportWorkUnits(@Param("dailyReportIdx") Long dailyReportIdx);

    int deleteDailyReportWorkUnits(@Param("dailyReportIdx") Long dailyReportIdx);

    boolean existsDailyReportByDate(
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("rptDt") LocalDate rptDt,
            @Param("excludeUuid") String excludeUuid
    );

    List<LocalDate> selectWrittenDates(
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );

    List<DailyReportAdmJvo> selectAdminDailyReportList(@Param("req") DailyReportListSearchVo req);

    long countAdminDailyReportList(@Param("req") DailyReportListSearchVo req);

    List<DailyReportAdmJvo> selectAdminMissingMembers(@Param("mbrAcctUuid") String mbrAcctUuid);

    List<WeeklyReportVo> selectWeeklyReportList(@Param("mbrAcctIdx") Long mbrAcctIdx);

    WeeklyReportVo selectWeeklyReport(@Param("uuid") String uuid, @Param("mbrAcctIdx") Long mbrAcctIdx);

    WeeklyReportVo selectWeeklyReportByIdx(@Param("idx") Long idx);

    List<OpenWeeklyReportJvo> selectOpenWeeklyReportMembers();

    List<OpenWeeklyReportJvo> selectOpenWeeklyReportList(@Param("memberUuid") String memberUuid);

    OpenWeeklyReportJvo selectOpenWeeklyReport(@Param("uuid") String uuid);

    void insertWeeklyReport(WeeklyReportVo weeklyReport);

    int updateWeeklyReport(WeeklyReportVo weeklyReport);

    List<DailyReportVo> selectWeeklySourceDailyReports(
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("weekStartDate") LocalDate weekStartDate,
            @Param("weekEndDate") LocalDate weekEndDate
    );
}
