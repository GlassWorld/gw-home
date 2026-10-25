package com.gw.share.common.policy;

import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import java.time.*;
import java.util.Set;

public final class LifeTodoPolicy {
    public static final String DEFAULT_TIME_ZONE = "Asia/Seoul";
    public static final Set<String> GROUPS = Set.of("ALL", "TODAY", "UPCOMING", "OVERDUE", "COMPLETED", "UNSCHEDULED");
    private LifeTodoPolicy() { }

    // 일상 일정의 날짜 경계를 사용자가 선택한 시간대로 계산한다.
    public static LocalDate today(String timeZone) {
        return dateAt(Instant.now(), timeZone);
    }

    // 기준 시각을 현지 날짜로 변환하고 잘못된 시간대를 거부한다.
    public static LocalDate dateAt(Instant instant, String timeZone) {
        try {
            return instant.atZone(ZoneId.of(timeZone == null ? DEFAULT_TIME_ZONE : timeZone)).toLocalDate();
        } catch (DateTimeException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "올바른 시간대를 지정해주세요.");
        }
    }
}
