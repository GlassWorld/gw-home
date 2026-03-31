package com.gw.api.dto.profile;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveNavigationFavoriteRequest(
        @JsonAlias("favorite_menus")
        @Size(max = 5, message = "favoriteMenus는 5개 이하여야 합니다.")
        List<String> favoriteMenus
) {
}
