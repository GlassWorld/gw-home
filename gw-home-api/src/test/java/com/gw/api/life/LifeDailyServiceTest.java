package com.gw.api.life;

import com.gw.api.dto.life.LifeDailyDtos.*;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.life.LifeDailyService;
import com.gw.infra.db.mapper.life.LifeDailyMapper;
import com.gw.share.common.exception.*;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.life.LifeDailyVo;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LifeDailyServiceTest {
    private static final String KEY = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
    @Mock LifeDailyMapper mapper;
    @Mock AccountLookupService accounts;
    LifeDailyService service;
    @BeforeEach void setup() { service = new LifeDailyService(mapper, accounts); }
    private void owner() { when(accounts.getAccountByLoginId("me")).thenReturn(AcctVo.builder().idx(7L).build()); }
    private void writer() { owner(); when(mapper.lockOwner(7L)).thenReturn(7L); }
    private LifeDailyVo item() { return LifeDailyVo.builder().uuid(KEY).ttl("산책").startDt(LocalDate.of(2024, 1, 1)).build(); }

    @Test void monthUsesAuthenticatedOwnerAndLeapMonthBoundaries() {
        owner();
        when(mapper.selectItems(anyLong(), any(), any())).thenReturn(List.of(item()));
        when(mapper.selectChecks(anyLong(), any(), any())).thenReturn(List.of());
        var result = service.getMonth("me", "2024-02", "Pacific/Kiritimati");
        assertEquals(LocalDate.now(ZoneId.of("Pacific/Kiritimati")), result.today());
        verify(mapper).selectItems(7L, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29));
        assertEquals(KEY, result.items().getFirst().itemUuid());
    }

    @Test void invalidMonthZoneTitleUuidAndFutureDateNeverWrite() {
        for (String month : List.of("2024-13", "2024-2", "0000-01", "x")) assertThrows(BusinessException.class, () -> service.getMonth("me", month, "UTC"));
        assertThrows(BusinessException.class, () -> service.getMonth("me", "2024-02", "Bad/Zone"));
        assertThrows(BusinessException.class, () -> service.createItem("me", new SaveItem(" "), "UTC"));
        assertThrows(BusinessException.class, () -> service.updateItem("me", "1-1-1-1-1", new SaveItem("산책")));
        assertThrows(BusinessException.class, () -> service.saveCheck("me", KEY, LocalDate.now(ZoneOffset.UTC).plusDays(1), new Completion(true), "UTC"));
        assertThrows(BusinessException.class, () -> service.saveCheck("me", KEY, LocalDate.of(2024, 1, 1), new Completion(null), "UTC"));
        verifyNoInteractions(mapper, accounts);
    }

    @Test void createUsesLocalTodayAndNormalizedTitle() {
        writer();
        var result = service.createItem("me", new SaveItem("  물 마시기  "), "Asia/Seoul");
        assertEquals("물 마시기", result.title());
        assertEquals(LocalDate.now(ZoneId.of("Asia/Seoul")), result.startDate());
        var capture = ArgumentCaptor.forClass(LifeDailyVo.class);
        verify(mapper).insertItem(capture.capture());
        assertEquals(7L, capture.getValue().getMbrAcctIdx());
        assertEquals("me", capture.getValue().getCreatedBy());
    }

    @Test void foreignItemsCannotBeChanged() {
        writer();
        assertThrows(BusinessException.class, () -> service.updateItem("me", KEY, new SaveItem("제목")));
        assertThrows(BusinessException.class, () -> service.endItem("me", KEY, "UTC"));
        assertThrows(BusinessException.class, () -> service.saveCheck("me", KEY, LocalDate.of(2024, 2, 1), new Completion(true), "UTC"));
        verify(mapper, never()).saveCheck(anyLong(), anyString(), any(), anyBoolean(), anyString());
        verify(mapper, never()).endItem(anyLong(), anyString(), any(), anyString());
        verify(mapper, never()).updateTitle(anyLong(), anyString(), anyString(), anyString());
    }

    @Test void endedItemsKeepPastChecksButRejectDatesOutsideTheirPeriod() {
        writer(); var item = item(); item.setEndDt(LocalDate.of(2024, 2, 29));
        when(mapper.selectItem(7L, KEY)).thenReturn(item);
        when(mapper.saveCheck(eq(7L), eq(KEY), any(), anyBoolean(), eq("me"))).thenReturn(1);
        service.saveCheck("me", KEY, LocalDate.of(2024, 2, 29), new Completion(true), "UTC");
        service.saveCheck("me", KEY, LocalDate.of(2024, 2, 29), new Completion(false), "UTC");
        assertThrows(BusinessException.class, () -> service.saveCheck("me", KEY, LocalDate.of(2024, 3, 1), new Completion(true), "UTC"));
        assertThrows(BusinessException.class, () -> service.saveCheck("me", KEY, LocalDate.of(2023, 12, 31), new Completion(true), "UTC"));
        assertThrows(BusinessException.class, () -> service.updateItem("me", KEY, new SaveItem("제목")));
        service.endItem("me", KEY, "UTC");
        verify(mapper, never()).endItem(anyLong(), anyString(), any(), anyString());
        verify(mapper, times(2)).saveCheck(anyLong(), anyString(), any(), anyBoolean(), anyString());
    }
}
