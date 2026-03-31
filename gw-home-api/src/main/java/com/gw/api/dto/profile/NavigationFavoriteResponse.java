package com.gw.api.dto.profile;

import java.util.List;

public record NavigationFavoriteResponse(
        List<String> favoriteMenus
) {
}
