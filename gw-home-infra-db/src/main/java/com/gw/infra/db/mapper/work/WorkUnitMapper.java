package com.gw.infra.db.mapper.work;

import com.gw.share.vo.work.WorkUnitListSearchVo;
import com.gw.share.vo.work.WorkUnitVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkUnitMapper {

    List<WorkUnitVo> selectWorkUnitList(@Param("req") WorkUnitListSearchVo req);

    WorkUnitVo selectWorkUnit(@Param("uuid") String uuid, @Param("mbrAcctIdx") Long mbrAcctIdx);

    WorkUnitVo selectWorkUnitByIdx(@Param("idx") Long idx);

    List<WorkUnitVo> selectWorkUnitOptions(
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("includeInactive") boolean includeInactive
    );

    List<WorkUnitVo> selectWorkUnitsByUuids(
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("uuids") List<String> uuids
    );

    void insertWorkUnit(WorkUnitVo workUnit);

    int updateWorkUnit(WorkUnitVo workUnit);

    int updateWorkUnitUse(
            @Param("uuid") String uuid,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("useYn") String useYn,
            @Param("updatedBy") String updatedBy
    );

    boolean existsTitle(
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("ttl") String ttl,
            @Param("excludeUuid") String excludeUuid
    );
}
