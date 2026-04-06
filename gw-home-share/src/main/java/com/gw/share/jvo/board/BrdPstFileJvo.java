package com.gw.share.jvo.board;

import com.gw.share.vo.board.BrdPstFileVo;
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
public class BrdPstFileJvo extends BrdPstFileVo {

    // 파일 UUID
    private String fileUuid;

    // 원본 파일명
    private String orgnlNm;

    // 파일 URL
    private String fileUrl;

    // MIME 타입
    private String mimeType;

    // 파일 크기
    private Long fileSize;

    // 업로더 타입
    private String upldrType;
}
