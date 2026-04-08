package com.gw.share.jvo.board;

import com.gw.share.vo.board.BrdPstShrVo;
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
public class BrdPstShrJvo extends BrdPstShrVo {

    private String brdPstUuid;

    private String ctgrNm;

    private String ttl;

    private String cntnt;

    private String athrNickNm;

    private OffsetDateTime brdPstCreatedAt;

    private OffsetDateTime brdPstUpdatedAt;

    private OffsetDateTime brdPstDelAt;
}
