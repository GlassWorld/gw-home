package com.gw.share.vo.board;

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
public class BrdPstListSrchVo extends PageSortSearchVo {

    private String ctgrUuid;
    private String kwd;
}
