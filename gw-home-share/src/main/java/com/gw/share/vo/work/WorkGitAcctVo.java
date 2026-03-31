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
public class WorkGitAcctVo extends BaseVo {

    private Long mbrAcctIdx;
    private String prvdCd;
    private String acctLbl;
    private String authNm;
    private String acsToknEnc;
    private String useYn;
}
