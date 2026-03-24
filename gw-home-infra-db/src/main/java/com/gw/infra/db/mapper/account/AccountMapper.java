package com.gw.infra.db.mapper.account;

import com.gw.share.vo.account.AcctVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {

    void insertAccount(AcctVo account);

    AcctVo selectAccountByLoginId(@Param("lgnId") String lgnId);

    AcctVo selectAccountByUuid(@Param("uuid") String uuid);

    AcctVo selectAccountByIdx(@Param("idx") Long idx);

    int updateAccount(AcctVo account);

    int deleteAccount(@Param("uuid") String uuid);

    boolean existsByLoginId(@Param("lgnId") String lgnId);

    boolean existsByEmail(@Param("email") String email);

    int incrementLoginFailCount(@Param("idx") Long idx);

    int lockAccount(@Param("idx") Long idx);

    int resetLoginFailCount(@Param("idx") Long idx);
}
