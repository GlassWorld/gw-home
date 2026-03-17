package com.gw.api.account.mapper;

import com.gw.api.account.dto.AccountDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {

    void insertAccount(AccountDto account);

    AccountDto selectAccountByLoginId(@Param("loginId") String loginId);

    AccountDto selectAccountByUuid(@Param("uuid") String uuid);

    int updateAccount(AccountDto account);

    int deleteAccount(@Param("uuid") String uuid);

    boolean existsByLoginId(@Param("loginId") String loginId);

    boolean existsByEmail(@Param("email") String email);
}
