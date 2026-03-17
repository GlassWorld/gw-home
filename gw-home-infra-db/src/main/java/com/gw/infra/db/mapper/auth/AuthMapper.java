package com.gw.infra.db.mapper.auth;

import com.gw.share.vo.auth.AuthRfshTknVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {

    void insertRefreshToken(AuthRfshTknVo rfshTkn);

    AuthRfshTknVo selectActiveRefreshTokenByTokenHash(@Param("tknHash") String tknHash);

    int deleteRefreshToken(
            @Param("tknHash") String tknHash,
            @Param("mbrAcctIdx") Long mbrAcctIdx
    );
}
