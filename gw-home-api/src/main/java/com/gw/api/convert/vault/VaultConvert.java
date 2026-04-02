package com.gw.api.convert.vault;

import com.gw.api.dto.vault.CredentialCategoryResponse;
import com.gw.api.dto.vault.CredentialResponse;
import com.gw.share.vo.vault.CatVo;
import com.gw.share.vo.vault.CrdVo;
import java.util.List;
import java.util.Map;

public final class VaultConvert {

    private VaultConvert() {
    }

    // 자격증명 VO를 상세 응답으로 변환한다.
    public static CredentialResponse toResponse(CrdVo credential, Map<Long, List<CatVo>> categoriesByCredentialIdx) {
        List<CredentialCategoryResponse> categories = categoriesByCredentialIdx
                .getOrDefault(credential.getIdx(), List.of())
                .stream()
                .map(category -> new CredentialCategoryResponse(
                        category.getUuid(),
                        category.getNm(),
                        category.getColor()
                ))
                .toList();

        return new CredentialResponse(
                credential.getUuid(),
                credential.getTtl(),
                categories,
                credential.getLgnId(),
                credential.getPwd(),
                credential.getMemo(),
                credential.getCreatedAt()
        );
    }
}
