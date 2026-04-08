package com.gw.share.vo.board;

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
public class BrdPstShrVo extends BaseVo {

    private Long brdPstIdx;

    private String shrTkn;

    private String pwdHash;

    private String pwdUseYn;

    private String actvYn;

    private OffsetDateTime exprAt;

    private OffsetDateTime rvkdAt;
}
