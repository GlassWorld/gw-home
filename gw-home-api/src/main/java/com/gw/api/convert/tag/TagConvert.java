package com.gw.api.convert.tag;

import com.gw.api.dto.tag.TagResponse;
import com.gw.share.vo.tag.TagVo;

public final class TagConvert {

    private TagConvert() {
    }

    // 태그 VO를 태그 응답으로 변환한다.
    public static TagResponse toResponse(TagVo tag) {
        return new TagResponse(tag.getUuid(), tag.getNm());
    }
}
