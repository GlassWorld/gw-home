package com.gw.infra.db.mapper.work;

import com.gw.share.vo.work.WorkTodoVo;
import com.gw.share.vo.work.WorkTodoListSearchVo;
import com.gw.share.jvo.work.WorkTodoListJvo;
import com.gw.share.jvo.work.WorkTodoInboxSummaryJvo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkTodoMapper {

    Long selectWorkUnitForUpdate(@Param("workUnitIdx") Long workUnitIdx, @Param("mbrAcctIdx") Long mbrAcctIdx);

    List<WorkTodoListJvo> selectMyWorkTodos(@Param("req") WorkTodoListSearchVo request);

    long selectMyWorkTodoCount(@Param("req") WorkTodoListSearchVo request);

    WorkTodoInboxSummaryJvo selectMyWorkTodoSummary(@Param("req") WorkTodoListSearchVo request);

    int updateWorkTodoCompletion(
            @Param("uuid") String uuid, @Param("workUnitIdx") Long workUnitIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx, @Param("completed") boolean completed,
            @Param("updatedBy") String updatedBy
    );

    List<WorkTodoVo> selectWorkTodoList(
            @Param("workUnitIdx") Long workUnitIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx
    );

    WorkTodoVo selectWorkTodo(
            @Param("uuid") String uuid,
            @Param("workUnitIdx") Long workUnitIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx
    );

    Integer selectMaxSortOrder(
            @Param("workUnitIdx") Long workUnitIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("prntWrkTodoIdx") Long prntWrkTodoIdx
    );

    void insertWorkTodo(WorkTodoVo workTodo);

    int updateWorkTodo(WorkTodoVo workTodo);

    int updateWorkTodoDerivedFields(
            @Param("idx") Long idx,
            @Param("sts") String sts,
            @Param("prgsRt") Integer prgsRt,
            @Param("updatedBy") String updatedBy
    );

    int updateWorkTodoSortOrder(
            @Param("idx") Long idx,
            @Param("sortOrd") Integer sortOrd,
            @Param("updatedBy") String updatedBy
    );

    int deleteWorkTodoSubtree(
            @Param("uuid") String uuid,
            @Param("workUnitIdx") Long workUnitIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("updatedBy") String updatedBy
    );
}
