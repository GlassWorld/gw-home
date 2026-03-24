package com.gw.share.vo.account;

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
public class AcctVo extends BaseVo {

    // 로그인 ID
    private String lgnId;

    // 비밀번호 해시
    private String pwd;

    // 이메일
    private String email;

    // 권한
    private String role;

    // 로그인 실패 횟수
    private int lgnFailCnt;

    // 계정 잠금 여부
    private boolean lckYn;

    // 계정 잠금 일시
    private OffsetDateTime lckAt;
}
