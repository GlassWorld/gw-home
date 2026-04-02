package com.gw.api.convert.profile;

import com.gw.api.dto.profile.ProfileResponse;
import com.gw.share.vo.profile.PrflVo;

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
}
