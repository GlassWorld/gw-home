package com.gw.infra.db.mapper.work;

import com.gw.share.vo.work.WorkGitAcctVo;
import com.gw.share.vo.work.WorkGitPrjVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkGitMapper {

    List<WorkGitAcctVo> selectGitAccounts(@Param("mbrAcctIdx") Long mbrAcctIdx);

    WorkGitAcctVo selectGitAccount(@Param("uuid") String uuid, @Param("mbrAcctIdx") Long mbrAcctIdx);

    WorkGitAcctVo selectGitAccountByIdx(@Param("idx") Long idx, @Param("mbrAcctIdx") Long mbrAcctIdx);

    void insertGitAccount(WorkGitAcctVo gitAccount);

    int updateGitAccount(WorkGitAcctVo gitAccount);

    int deleteGitAccount(
            @Param("uuid") String uuid,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("updatedBy") String updatedBy
    );

    List<WorkGitPrjVo> selectGitProjects(
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("gitAccountUuid") String gitAccountUuid
    );

    List<WorkGitPrjVo> selectGitProjectOptions(@Param("mbrAcctIdx") Long mbrAcctIdx);

    WorkGitPrjVo selectGitProject(@Param("uuid") String uuid, @Param("mbrAcctIdx") Long mbrAcctIdx);

    WorkGitPrjVo selectGitProjectByIdx(@Param("idx") Long idx, @Param("mbrAcctIdx") Long mbrAcctIdx);

    List<WorkGitPrjVo> selectGitProjectsByUuids(
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("uuids") List<String> uuids
    );

    void insertGitProject(WorkGitPrjVo gitProject);

    int updateGitProject(WorkGitPrjVo gitProject);

    int deleteGitProject(
            @Param("uuid") String uuid,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("updatedBy") String updatedBy
    );

    int deleteGitProjectsByAccount(
            @Param("wrkGitAcctIdx") Long wrkGitAcctIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("updatedBy") String updatedBy
    );

    List<WorkGitPrjVo> selectWorkUnitGitProjects(
            @Param("workUnitIdx") Long workUnitIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx
    );

    List<WorkGitPrjVo> selectWorkUnitGitProjectsByWorkUnitIdxs(
            @Param("workUnitIdxs") List<Long> workUnitIdxs,
            @Param("mbrAcctIdx") Long mbrAcctIdx
    );

    void insertWorkUnitGitProject(
            @Param("workUnitIdx") Long workUnitIdx,
            @Param("gitProjectIdx") Long gitProjectIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("createdBy") String createdBy
    );

    int deleteWorkUnitGitProjects(
            @Param("workUnitIdx") Long workUnitIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("updatedBy") String updatedBy
    );

    int deleteWorkUnitGitProjectsByGitProject(
            @Param("gitProjectIdx") Long gitProjectIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("updatedBy") String updatedBy
    );

    int deleteWorkUnitGitProjectsByGitAccount(
            @Param("wrkGitAcctIdx") Long wrkGitAcctIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("updatedBy") String updatedBy
    );
}
