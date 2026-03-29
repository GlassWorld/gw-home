package com.gw.share.util;

public final class ConvertUtil {

    private ConvertUtil() {
    }

    // nullable Integer 값을 기본값으로 변환한다.
    public static Integer toInteger(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}
