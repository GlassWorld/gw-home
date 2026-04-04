package com.gw.api.convert.work;

import com.gw.api.dto.work.WorkGitAccountResponse;
import com.gw.api.dto.work.WorkGitConnectionTestResponse;
import com.gw.api.dto.work.WorkGitProjectResponse;
import com.gw.api.dto.work.WorkUnitGitCommitResponse;
import com.gw.share.vo.work.WorkGitAcctVo;
import com.gw.share.vo.work.WorkGitPrjVo;
import java.time.OffsetDateTime;

public final class WorkGitConvert {

    private WorkGitConvert() {
    }

    public static WorkGitAccountResponse toAccountResponse(WorkGitAcctVo gitAccount) {
        return new WorkGitAccountResponse(
                gitAccount.getUuid(),
                gitAccount.getPrvdCd(),
                gitAccount.getAcctLbl(),
                gitAccount.getAuthNm(),
                gitAccount.getAcsToknEnc() != null && !gitAccount.getAcsToknEnc().isBlank(),
                gitAccount.getUseYn(),
                gitAccount.getCreatedAt(),
                gitAccount.getUpdatedAt()
        );
    }

    public static WorkGitProjectResponse toProjectResponse(WorkGitPrjVo gitProject) {
        return new WorkGitProjectResponse(
                gitProject.getUuid(),
                gitProject.getGitAccountUuid(),
                gitProject.getGitAccountLabel(),
                gitProject.getPrvdCd(),
                gitProject.getPrjNm(),
                gitProject.getRepoUrl(),
                gitProject.getUseYn(),
                gitProject.getCreatedAt(),
                gitProject.getUpdatedAt()
        );
    }

    // Git 프로젝트 연결 테스트 결과를 응답으로 변환한다.
    public static WorkGitConnectionTestResponse toConnectionTestResponse(
            WorkGitPrjVo gitProject,
            boolean connected,
            String message,
            OffsetDateTime checkedAt
    ) {
        return new WorkGitConnectionTestResponse(
                gitProject.getUuid(),
                gitProject.getPrvdCd(),
                gitProject.getPrjNm(),
                gitProject.getRepoUrl(),
                connected,
                message,
                checkedAt
        );
    }

    // Git 커밋 조회 결과를 응답으로 변환한다.
    public static WorkUnitGitCommitResponse toCommitResponse(
            WorkGitPrjVo gitProject,
            String commitSha,
            String message,
            String authorName,
            OffsetDateTime authoredAt,
            String commitUrl
    ) {
        return new WorkUnitGitCommitResponse(
                gitProject.getUuid(),
                gitProject.getPrvdCd(),
                gitProject.getRepoUrl(),
                gitProject.getPrjNm(),
                commitSha,
                message,
                authorName,
                authoredAt,
                commitUrl
        );
    }
}
