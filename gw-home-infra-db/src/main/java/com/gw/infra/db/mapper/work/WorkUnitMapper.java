package com.gw.infra.db.mapper.work;

import com.gw.share.vo.work.WorkUnitListSearchVo;
import com.gw.share.vo.work.WorkUnitGitCfgVo;
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

    List<WorkUnitGitCfgVo> selectWorkUnitGitConfigs(
            @Param("workUnitIdx") Long workUnitIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx
    );

    List<WorkUnitGitCfgVo> selectWorkUnitGitConfigsByWorkUnitIdxs(
            @Param("workUnitIdxs") List<Long> workUnitIdxs,
            @Param("mbrAcctIdx") Long mbrAcctIdx
    );

    void insertWorkUnit(WorkUnitVo workUnit);

    void insertWorkUnitGitConfig(WorkUnitGitCfgVo workUnitGitCfg);

    int updateWorkUnit(WorkUnitVo workUnit);

    int updateWorkUnitGitConfig(WorkUnitGitCfgVo workUnitGitCfg);

    int deleteWorkUnitGitConfig(
            @Param("uuid") String uuid,
            @Param("workUnitIdx") Long workUnitIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("updatedBy") String updatedBy
    );

    int updateWorkUnitUse(
            @Param("uuid") String uuid,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("useYn") String useYn,
            @Param("updatedBy") String updatedBy
    );

    int refreshWorkUnitUsageStats(
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("workUnitIdxs") List<Long> workUnitIdxs
    );

    boolean existsTitle(
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("ttl") String ttl,
            @Param("excludeUuid") String excludeUuid
    );
}
