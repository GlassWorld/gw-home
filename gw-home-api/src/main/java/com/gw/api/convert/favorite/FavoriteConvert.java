package com.gw.api.convert.favorite;

import com.gw.api.dto.favorite.FavoriteResponse;

public final class FavoriteConvert {

    private FavoriteConvert() {
    }

    // 즐겨찾기 상태와 개수를 응답으로 변환한다.
    public static FavoriteResponse toResponse(boolean favorited, long favoriteCount) {
        return new FavoriteResponse(favorited, favoriteCount);
    }
}
