package com.gw.api.work;

import com.gw.api.convert.work.WorkInsightsConvert;
import com.gw.api.dto.work.WorkInsightsRequest;
import com.gw.api.dto.work.WorkInsightsResponse.Period;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.work.WorkInsightsService;
import com.gw.infra.db.mapper.work.WorkInsightsMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.jvo.work.*;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.WorkTodoListSearchVo;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkInsightsServiceTest {
    WorkInsightsMapper mapper = mock(WorkInsightsMapper.class);
    AccountLookupService accounts = mock(AccountLookupService.class);
    WorkInsightsService service = new WorkInsightsService(mapper, accounts, Clock.fixed(Instant.parse("2026-10-25T16:00:00Z"), ZoneOffset.UTC));
    @BeforeEach void account() { when(accounts.getAccountByLoginId("me")).thenReturn(AcctVo.builder().idx(7L).build()); }
    private WorkInsightsRequest request(String from, String to, String zone) {
        var request = new WorkInsightsRequest();
        request.setFromDate(LocalDate.parse(from)); request.setToDate(LocalDate.parse(to)); request.setTimeZone(zone);
        return request;
    }
    @Test void defaultsAndOwnerAreSharedButDateFilterDoesNotChangeCurrentDenominator() {
        var result = service.getStatistics("me", new WorkInsightsRequest());
        var search = ArgumentCaptor.forClass(WorkTodoListSearchVo.class);
        verify(mapper).selectUnitInsights(search.capture(), any(), any());
        assertEquals(7L, search.getValue().getMbrAcctIdx());
        assertTrue(search.getValue().isIncludeInactive());
        assertNull(search.getValue().getFromDate()); assertNull(search.getValue().getToDate());
        assertEquals("2026-10-26", result.period().asOfDate().toString());
        assertEquals(30, result.trend().size()); assertEquals(0, result.summary().progressRate());
        var gantt = service.getGantt("me", new WorkInsightsRequest());
        assertFalse(gantt.period().includeInactive());
        assertEquals(LocalDate.parse("2026-11-08"), gantt.period().toDate());
    }
    @Test void dstSpringAndAutumnUseExclusiveNextLocalMidnight() {
        service.getStatistics("me", request("2026-03-08", "2026-03-08", "America/New_York"));
        verify(mapper).selectUnitInsights(any(), eq(OffsetDateTime.parse("2026-03-08T00:00:00-05:00")),
                eq(OffsetDateTime.parse("2026-03-09T00:00:00-04:00")));
        service.getStatistics("me", request("2026-11-01", "2026-11-01", "America/New_York"));
        verify(mapper).selectUnitInsights(any(), eq(OffsetDateTime.parse("2026-11-01T00:00:00-04:00")),
                eq(OffsetDateTime.parse("2026-11-02T00:00:00-05:00")));
    }
    @Test void invalidPeriodsZonesBucketsAndIdentifiersNeverReachMapper() {
        for (var invalid : List.of(request("2026-01-02","2026-01-01","UTC"), request("2026-01-01","2027-01-02","UTC"),
                request("2026-01-01","2026-01-02","not-a-zone"), request("2026-01-01","2026-01-02","+09:00"))) {
            assertThrows(BusinessException.class, () -> service.getStatistics("me", invalid));
        }
        var missing = new WorkInsightsRequest(); missing.setFromDate(LocalDate.now());
        assertThrows(BusinessException.class, () -> service.getGantt("me", missing));
        var malformed = new WorkInsightsRequest(); malformed.setWorkUnitUuid("1-1-1-1-1");
        assertThrows(BusinessException.class, () -> service.getGantt("me", malformed));
        var badBucket = new WorkInsightsRequest(); badBucket.setBucket("year");
        assertThrows(BusinessException.class, () -> service.getStatistics("me", badBucket));
        assertThrows(BusinessException.class, () -> service.getGantt("me", request("2026-01-01","2026-04-04","UTC")));
        verifyNoInteractions(mapper);
    }
    @Test void tooManyGanttRowsFailInsteadOfSilentlyTruncating() {
        when(mapper.selectGanttRows(any(),any(),any())).thenReturn(Collections.nCopies(2001, WorkGanttRowJvo.builder().build()));
        assertThrows(BusinessException.class, () -> service.getGantt("me", new WorkInsightsRequest()));
        verify(mapper,never()).selectUnitInsights(any(),any(),any());
    }
    @Test void emptyAndUnknownCompletionDoNotInventHistoricalResults() {
        var unit = WorkInsightUnitJvo.builder().workUnitUuid("unit").totalCount(4).completedCount(2).unknownCompletionCount(1).build();
        var period = new Period(LocalDate.parse("2026-10-25"),LocalDate.parse("2026-11-03"),"Asia/Seoul",LocalDate.parse("2026-10-25"),true,null);
        var result = WorkInsightsConvert.toStatistics(period,"week",List.of(unit),List.of(
                WorkCompletionBucketJvo.builder().bucketStart(LocalDate.parse("2026-10-26")).completedCount(1).build()));
        assertEquals(50, result.summary().progressRate());
        assertEquals(1, result.summary().unknownCompletionCount());
        assertEquals(1, result.periodCompletedCount());
        assertEquals(List.of(0L,1L,0L), result.trend().stream().map(point -> point.completedCount()).toList());
        assertEquals(period.fromDate(), result.trend().getFirst().startDate());
        assertEquals(period.toDate(), result.trend().getLast().endDate());
        var month = WorkInsightsConvert.toStatistics(period,"month",List.of(),List.of());
        assertEquals(2,month.trend().size()); assertEquals(LocalDate.parse("2026-10-31"),month.trend().getFirst().endDate());
    }
    @Test void ganttKeepsContextAndHierarchyWithoutDuplicatingLeafProgress() {
        var parent = WorkGanttRowJvo.builder().uuid("parent").workUnitUuid("unit").ttl("parent").sts("IN_PROGRESS").contextOnly(true).prgsRt(50).build();
        var child = WorkGanttRowJvo.builder().uuid("child").workUnitUuid("unit").prntTodoUuid("parent").ttl("child").sts("DONE").leaf(true).prgsRt(100).build();
        var period = new Period(LocalDate.parse("2026-10-25"),LocalDate.parse("2026-10-30"),"UTC",LocalDate.parse("2026-10-25"),false,null);
        var result = WorkInsightsConvert.toGantt(period,List.of(),List.of(child,parent));
        assertEquals("parent",result.rows().getFirst().todoUuid()); assertTrue(result.rows().getFirst().contextOnly());
        assertEquals(1,result.rows().getLast().depth()); assertTrue(result.rows().getLast().completed());
        parent.setPrntTodoUuid("child");
        assertEquals(2,WorkInsightsConvert.toGantt(period,List.of(),List.of(child,parent)).rows().size());
    }
}
