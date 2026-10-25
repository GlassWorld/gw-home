package com.gw.infra.db.mapper.work;

import com.gw.share.jvo.work.WorkGanttRowJvo;
import com.gw.share.jvo.work.WorkInsightUnitJvo;
import com.gw.share.jvo.work.WorkCompletionBucketJvo;
import com.gw.share.vo.work.WorkTodoListSearchVo;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkInsightsMapper {
    List<WorkGanttRowJvo> selectGanttRows(@Param("req") WorkTodoListSearchVo search,
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
    List<WorkInsightUnitJvo> selectUnitInsights(@Param("req") WorkTodoListSearchVo search,
            @Param("fromInstant") OffsetDateTime fromInstant, @Param("beforeInstant") OffsetDateTime beforeInstant);
    List<WorkCompletionBucketJvo> selectCompletionBuckets(@Param("req") WorkTodoListSearchVo search,
            @Param("fromInstant") OffsetDateTime fromInstant, @Param("beforeInstant") OffsetDateTime beforeInstant,
            @Param("timeZone") String timeZone, @Param("bucket") String bucket);
}
