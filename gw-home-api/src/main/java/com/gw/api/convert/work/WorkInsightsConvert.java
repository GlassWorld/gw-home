package com.gw.api.convert.work;

import com.gw.api.dto.work.WorkInsightsResponse.*;
import com.gw.share.jvo.work.WorkGanttRowJvo;
import com.gw.share.jvo.work.WorkInsightUnitJvo;
import com.gw.share.jvo.work.WorkCompletionBucketJvo;
import com.gw.share.jvo.work.WorkTodoInboxSummaryJvo;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public final class WorkInsightsConvert {
    private WorkInsightsConvert() { }

    // 업무의 수동 상태와 전체 최하위 TODO 진행률을 다른 필드로 반환한다.
    public static List<Unit> toUnits(List<WorkInsightUnitJvo> units, Period period) {
        return units.stream().map(unit -> new Unit(unit.getWorkUnitUuid(), unit.getTtl(), unit.getSts(),
                "Y".equals(unit.getUseYn()), unit.getStrtDt(), unit.getDueDt(), unit.getPartialCount() > 0,
                WorkTodoConvert.toInboxSummary(unit, period.asOfDate(), period.timeZone()),
                unit.getPeriodCompletedCount())).toList();
    }

    // 조회된 행을 계층 순으로 정렬하며 반복 방문·깊은 트리의 재귀 호출을 피한다.
    public static Gantt toGantt(Period period, List<WorkInsightUnitJvo> units, List<WorkGanttRowJvo> rows) {
        Map<String, List<WorkGanttRowJvo>> children = new HashMap<>();
        Set<String> ids = new HashSet<>();
        rows.forEach(row -> ids.add(row.getUuid()));
        for (var row : rows) {
            if (row.getPrntTodoUuid() != null && ids.contains(row.getPrntTodoUuid()))
                children.computeIfAbsent(row.getPrntTodoUuid(), key -> new ArrayList<>()).add(row);
        }
        List<GanttRow> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (var row : rows) {
            if (!ids.contains(row.getPrntTodoUuid())) appendBranch(row, children, visited, result, period.asOfDate());
        }
        // 비정상 순환 데이터도 조용히 숨기지 않고 한 번씩만 표시한다.
        for (var row : rows) appendBranch(row, children, visited, result, period.asOfDate());
        Set<String> visibleUnits = new HashSet<>();
        rows.forEach(row -> visibleUnits.add(row.getWorkUnitUuid()));
        return new Gantt(period, toUnits(units, period).stream().filter(unit -> visibleUnits.contains(unit.workUnitUuid())).toList(), result);
    }

    private static void appendBranch(WorkGanttRowJvo root, Map<String, List<WorkGanttRowJvo>> children,
            Set<String> visited, List<GanttRow> result, LocalDate today) {
        record Pending(WorkGanttRowJvo row, int depth) { }
        Deque<Pending> pending = new ArrayDeque<>();
        pending.push(new Pending(root, 0));
        while (!pending.isEmpty()) {
            var next = pending.pop();
            var row = next.row();
            if (!visited.add(row.getUuid())) continue;
            boolean completed = "DONE".equals(row.getSts());
            boolean delayed = !completed && row.getDueDt() != null && row.getDueDt().isBefore(today);
            result.add(new GanttRow(row.getUuid(), row.getWorkUnitUuid(), row.getPrntTodoUuid(), row.getTtl(),
                    row.getStrtDt(), row.getDueDt(), delayed ? "DELAYED" : row.getSts(),
                    row.getPrgsRt() == null ? 0 : row.getPrgsRt(), row.isLeaf(), completed, delayed, row.isContextOnly(), next.depth()));
            var descendants = children.getOrDefault(row.getUuid(), List.of());
            for (int index = descendants.size() - 1; index >= 0; index--)
                pending.push(new Pending(descendants.get(index), next.depth() + 1));
        }
    }

    // 현재 최하위 분모와 기간 실적을 구분하며 비어 있는 날짜 구간도 0으로 채운다.
    public static Statistics toStatistics(Period period, String bucket, List<WorkInsightUnitJvo> units,
            List<WorkCompletionBucketJvo> buckets) {
        var total = WorkTodoInboxSummaryJvo.builder()
                .totalCount(units.stream().mapToLong(WorkInsightUnitJvo::getTotalCount).sum())
                .todayCount(units.stream().mapToLong(WorkInsightUnitJvo::getTodayCount).sum())
                .upcomingCount(units.stream().mapToLong(WorkInsightUnitJvo::getUpcomingCount).sum())
                .overdueCount(units.stream().mapToLong(WorkInsightUnitJvo::getOverdueCount).sum())
                .completedCount(units.stream().mapToLong(WorkInsightUnitJvo::getCompletedCount).sum())
                .unscheduledCount(units.stream().mapToLong(WorkInsightUnitJvo::getUnscheduledCount).sum())
                .unknownCompletionCount(units.stream().mapToLong(WorkInsightUnitJvo::getUnknownCompletionCount).sum()).build();
        Map<LocalDate, Long> counts = new HashMap<>();
        buckets.forEach(row -> counts.put(row.getBucketStart(), row.getCompletedCount()));
        LocalDate start = switch (bucket) {
            case "week" -> period.fromDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "month" -> period.fromDate().withDayOfMonth(1);
            default -> period.fromDate();
        };
        List<CompletionBucket> trend = new ArrayList<>();
        while (!start.isAfter(period.toDate())) {
            LocalDate next = switch (bucket) {
                case "week" -> start.plusWeeks(1);
                case "month" -> start.plusMonths(1);
                default -> start.plusDays(1);
            };
            trend.add(new CompletionBucket(start.isBefore(period.fromDate()) ? period.fromDate() : start,
                    next.minusDays(1).isAfter(period.toDate()) ? period.toDate() : next.minusDays(1),
                    counts.getOrDefault(start, 0L)));
            start = next;
        }
        return new Statistics(period, bucket, WorkTodoConvert.toInboxSummary(total, period.asOfDate(), period.timeZone()),
                trend.stream().mapToLong(CompletionBucket::completedCount).sum(), trend, toUnits(units, period));
    }
}
