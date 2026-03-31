package com.gw.share.vo.notice;

import com.gw.share.common.query.PageSortSearchVo;
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
public class NtcListSrchVo extends PageSortSearchVo {

    private String kwd;
}
