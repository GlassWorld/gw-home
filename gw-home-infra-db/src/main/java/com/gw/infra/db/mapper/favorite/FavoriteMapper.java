package com.gw.infra.db.mapper.favorite;

import com.gw.share.vo.favorite.FavVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteMapper {

    void insertFavorite(FavVo favorite);

    int deleteFavorite(
            @Param("trgtType") String trgtType,
            @Param("trgtIdx") Long trgtIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx
    );

    boolean existsFavorite(
            @Param("trgtType") String trgtType,
            @Param("trgtIdx") Long trgtIdx,
            @Param("mbrAcctIdx") Long mbrAcctIdx
    );

    long countFavorite(
            @Param("trgtType") String trgtType,
            @Param("trgtIdx") Long trgtIdx
    );
}
