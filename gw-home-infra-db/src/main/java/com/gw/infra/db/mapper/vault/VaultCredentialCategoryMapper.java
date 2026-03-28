package com.gw.infra.db.mapper.vault;

import com.gw.share.vo.vault.CrdCatVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VaultCredentialCategoryMapper {

    List<CrdCatVo> selectCredentialCategoryMappings(@Param("credentialIdxList") List<Long> credentialIdxList);

    List<CrdCatVo> selectCredentialCategoryMappingsByCredentialIdx(@Param("credentialIdx") Long credentialIdx);

    int deleteCredentialCategoryMappings(@Param("credentialIdx") Long credentialIdx);

    int deleteCategoryMappings(@Param("categoryIdx") Long categoryIdx);

    void insertCredentialCategoryMappings(@Param("credentialIdx") Long credentialIdx, @Param("categoryIdxList") List<Long> categoryIdxList);
}
