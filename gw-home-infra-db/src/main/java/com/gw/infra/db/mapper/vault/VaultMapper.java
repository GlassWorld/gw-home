package com.gw.infra.db.mapper.vault;

import com.gw.share.vo.vault.CrdVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VaultMapper {

    List<CrdVo> selectCredentialList(
            @Param("keyword") String keyword,
            @Param("categoryUuid") String categoryUuid,
            @Param("createdBy") String createdBy
    );

    CrdVo selectCredentialByIdx(@Param("idx") Long idx);

    CrdVo selectCredential(@Param("uuid") String uuid, @Param("createdBy") String createdBy);

    void insertCredential(CrdVo credential);

    int updateCredential(CrdVo credential);

    int clearCredentialCategory(@Param("vltCatIdx") Long vltCatIdx);

    int deleteCredential(
            @Param("uuid") String uuid,
            @Param("createdBy") String createdBy,
            @Param("updatedBy") String updatedBy
    );
}
