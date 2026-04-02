package com.gw.api.convert.account;

import com.gw.api.dto.account.AccountResponse;
import com.gw.share.vo.account.AcctVo;

public final class AccountConvert {

    private AccountConvert() {
    }

    // 계정 VO를 계정 응답으로 변환한다.
    public static AccountResponse toResponse(AcctVo account) {
        return new AccountResponse(
                account.getUuid(),
                account.getLgnId(),
                account.getEmail(),
                account.getRole(),
                account.getCreatedAt()
        );
    }
}
