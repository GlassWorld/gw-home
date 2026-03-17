package com.gw.share.jvo.admin;

import com.gw.share.vo.account.AcctVo;
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
public class AdminMbrJvo extends AcctVo {

    // 닉네임
    private String nickNm;

    // 자기소개
    private String intro;
}
