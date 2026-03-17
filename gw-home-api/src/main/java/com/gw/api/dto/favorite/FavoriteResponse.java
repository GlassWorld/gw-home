package com.gw.api.dto.favorite;

public record FavoriteResponse(
        boolean favorited,
        long favoriteCount
) {
}
