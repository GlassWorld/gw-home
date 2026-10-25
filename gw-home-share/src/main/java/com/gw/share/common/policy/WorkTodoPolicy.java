package com.gw.share.common.policy;

import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

public final class WorkTodoPolicy {
    public static final String DEFAULT_TIME_ZONE = "Asia/Seoul";
    public static final Set<String> GROUPS = Set.of("ALL", "TODAY", "UPCOMING", "OVERDUE", "COMPLETED", "UNSCHEDULED");

    private WorkTodoPolicy() { }

    // 유효한 IANA 시간대에서 기준 시각의 달력 날짜를 구한다.
    public static LocalDate today(String timeZone) {
        return dateAt(Instant.now(), timeZone);
    }

    // 날짜 경계 계산을 트리와 통합 조회에서 동일하게 사용한다.
    public static LocalDate dateAt(Instant instant, String timeZone) {
        try {
            return instant.atZone(ZoneId.of(timeZone == null ? DEFAULT_TIME_ZONE : timeZone)).toLocalDate();
        } catch (DateTimeException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "올바른 시간대를 지정해주세요.");
        }
    }
}
