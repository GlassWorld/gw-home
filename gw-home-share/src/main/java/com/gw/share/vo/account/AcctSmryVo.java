package com.gw.share.vo.account;

import com.gw.share.vo.common.BaseVo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// 관리자 계정 목록에 필요한 공개 상태 정보만 담는다.
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AcctSmryVo extends BaseVo {
    private String lgnId;
    private String email;
    private String role;
    private String acctStat;
    private boolean lckYn;
    private boolean otpRequired;
    private boolean otpEnabled;
}
