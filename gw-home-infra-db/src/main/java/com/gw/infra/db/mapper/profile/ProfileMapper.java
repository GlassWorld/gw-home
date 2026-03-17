package com.gw.infra.db.mapper.profile;

import com.gw.share.vo.profile.PrflVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProfileMapper {

    PrflVo selectProfileByUuid(@Param("uuid") String uuid);

    PrflVo selectProfileByAccountIdx(@Param("mbrAcctIdx") Long mbrAcctIdx);

    int updateProfile(PrflVo prfl);

    void insertProfile(PrflVo prfl);
}
