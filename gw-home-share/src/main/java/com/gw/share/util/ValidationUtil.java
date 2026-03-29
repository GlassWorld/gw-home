package com.gw.share.util;

import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    // null 허용 여부를 공통 예외 규칙으로 검증한다.
    public static <T> T requireNonNull(T value, ErrorCode errorCode, String detailMessage) {
        if (value == null) {
            throw new BusinessException(errorCode, detailMessage);
        }

        return value;
    }

    // 조건식을 공통 예외 규칙으로 검증한다.
    public static void requireTrue(boolean condition, ErrorCode errorCode, String detailMessage) {
        if (!condition) {
            throw new BusinessException(errorCode, detailMessage);
        }
    }
}
