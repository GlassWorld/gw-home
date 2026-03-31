package com.gw.share.vo.profile;

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
public class PrflVo extends BaseVo {

    // 회원 계정 PK
    private Long mbrAcctIdx;

    // 닉네임
    private String nickNm;

    // 자기소개
    private String intro;

    // 프로필 이미지 URL
    private String prflImgUrl;

    // 개인 메모
    private String memo;

    // 헤더 즐겨찾기 메뉴 JSON
    private String favMenuJson;
}
