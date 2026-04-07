package com.gw.api.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    void setUp() {
        workTodoService = new WorkTodoService(workTodoMapper, workUnitMapper, accountLookupService);
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

        WorkTodoTreeResponse response = workTodoService.getWorkTodoTree("tester", "work-unit-uuid");

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
                        new UpdateWorkTodoCompletionRequest(true)
                )
        );

        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        verify(workTodoMapper, never()).updateWorkTodoDerivedFields(anyLong(), eq(WorkPolicy.TODO_STATUS_DONE), eq(100), eq("tester"));
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
