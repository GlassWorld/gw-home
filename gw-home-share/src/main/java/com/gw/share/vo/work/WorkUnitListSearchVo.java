package com.gw.share.vo.work;

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
public class WorkUnitListSearchVo {

    private Long mbrAcctIdx;

    private String kwd;

    private String ctgr;

    private String sts;

    private String useYn;

    private String orderByClause;
}
