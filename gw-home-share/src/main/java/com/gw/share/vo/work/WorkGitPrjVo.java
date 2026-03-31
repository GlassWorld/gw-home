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
public class WorkGitPrjVo extends BaseVo {

    private Long workUnitIdx;
    private Long wrkGitAcctIdx;
    private Long mbrAcctIdx;
    private String prjNm;
    private String repoUrl;
    private String useYn;
    private String gitAccountUuid;
    private String gitAccountLabel;
    private String prvdCd;
    private String authNm;
    private String acsToknEnc;
}
