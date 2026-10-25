package com.gw.api.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import com.gw.api.dto.work.CreateWorkTodoRequest;
import com.gw.api.dto.work.UpdateWorkTodoRequest;
import com.gw.api.convert.work.WorkTodoConvert;

import com.gw.api.dto.work.UpdateWorkTodoCompletionRequest;
import com.gw.api.dto.work.WorkTodoTreeResponse;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.work.WorkTodoService;
import com.gw.infra.db.mapper.work.WorkTodoMapper;
import com.gw.infra.db.mapper.work.WorkUnitMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.WorkPolicy;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.WorkTodoVo;
import com.gw.share.vo.work.WorkUnitVo;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkTodoServiceTest {

    @Mock
    private WorkTodoMapper workTodoMapper;

    @Mock
    private WorkUnitMapper workUnitMapper;

    @Mock
    private AccountLookupService accountLookupService;

    private WorkTodoService workTodoService;

    @Mock
    private com.gw.api.service.work.WorkNotificationReservations notificationReservations;

    @BeforeEach
    void setUp() {
        workTodoService = new WorkTodoService(workTodoMapper, workUnitMapper, accountLookupService, notificationReservations);
    }

    @Test
    void getWorkTodoTreeBuildsDelayedSummaryFromLeafTodos() {
        when(accountLookupService.getAccountByLoginId("tester")).thenReturn(createAccount());
        when(workUnitMapper.selectWorkUnit("work-unit-uuid", 7L)).thenReturn(createWorkUnit());
        when(workTodoMapper.selectWorkTodoList(13L, 7L)).thenReturn(List.of(
                createTodo(101L, "root-uuid", null, "루트", WorkPolicy.TODO_STATUS_IN_PROGRESS, 50, null),
                createTodo(102L, "leaf-done-uuid", 101L, "완료 리프", WorkPolicy.TODO_STATUS_DONE, 100, LocalDate.now().minusDays(1)),
                createTodo(103L, "leaf-delay-uuid", 101L, "지연 리프", WorkPolicy.TODO_STATUS_TODO, 0, LocalDate.now().minusDays(2))
        ));

        WorkTodoTreeResponse response = workTodoService.getWorkTodoTree("tester", "work-unit-uuid", "Asia/Seoul");

        assertEquals(50, response.summary().progressRate());
        assertEquals(2, response.summary().leafCount());
        assertEquals(1, response.summary().completedLeafCount());
        assertEquals(1, response.summary().delayedCount());
        assertEquals(WorkPolicy.TODO_STATUS_DELAYED, response.summary().status());
        assertEquals(1, response.todos().size());
        assertEquals(2, response.todos().getFirst().children().size());
        assertEquals(WorkPolicy.TODO_STATUS_DELAYED, response.todos().getFirst().children().get(1).status());
    }

    @Test
    void updateWorkTodoCompletionRejectsNonLeafTodo() {
        when(workTodoMapper.selectWorkUnitForUpdate(13L, 7L)).thenReturn(13L);
        when(accountLookupService.getAccountByLoginId("tester")).thenReturn(createAccount());
        when(workUnitMapper.selectWorkUnit("work-unit-uuid", 7L)).thenReturn(createWorkUnit());
        when(workTodoMapper.selectWorkTodoList(13L, 7L)).thenReturn(List.of(
                createTodo(101L, "root-uuid", null, "루트", WorkPolicy.TODO_STATUS_IN_PROGRESS, 0, null),
                createTodo(102L, "child-uuid", 101L, "자식", WorkPolicy.TODO_STATUS_TODO, 0, null)
        ));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> workTodoService.updateWorkTodoCompletion(
                        "tester",
                        "work-unit-uuid",
                        "root-uuid",
                        new UpdateWorkTodoCompletionRequest(true), "Asia/Seoul"
                )
        );

        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        verify(workTodoMapper, never()).updateWorkTodoDerivedFields(anyLong(), eq(WorkPolicy.TODO_STATUS_DONE), eq(100), eq("tester"));
    }

    @Test
    void anotherOwnersWorkUnitCannotBeReadOrChanged() {
        when(accountLookupService.getAccountByLoginId("tester")).thenReturn(createAccount());
        assertThrows(BusinessException.class, () -> workTodoService.getWorkTodoTree("tester", "other-unit", "Asia/Seoul"));
        assertThrows(BusinessException.class, () -> workTodoService.updateWorkTodoCompletion("tester", "other-unit", "other-todo", new UpdateWorkTodoCompletionRequest(true), "Asia/Seoul"));
        verifyNoInteractions(workTodoMapper);
    }

    @Test
    void completionLocksUnitAndRecalculatesParentFromLeaves() {
        when(accountLookupService.getAccountByLoginId("tester")).thenReturn(createAccount());
        when(workUnitMapper.selectWorkUnit("work-unit-uuid", 7L)).thenReturn(createWorkUnit());
        when(workTodoMapper.selectWorkUnitForUpdate(13L, 7L)).thenReturn(13L);
        var root = createTodo(101L, "root", null, "부모", "TODO", 0, null);
        var leaf = createTodo(102L, "leaf", 101L, "자식", "TODO", 0, null);
        var doneLeaf = createTodo(102L, "leaf", 101L, "자식", "DONE", 100, null);
        doneLeaf.setCmplAt(OffsetDateTime.parse("2026-10-25T10:00:00+09:00"));
        when(workTodoMapper.selectWorkTodoList(13L, 7L))
                .thenReturn(List.of(root, leaf), List.of(root, doneLeaf));
        workTodoService.updateWorkTodoCompletion("tester", "work-unit-uuid", "leaf", new UpdateWorkTodoCompletionRequest(true), "Asia/Seoul");
        var order = inOrder(notificationReservations, workTodoMapper);
        order.verify(notificationReservations).lock(7L);
        order.verify(workTodoMapper).selectWorkUnitForUpdate(13L, 7L);
        order.verify(workTodoMapper).selectWorkTodoList(13L, 7L);
        order.verify(workTodoMapper).updateWorkTodoCompletion("leaf", 13L, 7L, true, "tester");
        order.verify(notificationReservations).synchronize(7L, "tester");
        verify(workTodoMapper).updateWorkTodoDerivedFields(101L, "DONE", 100, "tester");
        verify(workTodoMapper, never()).updateWorkTodoDerivedFields(102L, "DONE", 100, "tester");
    }

    @Test
    void addingChildClearsFormerLeafCompletionAndRecalculatesParent() {
        when(accountLookupService.getAccountByLoginId("tester")).thenReturn(createAccount());
        when(workUnitMapper.selectWorkUnit("work-unit-uuid", 7L)).thenReturn(createWorkUnit());
        when(workTodoMapper.selectWorkUnitForUpdate(13L, 7L)).thenReturn(13L);
        var root = createTodo(101L, "root", null, "부모", "DONE", 100, null);
        root.setCmplAt(OffsetDateTime.parse("2026-10-25T10:00:00+09:00"));
        var child = createTodo(102L, "child", 101L, "자식", "TODO", 0, null);
        when(workTodoMapper.selectWorkTodoList(13L, 7L)).thenReturn(List.of(root), List.of(root, child));
        workTodoService.createWorkTodo("tester", "work-unit-uuid", new CreateWorkTodoRequest("root", "자식", "", null, null, null), "Asia/Seoul");
        verify(workTodoMapper).updateWorkTodoDerivedFields(101L, "TODO", 0, "tester");
        verify(notificationReservations).synchronize(7L, "tester");
    }

    @Test
    void rejectsMovingParentBelowItsDescendant() {
        when(accountLookupService.getAccountByLoginId("tester")).thenReturn(createAccount());
        when(workUnitMapper.selectWorkUnit("work-unit-uuid", 7L)).thenReturn(createWorkUnit());
        when(workTodoMapper.selectWorkUnitForUpdate(13L, 7L)).thenReturn(13L);
        when(workTodoMapper.selectWorkTodoList(13L, 7L)).thenReturn(List.of(
                createTodo(101L, "root", null, "부모", "TODO", 0, null),
                createTodo(102L, "child", 101L, "자식", "TODO", 0, null)));
        assertThrows(BusinessException.class, () -> workTodoService.updateWorkTodo("tester", "work-unit-uuid", "root",
                new UpdateWorkTodoRequest("child", "부모", "", null, null, null), "Asia/Seoul"));
    }

    @Test
    void parentDelayIsNotDoubleCountedAndUpdatedAtIsNotCompletionTime() {
        var today = LocalDate.of(2026, 10, 25);
        var tree = WorkTodoConvert.toTreeResponse("unit", List.of(
                createTodo(101L, "root", null, "부모", "IN_PROGRESS", 50, today.minusDays(2)),
                createTodo(102L, "done", 101L, "완료", "DONE", 100, today.minusDays(1)),
                createTodo(103L, "late", 101L, "지연", "TODO", 0, today.minusDays(1))), today);
        assertEquals(1, tree.summary().delayedCount());
        assertEquals(2, tree.summary().leafCount());
        assertNull(tree.todos().getFirst().children().getFirst().completedAt());
    }

    private AcctVo createAccount() {
        return AcctVo.builder()
                .idx(7L)
                .uuid("account-uuid")
                .lgnId("tester")
                .email("tester@example.com")
                .role("USER")
                .build();
    }

    private WorkUnitVo createWorkUnit() {
        return WorkUnitVo.builder()
                .idx(13L)
                .uuid("work-unit-uuid")
                .mbrAcctIdx(7L)
                .ttl("업무")
                .sts("IN_PROGRESS")
                .useYn("Y")
                .createdAt(OffsetDateTime.parse("2026-04-07T09:00:00+09:00"))
                .updatedAt(OffsetDateTime.parse("2026-04-07T09:00:00+09:00"))
                .build();
    }

    private WorkTodoVo createTodo(
            Long idx,
            String uuid,
            Long parentTodoIdx,
            String title,
            String status,
            Integer progressRate,
            LocalDate dueDate
    ) {
        return WorkTodoVo.builder()
                .idx(idx)
                .uuid(uuid)
                .workUnitIdx(13L)
                .mbrAcctIdx(7L)
                .prntWrkTodoIdx(parentTodoIdx)
                .ttl(title)
                .sts(status)
                .prgsRt(progressRate)
                .dueDt(dueDate)
                .sortOrd(1)
                .createdAt(OffsetDateTime.parse("2026-04-07T09:00:00+09:00"))
                .updatedAt(OffsetDateTime.parse("2026-04-07T09:00:00+09:00"))
                .build();
    }
}
