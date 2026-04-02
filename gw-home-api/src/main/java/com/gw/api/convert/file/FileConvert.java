package com.gw.api.convert.file;

import com.gw.api.dto.file.FileUploadResponse;
import com.gw.share.vo.file.FileVo;

public final class FileConvert {

    private FileConvert() {
    }

    // 파일 VO를 업로드 응답으로 변환한다.
    public static FileUploadResponse toResponse(FileVo file) {
        return new FileUploadResponse(
                file.getUuid(),
                file.getFileUrl(),
                file.getOrgnlNm(),
                file.getMimeType(),
                file.getFileSize()
        );
    }
}
