package com.gw.share.jvo.admin;

import com.gw.share.jvo.board.BrdPstJvo;
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
public class AdminBrdPstJvo extends BrdPstJvo {

    // 로그인 ID
    private String lgnId;
}
