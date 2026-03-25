package com.gw.infra.db.mapper.vault;

import com.gw.share.vo.vault.CatVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VaultCategoryMapper {

    List<CatVo> selectCategoryList();

    CatVo selectCategoryByIdx(@Param("idx") Long idx);

    CatVo selectCategory(@Param("uuid") String uuid);

    boolean existsByName(
            @Param("name") String name,
            @Param("excludeUuid") String excludeUuid
    );

    void insertCategory(CatVo category);

    int updateCategory(CatVo category);

    int deleteCategory(
            @Param("uuid") String uuid,
            @Param("updatedBy") String updatedBy
    );
}
