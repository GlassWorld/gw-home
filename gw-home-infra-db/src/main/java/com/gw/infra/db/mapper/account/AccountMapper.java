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

    java.util.List<AcctVo> selectAllAccounts(
            @Param("loginId") String loginId,
            @Param("role") String role,
            @Param("acctStat") String acctStat,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    long countAllAccounts(
            @Param("loginId") String loginId,
            @Param("role") String role,
            @Param("acctStat") String acctStat
    );

    int updateAccount(AcctVo account);

    int deleteAccount(@Param("uuid") String uuid);

    boolean existsByLoginId(@Param("lgnId") String lgnId);

    boolean existsByEmail(@Param("email") String email);

    int incrementLoginFailCount(@Param("idx") Long idx);

    int lockAccount(@Param("idx") Long idx);

    int resetLoginFailCount(@Param("idx") Long idx);

    int unlockAccountByUuid(
            @Param("uuid") String uuid,
            @Param("updatedBy") String updatedBy
    );

    int updateOtpSecret(@Param("idx") Long idx, @Param("otpSecret") String otpSecret);

    int enableOtp(@Param("idx") Long idx);

    int disableOtp(@Param("idx") Long idx);

    int incrementOtpFailCount(@Param("idx") Long idx);

    int resetOtpFailCount(@Param("idx") Long idx);

    int resetOtpFailureByUuid(
            @Param("uuid") String uuid,
            @Param("updatedBy") String updatedBy
    );

    int resetOtpByUuid(
            @Param("uuid") String uuid,
            @Param("updatedBy") String updatedBy
    );

    int updatePassword(
            @Param("idx") Long idx,
            @Param("pwd") String pwd,
            @Param("updatedBy") String updatedBy
    );

    int updatePasswordByUuid(
            @Param("uuid") String uuid,
            @Param("pwd") String pwd,
            @Param("updatedBy") String updatedBy
    );

    int updateRole(
            @Param("uuid") String uuid,
            @Param("role") String role,
            @Param("updatedBy") String updatedBy
    );

    int updateStatus(
            @Param("uuid") String uuid,
            @Param("acctStat") String acctStat,
            @Param("updatedBy") String updatedBy
    );

    int updateOtpRequired(
            @Param("uuid") String uuid,
            @Param("otpRequired") boolean otpRequired,
            @Param("updatedBy") String updatedBy
    );
}
