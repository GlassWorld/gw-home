package com.gw.infra.db.mapper.life;

import com.gw.share.vo.life.LifeDailyVo;
import com.gw.share.jvo.life.LifeDailyCheckJvo;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LifeDailyMapper {
    Long lockOwner(@Param("owner") Long owner);
    List<LifeDailyVo> selectItems(@Param("owner") Long owner, @Param("start") LocalDate start, @Param("end") LocalDate end);
    List<LifeDailyCheckJvo> selectChecks(@Param("owner") Long owner, @Param("start") LocalDate start, @Param("end") LocalDate end);
    LifeDailyVo selectItem(@Param("owner") Long owner, @Param("uuid") String uuid);
    int insertItem(LifeDailyVo item);
    int updateTitle(@Param("owner") Long owner, @Param("uuid") String uuid, @Param("title") String title, @Param("loginId") String loginId);
    int endItem(@Param("owner") Long owner, @Param("uuid") String uuid, @Param("today") LocalDate today, @Param("loginId") String loginId);
    int saveCheck(@Param("owner") Long owner, @Param("uuid") String uuid, @Param("date") LocalDate date,
                  @Param("completed") boolean completed, @Param("loginId") String loginId);
}
