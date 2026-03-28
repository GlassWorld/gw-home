package com.gw.share.vo.vault;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CrdCatVo {

    // 개인 자격증명 PK
    private Long tbVltCrdIdx;

    // Vault 카테고리 PK
    private Long tbVltCatIdx;
}
