package com.gw.api.convert.profile;

import com.gw.api.dto.profile.MemoResponse;
import com.gw.api.dto.profile.NavigationFavoriteResponse;
import com.gw.api.dto.profile.ProfileResponse;
import com.gw.share.vo.profile.PrflVo;
import java.util.List;

public final class ProfileConvert {

    private ProfileConvert() {
    }

    // 프로필 VO를 프로필 응답으로 변환한다.
    public static ProfileResponse toResponse(PrflVo profile) {
        return new ProfileResponse(
                profile.getUuid(),
                profile.getNickNm(),
                profile.getIntro(),
                profile.getPrflImgUrl(),
                profile.getCreatedAt()
        );
    }

    // 메모 문자열을 메모 응답으로 변환한다.
    public static MemoResponse toMemoResponse(String memo) {
        return new MemoResponse(memo == null ? "" : memo);
    }

    // 즐겨찾기 메뉴 목록을 응답으로 변환한다.
    public static NavigationFavoriteResponse toNavigationFavoriteResponse(List<String> favoriteMenus) {
        return new NavigationFavoriteResponse(favoriteMenus);
    }
}
