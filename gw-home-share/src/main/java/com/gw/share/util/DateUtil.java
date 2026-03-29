package com.gw.share.util;

import com.gw.share.common.exception.ErrorCode;
import java.time.DayOfWeek;
import java.time.LocalDate;

public final class DateUtil {

    private DateUtil() {
    }

    // 시작일과 종료일의 기본 순서를 검증한다.
    public static void validateDateOrder(
            LocalDate startDate,
            LocalDate endDate,
            ErrorCode errorCode,
            String detailMessage
    ) {
        ValidationUtil.requireTrue(!startDate.isAfter(endDate), errorCode, detailMessage);
    }

    // 주말 여부를 공통 기준으로 확인한다.
    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
