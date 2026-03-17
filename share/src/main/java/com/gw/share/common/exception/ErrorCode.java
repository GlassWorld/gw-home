package com.gw.share.common.exception;

public enum ErrorCode {
    NOT_FOUND(404, "리소스를 찾을 수 없습니다"),
    UNAUTHORIZED(401, "인증이 필요합니다"),
    FORBIDDEN(403, "접근 권한이 없습니다"),
    BAD_REQUEST(400, "잘못된 요청입니다"),
    DUPLICATE(409, "이미 존재합니다"),
    INTERNAL_ERROR(500, "서버 오류가 발생했습니다");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
