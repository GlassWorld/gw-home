package com.gw.api.work;

import com.gw.api.dto.work.WorkTodoListRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.work.WorkTodoInboxService;
import com.gw.infra.db.mapper.work.WorkTodoMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.WorkTodoPolicy;
import com.gw.share.jvo.work.WorkTodoInboxSummaryJvo;
import com.gw.share.jvo.work.WorkTodoListJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.WorkTodoListSearchVo;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkTodoInboxServiceTest {
    @Mock WorkTodoMapper mapper;
    @Mock AccountLookupService accounts;
    WorkTodoInboxService service;

    @BeforeEach void setUp() { service = new WorkTodoInboxService(mapper, accounts); }

    @Test void pagingUsesAuthenticatedOwnerAndNormalizedFilters() {
        when(accounts.getAccountByLoginId("me")).thenReturn(AcctVo.builder().idx(7L).build());
        when(mapper.selectMyWorkTodoCount(any())).thenReturn(21L);
        when(mapper.selectMyWorkTodos(any())).thenReturn(List.of(WorkTodoListJvo.builder()
                .uuid("todo-uuid").workUnitUuid("unit-uuid").ttl("내 할 일").sts("DONE")
                .workUnitUseYn("Y").todoGroup("COMPLETED").build()));
        var request = new WorkTodoListRequest();
        request.setPage(2);
        request.setKeyword("  검색  ");
        request.setWorkUnitUuid(" ");
        var response = service.getMyWorkTodos("me", request);
        var search = ArgumentCaptor.forClass(WorkTodoListSearchVo.class);
        verify(mapper).selectMyWorkTodos(search.capture());
        assertEquals(7L, search.getValue().getMbrAcctIdx());
        assertEquals(20L, search.getValue().getOffset());
        assertEquals("검색", search.getValue().getKeyword());
        assertNull(search.getValue().getWorkUnitUuid());
        assertEquals(2, response.totalPages());
        assertEquals(21L, response.totalCount());
        assertTrue(response.content().getFirst().completed());
        assertNull(response.content().getFirst().completedAt());
    }

    @Test void summaryUsesLeafDenominatorAndExposesUnknownCompletion() {
        when(accounts.getAccountByLoginId("me")).thenReturn(AcctVo.builder().idx(7L).build());
        when(mapper.selectMyWorkTodoSummary(any())).thenReturn(WorkTodoInboxSummaryJvo.builder()
                .totalCount(3).completedCount(1).todayCount(1).overdueCount(1).unknownCompletionCount(1).build());
        var response = service.getMyWorkTodoSummary("me", new WorkTodoListRequest());
        assertEquals(33, response.progressRate());
        assertEquals(1, response.unknownCompletionCount());
        assertEquals("Asia/Seoul", response.timeZone());
    }

    @Test void rejectsInvalidQueryBeforeDatabaseAccess() {
        var request = new WorkTodoListRequest();
        request.setGroup(null);
        assertBadRequest(request);
        request.setGroup("ALL");
        request.setSize(101);
        assertBadRequest(request);
        request.setSize(20);
        request.setWorkUnitUuid("invalid-uuid");
        assertBadRequest(request);
        request.setWorkUnitUuid(null);
        request.setFromDate(LocalDate.of(2026, 10, 25));
        request.setToDate(LocalDate.of(2026, 10, 24));
        assertBadRequest(request);
        verifyNoInteractions(mapper, accounts);
    }

    @Test void sameInstantUsesUsersCalendarDateAndRejectsUnknownZone() {
        Instant instant = Instant.parse("2026-10-24T16:00:00Z");
        assertEquals(LocalDate.of(2026, 10, 25), WorkTodoPolicy.dateAt(instant, "Asia/Seoul"));
        assertEquals(LocalDate.of(2026, 10, 24), WorkTodoPolicy.dateAt(instant, "America/Los_Angeles"));
        assertEquals(LocalDate.of(2026, 3, 8), WorkTodoPolicy.dateAt(Instant.parse("2026-03-08T10:01:00Z"), "America/Los_Angeles"));
        assertThrows(BusinessException.class, () -> WorkTodoPolicy.dateAt(instant, "Not/AZone"));
    }

    private void assertBadRequest(WorkTodoListRequest request) {
        assertEquals(ErrorCode.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> service.getMyWorkTodos("me", request)).getErrorCode());
    }
}
