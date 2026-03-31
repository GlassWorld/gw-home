package com.gw.api.convert.work;

import com.gw.api.dto.work.WorkGitAccountResponse;
import com.gw.api.dto.work.WorkGitProjectResponse;
import com.gw.share.vo.work.WorkGitAcctVo;
import com.gw.share.vo.work.WorkGitPrjVo;

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
}
