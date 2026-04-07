package com.gw.infra.db.mapper.work;

import com.gw.share.vo.work.WorkTodoVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkTodoMapper {

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
