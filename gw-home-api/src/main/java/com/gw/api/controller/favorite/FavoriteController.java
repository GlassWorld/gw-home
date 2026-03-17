package com.gw.api.controller.favorite;

import com.gw.api.dto.favorite.FavoriteResponse;
import com.gw.api.service.favorite.FavoriteService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/boards/{boardPostUuid}/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    public ApiResponse<FavoriteResponse> toggleFavorite(Principal principal, @PathVariable String boardPostUuid) {
        return ApiResponse.ok(favoriteService.toggleBoardPostFavorite(getLoginId(principal), boardPostUuid));
    }

    @GetMapping("/count")
    public ApiResponse<FavoriteResponse> getFavoriteCount(@PathVariable String boardPostUuid) {
        return ApiResponse.ok(favoriteService.getBoardPostFavoriteCount(boardPostUuid));
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
