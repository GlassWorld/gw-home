package com.gw.infra.db.mapper.life;

import com.gw.share.jvo.life.*;
import com.gw.share.vo.life.*;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LifeTodoMapper {
    Long lockOwner(@Param("owner") Long owner);
    List<LifeTodoJvo> selectTodos(@Param("req") LifeTodoSearchVo request);
    long selectCount(@Param("req") LifeTodoSearchVo request);
    LifeTodoSummaryJvo selectSummary(@Param("req") LifeTodoSearchVo request);
    LifeTodoJvo selectTodo(@Param("owner") Long owner, @Param("uuid") String uuid, @Param("today") LocalDate today);
    void insertTodo(LifeTodoVo todo);
    int updateTodo(LifeTodoVo todo);
    int updateCompletion(@Param("owner") Long owner, @Param("uuid") String uuid, @Param("completed") boolean completed, @Param("loginId") String loginId);
    int deleteTodo(@Param("owner") Long owner, @Param("uuid") String uuid, @Param("loginId") String loginId);
    List<LifeTodoCategoryVo> selectCategories(@Param("owner") Long owner);
    LifeTodoCategoryVo selectCategory(@Param("owner") Long owner, @Param("uuid") String uuid);
    boolean categoryNameExists(@Param("owner") Long owner, @Param("name") String name, @Param("exceptUuid") String exceptUuid);
    void insertCategory(LifeTodoCategoryVo category);
    int updateCategory(LifeTodoCategoryVo category);
    int clearCategory(@Param("owner") Long owner, @Param("category") Long category, @Param("loginId") String loginId);
    int deleteCategory(@Param("owner") Long owner, @Param("uuid") String uuid, @Param("loginId") String loginId);
}
