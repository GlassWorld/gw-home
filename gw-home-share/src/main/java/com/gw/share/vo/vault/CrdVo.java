package com.gw.share.vo.vault;

import com.gw.share.vo.common.BaseVo;
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
public class CrdVo extends BaseVo {

    // 제목
    private String ttl;

    // 로그인 ID
    private String lgnId;

    // 비밀번호
    private String pwd;

    // 메모
    private String memo;

    // Vault 카테고리 PK
    private Long vltCatIdx;
}
