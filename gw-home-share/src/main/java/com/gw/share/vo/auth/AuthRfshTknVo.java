package com.gw.share.vo.auth;

import com.gw.share.vo.common.BaseVo;
import java.time.OffsetDateTime;
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
public class AuthRfshTknVo extends BaseVo {

    // 회원 계정 PK
    private Long mbrAcctIdx;

    // 리프레시 토큰 해시
    private String tknHash;

    // 만료 일시
    private OffsetDateTime exprAt;
}
