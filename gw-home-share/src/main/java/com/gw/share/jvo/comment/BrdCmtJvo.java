package com.gw.share.jvo.comment;

import com.gw.share.vo.comment.BrdCmtVo;
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
public class BrdCmtJvo extends BrdCmtVo {

    // 부모 댓글 UUID
    private String prntBrdCmtUuid;

    // 작성자 닉네임
    private String athrNickNm;
}
