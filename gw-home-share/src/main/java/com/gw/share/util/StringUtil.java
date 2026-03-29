package com.gw.share.util;

public final class StringUtil {

    private StringUtil() {
    }

    // 공백 문자열을 정리해 비어 있으면 null로 변환한다.
    public static String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
