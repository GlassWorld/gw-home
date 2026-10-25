package com.gw.api.life;

import com.gw.api.dto.life.LifeTodoDtos.*;
import com.gw.api.dto.life.LifeTodoListRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.life.LifeTodoService;
import com.gw.infra.db.mapper.life.LifeTodoMapper;
import com.gw.share.common.exception.*;
import com.gw.share.common.policy.LifeTodoPolicy;
import com.gw.share.jvo.life.LifeTodoJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.life.*;
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
class LifeTodoServiceTest {
    private static final String UUID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
    private static final String CATEGORY = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";
    @Mock LifeTodoMapper mapper;
    @Mock AccountLookupService accounts;
    LifeTodoService service;
    @BeforeEach void setup() { service = new LifeTodoService(mapper, accounts); }
    private void owner() { when(accounts.getAccountByLoginId("me")).thenReturn(AcctVo.builder().idx(7L).build()); }
    private void writer() { owner(); when(mapper.lockOwner(7L)).thenReturn(7L); }

    @Test void queryUsesAuthenticatedOwnerAndStablePaging() {
        owner();
        when(mapper.selectCount(any())).thenReturn(21L);
        when(mapper.selectTodos(any())).thenReturn(List.of());
        var request = new LifeTodoListRequest(); request.setKeyword("  장보기  "); request.setPage(2);
        request.setUncategorized(true);
        var response = service.getTodos("me", request);
        var capture = ArgumentCaptor.forClass(LifeTodoSearchVo.class);
        verify(mapper).selectTodos(capture.capture());
        assertEquals(7L, capture.getValue().getMbrAcctIdx());
        assertEquals("장보기", capture.getValue().getKeyword());
        assertEquals(20, capture.getValue().getOffset());
        assertTrue(capture.getValue().isUncategorized());
        assertEquals(2, response.totalPages());
    }

    @Test void invalidFiltersAreRejectedBeforeDatabaseAccess() {
        var request = new LifeTodoListRequest();
        request.setGroup("INVALID"); assertBad(() -> service.getTodos("me", request));
        request.setGroup("ALL"); request.setSize(101); assertBad(() -> service.getTodos("me", request));
        request.setSize(20); request.setCategoryUuid("1-1-1-1-1"); assertBad(() -> service.getTodos("me", request));
        request.setCategoryUuid(CATEGORY); request.setUncategorized(true); assertBad(() -> service.getTodos("me", request));
        request.setUncategorized(false); request.setTimeZone("Bad/Zone"); assertBad(() -> service.getSummary("me", request));
        verifyNoInteractions(mapper, accounts);
    }

    @Test void createsTitleOnlyTodoWithoutWorkOrCategory() {
        writer();
        when(mapper.selectTodo(eq(7L), anyString(), any())).thenReturn(LifeTodoJvo.builder().uuid(UUID).ttl("장보기").todoGroup("UNSCHEDULED").build());
        var response = service.createTodo("me", new SaveTodo("  장보기  ", " ", null, null), "Asia/Seoul");
        var capture = ArgumentCaptor.forClass(LifeTodoVo.class);
        verify(mapper).insertTodo(capture.capture());
        assertEquals("장보기", capture.getValue().getTtl());
        assertEquals(7L, capture.getValue().getMbrAcctIdx());
        assertEquals("me", capture.getValue().getCreatedBy());
        assertNull(capture.getValue().getLfTodoCatIdx());
        assertNull(capture.getValue().getMemo());
        assertNull(capture.getValue().getDueDt());
        assertFalse(response.completed());
        verify(mapper, never()).selectCategory(anyLong(), anyString());
    }

    @Test void cannotAttachAnotherUsersCategory() {
        writer();
        assertNotFound(() -> service.createTodo("me", new SaveTodo("장보기", null, CATEGORY, null), "UTC"));
        verify(mapper).selectCategory(7L, CATEGORY);
        verify(mapper, never()).insertTodo(any());
    }

    @Test void cannotEditAnotherUsersTodo() {
        writer();
        assertNotFound(() -> service.updateTodo("me", UUID, new SaveTodo("변경", null, null, null), "UTC"));
        verify(mapper, never()).updateTodo(any());
    }

    @Test void cannotDeleteOrCompleteMissingOrForeignTodo() {
        writer();
        assertNotFound(() -> service.deleteTodo("me", UUID));
        assertNotFound(() -> service.completeTodo("me", UUID, new Completion(true), "UTC"));
        verify(mapper).deleteTodo(7L, UUID, "me");
        verify(mapper).updateCompletion(7L, UUID, true, "me");
    }

    @Test void deletingCategorySerializesAndPreservesTodosAsUncategorized() {
        writer();
        when(mapper.selectCategory(7L, CATEGORY)).thenReturn(LifeTodoCategoryVo.builder().idx(12L).uuid(CATEGORY).build());
        when(mapper.deleteCategory(7L, CATEGORY, "me")).thenReturn(1);
        service.deleteCategory("me", CATEGORY);
        var order = inOrder(mapper);
        order.verify(mapper).lockOwner(7L);
        order.verify(mapper).selectCategory(7L, CATEGORY);
        order.verify(mapper).clearCategory(7L, 12L, "me");
        order.verify(mapper).deleteCategory(7L, CATEGORY, "me");
        verify(mapper, never()).deleteTodo(any(), any(), any());
    }

    @Test void foreignCategoryDeletionDoesNotTouchTodos() {
        writer();
        assertNotFound(() -> service.deleteCategory("me", CATEGORY));
        verify(mapper, never()).clearCategory(any(), any(), any());
        verify(mapper, never()).deleteCategory(any(), any(), any());
    }

    @Test void duplicateCategoryNameIsRejected() {
        writer();
        when(mapper.categoryNameExists(7L, "집안일", null)).thenReturn(true);
        var error = assertThrows(BusinessException.class, () -> service.createCategory("me", new SaveCategory(" 집안일 ")));
        assertEquals(ErrorCode.DUPLICATE, error.getErrorCode());
        verify(mapper, never()).insertCategory(any());
    }

    @Test void emptyAndOversizedInputAndNullCompletionAreRejected() {
        assertBad(() -> service.createTodo("me", new SaveTodo(" ", null, null, null), "UTC"));
        assertBad(() -> service.createTodo("me", new SaveTodo("x".repeat(201), null, null, null), "UTC"));
        assertBad(() -> service.createTodo("me", new SaveTodo("장보기", "x".repeat(4001), null, null), "UTC"));
        assertBad(() -> service.createCategory("me", new SaveCategory(" ")));
        assertBad(() -> service.completeTodo("me", UUID, new Completion(null), "UTC"));
        verifyNoInteractions(accounts, mapper);
    }

    @Test void sameInstantIsClassifiedUsingUsersCalendarDate() {
        var instant = Instant.parse("2026-10-24T16:00:00Z");
        assertEquals(LocalDate.parse("2026-10-25"), LifeTodoPolicy.dateAt(instant, "Asia/Seoul"));
        assertEquals(LocalDate.parse("2026-10-24"), LifeTodoPolicy.dateAt(instant, "America/Los_Angeles"));
    }

    private void assertBad(Runnable action) { assertEquals(ErrorCode.BAD_REQUEST, assertThrows(BusinessException.class, action::run).getErrorCode()); }
    private void assertNotFound(Runnable action) { assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class, action::run).getErrorCode()); }
}
