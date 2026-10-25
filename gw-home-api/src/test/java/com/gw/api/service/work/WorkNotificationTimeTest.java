package com.gw.api.service.work;

import com.gw.share.common.exception.BusinessException;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkNotificationTimeTest {
    @Test void nextDigestUsesConfiguredLocalDateAndSkipsWeekends() {
        assertEquals(Instant.parse("2026-10-26T00:00:00Z"),
                WorkNotificationTime.next(Instant.parse("2026-10-23T00:00:00Z"),"09:00","Asia/Seoul","WEEKDAYS").toInstant());
        assertEquals(Instant.parse("2026-10-25T00:00:00Z"),
                WorkNotificationTime.next(Instant.parse("2026-10-24T23:59:59Z"),"09:00","Asia/Seoul","DAILY").toInstant());
    }
    @Test void daylightSavingGapMovesForwardAndOverlapUsesFirstOccurrence() {
        assertEquals(Instant.parse("2026-03-08T07:30:00Z"),
                WorkNotificationTime.at(LocalDate.parse("2026-03-08"),"02:30","America/New_York").toInstant());
        assertEquals(Instant.parse("2026-11-01T05:30:00Z"),
                WorkNotificationTime.at(LocalDate.parse("2026-11-01"),"01:30","America/New_York").toInstant());
        assertEquals(Instant.parse("2026-11-02T06:30:00Z"),
                WorkNotificationTime.next(Instant.parse("2026-11-01T05:31:00Z"),"01:30","America/New_York","DAILY").toInstant());
    }
    @Test void fixedOffsetsAndUnknownZoneAreRejected() {
        assertThrows(BusinessException.class,() -> WorkNotificationTime.zone("+09:00"));
        assertThrows(BusinessException.class,() -> WorkNotificationTime.zone("Unknown/City"));
    }
}
