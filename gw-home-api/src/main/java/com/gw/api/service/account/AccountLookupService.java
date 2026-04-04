package com.gw.api.service.account;

import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.account.AcctVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class AccountLookupService {

    private final AccountMapper accountMapper;

    public AccountLookupService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    // 로그인 ID로 회원 계정을 조회한다.
    public AcctVo getAccountByLoginId(String loginId) {
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
            log.error("getAccountByLoginId 실패 - 원인: 계정을 찾을 수 없습니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        return account;
    }

    // UUID로 회원 계정을 조회한다.
    public AcctVo getAccountByUuid(String uuid) {
        AcctVo account = accountMapper.selectAccountByUuid(uuid);

        if (account == null) {
            log.error("getAccountByUuid 실패 - 원인: 계정을 찾을 수 없습니다. uuid={}", uuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        return account;
    }
}
