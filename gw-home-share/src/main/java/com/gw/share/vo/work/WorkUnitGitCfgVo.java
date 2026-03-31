package com.gw.share.vo.work;

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
public class WorkUnitGitCfgVo extends BaseVo {

    // 업무 PK
    private Long workUnitIdx;

    // 회원 계정 PK
    private Long mbrAcctIdx;

    // provider 코드
    private String prvdCd;

    // 저장소 URL
    private String repoUrl;

    // commit author 이름
    private String authNm;

    // 암호화 저장된 access token
    private String acsToknEnc;
}
