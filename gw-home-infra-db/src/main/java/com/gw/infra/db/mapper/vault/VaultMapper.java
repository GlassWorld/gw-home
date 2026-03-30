package com.gw.infra.db.mapper.vault;

import com.gw.share.vo.vault.CrdVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VaultMapper {

    List<CrdVo> selectCredentialList(
            @Param("keywordTokens") List<String> keywordTokens,
            @Param("categoryUuid") String categoryUuid,
            @Param("mbrAcctIdx") Long mbrAcctIdx
    );

    CrdVo selectCredentialByIdx(@Param("idx") Long idx);

    CrdVo selectCredential(@Param("uuid") String uuid, @Param("mbrAcctIdx") Long mbrAcctIdx);

    void insertCredential(CrdVo credential);

    int updateCredential(CrdVo credential);

    int deleteCredential(
            @Param("uuid") String uuid,
            @Param("mbrAcctIdx") Long mbrAcctIdx,
            @Param("updatedBy") String updatedBy
    );
}
