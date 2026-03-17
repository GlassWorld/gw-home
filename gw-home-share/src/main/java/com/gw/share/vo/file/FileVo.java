package com.gw.share.vo.file;

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
public class FileVo extends BaseVo {

    // 원본 파일명
    private String orgnlNm;

    // 저장 파일명
    private String strgNm;

    // 파일 저장 경로
    private String filePath;

    // 파일 URL
    private String fileUrl;

    // MIME 타입
    private String mimeType;

    // 파일 크기
    private Long fileSize;

    // 업로더 타입
    private String upldrType;
}
