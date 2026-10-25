package com.gw.api.convert.work;

import com.gw.api.dto.work.WorkTodoResponse;
import com.gw.api.dto.work.WorkTodoSummaryResponse;
import com.gw.api.dto.work.WorkTodoTreeResponse;
import com.gw.api.dto.work.WorkTodoListResponse;
import com.gw.api.dto.work.WorkTodoInboxSummaryResponse;
import com.gw.share.common.policy.WorkPolicy;
import com.gw.share.jvo.work.WorkTodoListJvo;
import com.gw.share.jvo.work.WorkTodoInboxSummaryJvo;
import com.gw.share.vo.work.WorkTodoVo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WorkTodoConvert {
    private WorkTodoConvert() { }

    // 저장 모델을 트리로 조합하고 최하위 기준 진행률·지연 건수를 계산한다.
    public static WorkTodoTreeResponse toTreeResponse(String workUnitUuid, List<WorkTodoVo> todos, LocalDate today) {
        Map<Long, List<WorkTodoVo>> childrenByParent = new HashMap<>();
        for (WorkTodoVo todo : todos) {
            childrenByParent.computeIfAbsent(todo.getPrntWrkTodoIdx(), key -> new ArrayList<>()).add(todo);
        }
        for (List<WorkTodoVo> siblings : childrenByParent.values()) {
            siblings.sort(Comparator.comparing((WorkTodoVo todo) -> todo.getSortOrd() == null ? Integer.MAX_VALUE : todo.getSortOrd())
                    .thenComparing(WorkTodoVo::getCreatedAt).thenComparing(WorkTodoVo::getIdx));
        }
        Counter counter = new Counter();
        List<WorkTodoResponse> roots = childrenByParent.getOrDefault(null, List.of()).stream()
                .map(todo -> toNodeResponse(todo, null, 0, childrenByParent, counter, today)).toList();
        int progress = counter.leafCount == 0 ? 0 : Math.round(counter.completedLeafCount * 100f / counter.leafCount);
        String status = progress == 100 ? WorkPolicy.TODO_STATUS_DONE
                : counter.delayedCount > 0 ? WorkPolicy.TODO_STATUS_DELAYED
                : progress > 0 ? WorkPolicy.TODO_STATUS_IN_PROGRESS : WorkPolicy.TODO_STATUS_TODO;
        return new WorkTodoTreeResponse(workUnitUuid,
                new WorkTodoSummaryResponse(status, progress, todos.size(), counter.leafCount, counter.completedLeafCount, counter.delayedCount), roots);
    }

    // 부모의 표시 상태와 최하위 집계 상태를 구분하여 응답한다.
    private static WorkTodoResponse toNodeResponse(WorkTodoVo todo, WorkTodoVo parent, int depth,
            Map<Long, List<WorkTodoVo>> childrenByParent, Counter counter, LocalDate today) {
        List<WorkTodoResponse> children = childrenByParent.getOrDefault(todo.getIdx(), List.of()).stream()
                .map(child -> toNodeResponse(child, todo, depth + 1, childrenByParent, counter, today)).toList();
        boolean leaf = children.isEmpty();
        boolean completed = WorkPolicy.TODO_STATUS_DONE.equals(todo.getSts());
        boolean delayed = !completed && todo.getDueDt() != null && todo.getDueDt().isBefore(today);
        if (leaf) {
            counter.leafCount++;
            if (completed) counter.completedLeafCount++;
            if (delayed) counter.delayedCount++;
        }
        return new WorkTodoResponse(todo.getUuid(), parent == null ? null : parent.getUuid(), todo.getTtl(), todo.getDscr(),
                delayed ? WorkPolicy.TODO_STATUS_DELAYED : todo.getSts(), todo.getPrgsRt(), todo.getStrtDt(), todo.getDueDt(),
                todo.getSortOrd(), depth, leaf, completed, delayed, leaf ? todo.getCmplAt() : null,
                todo.getCreatedAt(), todo.getUpdatedAt(), children);
    }

    // 통합 조회 결과에서 외부 식별자와 필요한 업무 정보만 노출한다.
    public static WorkTodoListResponse toListResponse(WorkTodoListJvo todo) {
        return new WorkTodoListResponse(todo.getUuid(), todo.getWorkUnitUuid(), todo.getWorkUnitTtl(), "Y".equals(todo.getWorkUnitUseYn()),
                todo.getPrntTodoUuid(), todo.getPrntTodoTtl(), todo.getTtl(), todo.getDscr(), todo.getStrtDt(), todo.getDueDt(),
                todo.getSortOrd(), WorkPolicy.TODO_STATUS_DONE.equals(todo.getSts()), todo.getCmplAt(), todo.getTodoGroup());
    }

    // DB의 최하위 건수로 현재 완료율을 계산하고 기준 날짜·시간대를 명시한다.
    public static WorkTodoInboxSummaryResponse toInboxSummary(WorkTodoInboxSummaryJvo summary, LocalDate today, String timeZone) {
        return new WorkTodoInboxSummaryResponse(summary.getTotalCount(), summary.getTodayCount(), summary.getUpcomingCount(),
                summary.getOverdueCount(), summary.getCompletedCount(), summary.getUnscheduledCount(), summary.getUnknownCompletionCount(),
                summary.getTotalCount() == 0 ? 0 : (int) Math.round(summary.getCompletedCount() * 100.0 / summary.getTotalCount()), today, timeZone);
    }

    private static final class Counter {
        private int leafCount;
        private int completedLeafCount;
        private int delayedCount;
    }
}
