package com.gw.api.convert.vault;

import com.gw.api.dto.vault.VaultCategoryResponse;
import com.gw.share.vo.vault.CatVo;

public final class VaultCategoryConvert {

    private VaultCategoryConvert() {
    }

    // 카테고리 VO를 카테고리 응답으로 변환한다.
    public static VaultCategoryResponse toResponse(CatVo category) {
        return new VaultCategoryResponse(
                category.getUuid(),
                category.getNm(),
                category.getDsc(),
                category.getColor(),
                category.getSortOrd()
        );
    }
}
