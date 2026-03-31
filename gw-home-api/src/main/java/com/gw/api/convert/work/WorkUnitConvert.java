package com.gw.api.convert.work;

import com.gw.api.dto.work.DailyReportWorkUnitResponse;
import com.gw.api.dto.work.WorkGitProjectResponse;
import com.gw.api.dto.work.WorkUnitOptionResponse;
import com.gw.api.dto.work.WorkUnitResponse;
import com.gw.share.util.ConvertUtil;
import com.gw.share.vo.work.WorkGitPrjVo;
import com.gw.share.vo.work.WorkUnitVo;
import java.util.List;

public final class WorkUnitConvert {

    private WorkUnitConvert() {
    }

    // 업무 VO를 목록/상세 응답으로 변환한다.
    public static WorkUnitResponse toResponse(WorkUnitVo workUnit) {
        return new WorkUnitResponse(
                workUnit.getUuid(),
                workUnit.getTtl(),
                workUnit.getDscr(),
                workUnit.getCtgr(),
                workUnit.getSts(),
                workUnit.getUseYn(),
                ConvertUtil.toInteger(workUnit.getUseCnt(), 0),
                workUnit.getLastUsedAt(),
                workUnit.getCreatedAt(),
                workUnit.getUpdatedAt(),
                toGitProjectResponses(workUnit.getGitProjects())
        );
    }

    // 업무 VO를 선택 옵션 응답으로 변환한다.
    public static WorkUnitOptionResponse toOptionResponse(WorkUnitVo workUnit) {
        return new WorkUnitOptionResponse(
                workUnit.getUuid(),
                workUnit.getTtl(),
                workUnit.getCtgr(),
                workUnit.getSts(),
                workUnit.getUseYn()
        );
    }

    // 업무 VO 목록을 일일/주간보고 하위 응답으로 변환한다.
    public static List<DailyReportWorkUnitResponse> toDailyReportWorkUnitResponses(List<WorkUnitVo> workUnits) {
        if (workUnits == null || workUnits.isEmpty()) {
            return List.of();
        }

        return workUnits.stream()
                .map(workUnit -> new DailyReportWorkUnitResponse(
                        workUnit.getUuid(),
                        workUnit.getTtl(),
                        workUnit.getCtgr()
                ))
                .toList();
    }

    public static List<WorkGitProjectResponse> toGitProjectResponses(List<WorkGitPrjVo> gitProjects) {
        if (gitProjects == null || gitProjects.isEmpty()) {
            return List.of();
        }

        return gitProjects.stream()
                .map(gitProject -> new WorkGitProjectResponse(
                        gitProject.getUuid(),
                        gitProject.getGitAccountUuid(),
                        gitProject.getGitAccountLabel(),
                        gitProject.getPrvdCd(),
                        gitProject.getPrjNm(),
                        gitProject.getRepoUrl(),
                        gitProject.getUseYn(),
                        gitProject.getCreatedAt(),
                        gitProject.getUpdatedAt()
                ))
                .toList();
    }
}
