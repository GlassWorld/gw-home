package com.gw.api.service.work;

import com.gw.share.common.exception.*;
import java.time.*;

final class WorkNotificationTime {
    private WorkNotificationTime() {}
    static ZoneId zone(String value) {
        try {
            if (!ZoneId.getAvailableZoneIds().contains(value)) throw new DateTimeException("zone");
            return ZoneId.of(value);
        } catch (RuntimeException failure) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,"유효한 IANA 시간대를 선택하세요.");
        }
    }
    static boolean eligible(LocalDate day,String cycle) {
        return !"WEEKDAYS".equals(cycle) || day.getDayOfWeek().getValue()<=5;
    }
    // DST 공백은 이후 유효 시각, 중복 시각은 첫 번째 오프셋으로 하루 한 번 보낸다.
    static OffsetDateTime at(LocalDate day,String time,String zone) {
        return day.atTime(LocalTime.parse(time)).atZone(zone(zone)).toOffsetDateTime();
    }
    static OffsetDateTime next(Instant now,String time,String zone,String cycle) {
        LocalDate date=now.atZone(zone(zone)).toLocalDate();
        for (int index=0;index<8;index++,date=date.plusDays(1)) {
            OffsetDateTime candidate=at(date,time,zone);
            if (eligible(date,cycle) && candidate.toInstant().isAfter(now)) return candidate;
        }
        throw new IllegalStateException("다음 요약 시각을 계산할 수 없습니다.");
    }
}
