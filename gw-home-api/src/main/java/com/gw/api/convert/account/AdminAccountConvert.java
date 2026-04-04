package com.gw.api.convert.account;

import com.gw.api.dto.account.AdminAccountDetailResponse;
import com.gw.api.dto.account.AdminAccountListResponse;
import com.gw.api.dto.account.AdminPasswordResetResponse;
import com.gw.share.vo.account.AcctVo;

public final class AdminAccountConvert {

    private AdminAccountConvert() {
    }

    // 관리자 계정 목록 조회 결과를 응답으로 변환한다.
    public static AdminAccountListResponse toListResponse(AcctVo account) {
        return new AdminAccountListResponse(
                account.getUuid(),
                account.getLgnId(),
                account.getEmail(),
                account.getRole(),
                account.getAcctStat(),
                account.isLckYn(),
                account.getCreatedAt()
        );
    }

    // 관리자 계정 상세 조회 결과를 응답으로 변환한다.
    public static AdminAccountDetailResponse toDetailResponse(AcctVo account) {
        return new AdminAccountDetailResponse(
                account.getUuid(),
                account.getLgnId(),
                account.getEmail(),
                account.getRole(),
                account.getAcctStat(),
                account.isLckYn(),
                account.getLckAt(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    // 관리자 비밀번호 초기화 결과를 응답으로 변환한다.
    public static AdminPasswordResetResponse toPasswordResetResponse(String temporaryPassword) {
        return new AdminPasswordResetResponse(temporaryPassword);
    }
}
