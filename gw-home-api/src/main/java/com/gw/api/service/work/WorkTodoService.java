package com.gw.api.service.work;

import com.gw.api.convert.work.WorkTodoConvert;
import com.gw.api.dto.work.CreateWorkTodoRequest;
import com.gw.api.dto.work.UpdateWorkTodoCompletionRequest;
import com.gw.api.dto.work.UpdateWorkTodoRequest;
import com.gw.api.dto.work.WorkTodoResponse;
import com.gw.api.dto.work.WorkTodoSummaryResponse;
import com.gw.api.dto.work.WorkTodoTreeResponse;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.work.WorkTodoMapper;
import com.gw.infra.db.mapper.work.WorkUnitMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.WorkPolicy;
import com.gw.share.util.DateUtil;
import com.gw.share.util.StringUtil;
import com.gw.share.util.ValidationUtil;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.WorkTodoVo;
import com.gw.share.vo.work.WorkUnitVo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class WorkTodoService {

    private final WorkTodoMapper workTodoMapper;
    private final WorkUnitMapper workUnitMapper;
    private final AccountLookupService accountLookupService;

    public WorkTodoService(
            WorkTodoMapper workTodoMapper,
            WorkUnitMapper workUnitMapper,
            AccountLookupService accountLookupService
    ) {
        this.workTodoMapper = workTodoMapper;
        this.workUnitMapper = workUnitMapper;
        this.accountLookupService = accountLookupService;
    }

    // 로그인 사용자의 업무 TODO 트리를 조회한다.
    @Transactional(readOnly = true)
    public WorkTodoTreeResponse getWorkTodoTree(String loginId, String workUnitUuid) {
        log.info("getWorkTodoTree 시작 - loginId: {}, workUnitUuid: {}", loginId, workUnitUuid);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkUnitVo workUnit = getWorkUnit(workUnitUuid, account.getIdx());
            List<WorkTodoVo> workTodos = loadWorkTodos(workUnit.getIdx(), account.getIdx());
            WorkTodoTreeResponse response = buildTreeResponse(workUnit.getUuid(), workTodos);
            log.info("getWorkTodoTree 완료 - loginId: {}, workUnitUuid: {}, itemCount: {}",
                    loginId, workUnitUuid, response.summary().itemCount());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getWorkTodoTree 실패 - loginId: {}, workUnitUuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    workUnitUuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자가 업무 TODO를 추가한다.
    public WorkTodoTreeResponse createWorkTodo(String loginId, String workUnitUuid, CreateWorkTodoRequest request) {
        log.info("createWorkTodo 시작 - loginId: {}, workUnitUuid: {}, title: {}", loginId, workUnitUuid, request.title());
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkUnitVo workUnit = getWorkUnit(workUnitUuid, account.getIdx());
            validateDateRange(request.startDate(), request.dueDate());

            List<WorkTodoVo> workTodos = loadWorkTodos(workUnit.getIdx(), account.getIdx());
            WorkTodoVo parentTodo = getOptionalTodo(request.parentTodoUuid(), workTodos);
            Integer requestedSortOrder = request.sortOrder();
            int sortOrder = requestedSortOrder == null
                    ? resolveAppendSortOrder(workTodos, parentTodo == null ? null : parentTodo.getIdx())
                    : requestedSortOrder;

            WorkTodoVo workTodo = WorkTodoVo.builder()
                    .workUnitIdx(workUnit.getIdx())
                    .mbrAcctIdx(account.getIdx())
                    .prntWrkTodoIdx(parentTodo == null ? null : parentTodo.getIdx())
                    .ttl(normalizeRequiredTitle(request.title()))
                    .dscr(normalizeText(request.description()))
                    .sts(WorkPolicy.TODO_STATUS_TODO)
                    .prgsRt(0)
                    .strtDt(request.startDate())
                    .dueDt(request.dueDate())
                    .sortOrd(sortOrder)
                    .createdBy(loginId)
                    .build();
            workTodoMapper.insertWorkTodo(workTodo);

            WorkTodoTreeResponse response = recalculateAndBuildTree(workUnit.getUuid(), workUnit.getIdx(), account.getIdx(), loginId);
            log.info("createWorkTodo 완료 - loginId: {}, workUnitUuid: {}, todoIdx: {}", loginId, workUnitUuid, workTodo.getIdx());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "createWorkTodo 실패 - loginId: {}, workUnitUuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    workUnitUuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자가 업무 TODO를 수정한다.
    public WorkTodoTreeResponse updateWorkTodo(
            String loginId,
            String workUnitUuid,
            String todoUuid,
            UpdateWorkTodoRequest request
    ) {
        log.info("updateWorkTodo 시작 - loginId: {}, workUnitUuid: {}, todoUuid: {}", loginId, workUnitUuid, todoUuid);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkUnitVo workUnit = getWorkUnit(workUnitUuid, account.getIdx());
            validateDateRange(request.startDate(), request.dueDate());

            List<WorkTodoVo> workTodos = loadWorkTodos(workUnit.getIdx(), account.getIdx());
            WorkTodoVo targetTodo = getRequiredTodo(todoUuid, workTodos);
            WorkTodoVo parentTodo = getOptionalTodo(request.parentTodoUuid(), workTodos);

            if (parentTodo != null) {
                ValidationUtil.requireTrue(
                        !parentTodo.getUuid().equals(targetTodo.getUuid()),
                        ErrorCode.BAD_REQUEST,
                        "부모 TODO를 자기 자신으로 지정할 수 없습니다."
                );
                validateNotDescendantParent(targetTodo, parentTodo, workTodos);
            }

            targetTodo.setPrntWrkTodoIdx(parentTodo == null ? null : parentTodo.getIdx());
            targetTodo.setTtl(normalizeRequiredTitle(request.title()));
            targetTodo.setDscr(normalizeText(request.description()));
            targetTodo.setStrtDt(request.startDate());
            targetTodo.setDueDt(request.dueDate());
            targetTodo.setSortOrd(request.sortOrder() == null ? targetTodo.getSortOrd() : request.sortOrder());
            targetTodo.setUpdatedBy(loginId);
            workTodoMapper.updateWorkTodo(targetTodo);

            WorkTodoTreeResponse response = recalculateAndBuildTree(workUnit.getUuid(), workUnit.getIdx(), account.getIdx(), loginId);
            log.info("updateWorkTodo 완료 - loginId: {}, workUnitUuid: {}, todoUuid: {}", loginId, workUnitUuid, todoUuid);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "updateWorkTodo 실패 - loginId: {}, workUnitUuid: {}, todoUuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    workUnitUuid,
                    todoUuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자가 최하위 TODO 완료 여부를 변경한다.
    public WorkTodoTreeResponse updateWorkTodoCompletion(
            String loginId,
            String workUnitUuid,
            String todoUuid,
            UpdateWorkTodoCompletionRequest request
    ) {
        log.info("updateWorkTodoCompletion 시작 - loginId: {}, workUnitUuid: {}, todoUuid: {}, completed: {}",
                loginId, workUnitUuid, todoUuid, request.completed());
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkUnitVo workUnit = getWorkUnit(workUnitUuid, account.getIdx());
            List<WorkTodoVo> workTodos = loadWorkTodos(workUnit.getIdx(), account.getIdx());
            WorkTodoVo targetTodo = getRequiredTodo(todoUuid, workTodos);

            ValidationUtil.requireTrue(
                    findChildren(targetTodo.getIdx(), workTodos).isEmpty(),
                    ErrorCode.BAD_REQUEST,
                    "하위 TODO가 있는 항목은 직접 완료 처리할 수 없습니다."
            );

            workTodoMapper.updateWorkTodoDerivedFields(
                    targetTodo.getIdx(),
                    request.completed() ? WorkPolicy.TODO_STATUS_DONE : WorkPolicy.TODO_STATUS_TODO,
                    request.completed() ? 100 : 0,
                    loginId
            );

            WorkTodoTreeResponse response = recalculateAndBuildTree(workUnit.getUuid(), workUnit.getIdx(), account.getIdx(), loginId);
            log.info("updateWorkTodoCompletion 완료 - loginId: {}, workUnitUuid: {}, todoUuid: {}",
                    loginId, workUnitUuid, todoUuid);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "updateWorkTodoCompletion 실패 - loginId: {}, workUnitUuid: {}, todoUuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    workUnitUuid,
                    todoUuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자가 업무 TODO를 삭제한다.
    public WorkTodoTreeResponse deleteWorkTodo(String loginId, String workUnitUuid, String todoUuid) {
        log.info("deleteWorkTodo 시작 - loginId: {}, workUnitUuid: {}, todoUuid: {}", loginId, workUnitUuid, todoUuid);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkUnitVo workUnit = getWorkUnit(workUnitUuid, account.getIdx());
            WorkTodoVo targetTodo = workTodoMapper.selectWorkTodo(todoUuid, workUnit.getIdx(), account.getIdx());
            if (targetTodo == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "업무 TODO를 찾을 수 없습니다.");
            }

            workTodoMapper.deleteWorkTodoSubtree(todoUuid, workUnit.getIdx(), account.getIdx(), loginId);
            WorkTodoTreeResponse response = recalculateAndBuildTree(workUnit.getUuid(), workUnit.getIdx(), account.getIdx(), loginId);
            log.info("deleteWorkTodo 완료 - loginId: {}, workUnitUuid: {}, todoUuid: {}", loginId, workUnitUuid, todoUuid);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "deleteWorkTodo 실패 - loginId: {}, workUnitUuid: {}, todoUuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    workUnitUuid,
                    todoUuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    private WorkTodoTreeResponse recalculateAndBuildTree(
            String workUnitUuid,
            Long workUnitIdx,
            Long mbrAcctIdx,
            String loginId
    ) {
        List<WorkTodoVo> latestTodos = loadWorkTodos(workUnitIdx, mbrAcctIdx);
        normalizeSortOrders(latestTodos, loginId);
        List<WorkTodoVo> recalculationTargets = loadWorkTodos(workUnitIdx, mbrAcctIdx);
        recalculateTree(recalculationTargets, loginId);
        return buildTreeResponse(workUnitUuid, loadWorkTodos(workUnitIdx, mbrAcctIdx));
    }

    private WorkTodoTreeResponse buildTreeResponse(String workUnitUuid, List<WorkTodoVo> workTodos) {
        TreeBuildResult treeBuildResult = buildTree(workTodos);
        return WorkTodoConvert.toTreeResponse(workUnitUuid, treeBuildResult.summary(), treeBuildResult.roots());
    }

    private List<WorkTodoVo> loadWorkTodos(Long workUnitIdx, Long mbrAcctIdx) {
        List<WorkTodoVo> workTodos = workTodoMapper.selectWorkTodoList(workUnitIdx, mbrAcctIdx);
        return workTodos == null ? List.of() : workTodos;
    }

    private void normalizeSortOrders(List<WorkTodoVo> workTodos, String loginId) {
        Map<Long, List<WorkTodoVo>> siblingsByParent = new LinkedHashMap<>();

        for (WorkTodoVo workTodo : workTodos) {
            siblingsByParent.computeIfAbsent(workTodo.getPrntWrkTodoIdx(), key -> new ArrayList<>()).add(workTodo);
        }

        for (List<WorkTodoVo> siblings : siblingsByParent.values()) {
            siblings.sort(Comparator
                    .comparing((WorkTodoVo workTodo) -> safeSortOrder(workTodo.getSortOrd()))
                    .thenComparing(WorkTodoVo::getCreatedAt)
                    .thenComparing(WorkTodoVo::getIdx));

            for (int index = 0; index < siblings.size(); index++) {
                int normalizedSortOrder = index + 1;
                WorkTodoVo sibling = siblings.get(index);
                if (!Integer.valueOf(normalizedSortOrder).equals(sibling.getSortOrd())) {
                    workTodoMapper.updateWorkTodoSortOrder(sibling.getIdx(), normalizedSortOrder, loginId);
                }
            }
        }
    }

    private void recalculateTree(List<WorkTodoVo> workTodos, String loginId) {
        Map<Long, List<WorkTodoVo>> childrenByParent = new HashMap<>();
        List<WorkTodoVo> roots = new ArrayList<>();

        for (WorkTodoVo workTodo : workTodos) {
            childrenByParent.computeIfAbsent(workTodo.getPrntWrkTodoIdx(), key -> new ArrayList<>()).add(workTodo);
        }

        for (List<WorkTodoVo> children : childrenByParent.values()) {
            children.sort(Comparator
                    .comparing((WorkTodoVo workTodo) -> safeSortOrder(workTodo.getSortOrd()))
                    .thenComparing(WorkTodoVo::getCreatedAt)
                    .thenComparing(WorkTodoVo::getIdx));
        }

        roots.addAll(childrenByParent.getOrDefault(null, List.of()));
        for (WorkTodoVo root : roots) {
            recalculateNode(root, childrenByParent, loginId);
        }
    }

    private ProgressAggregate recalculateNode(
            WorkTodoVo currentTodo,
            Map<Long, List<WorkTodoVo>> childrenByParent,
            String loginId
    ) {
        List<WorkTodoVo> children = childrenByParent.getOrDefault(currentTodo.getIdx(), List.of());
        if (children.isEmpty()) {
            String nextStatus = WorkPolicy.TODO_STATUS_DONE.equals(currentTodo.getSts())
                    ? WorkPolicy.TODO_STATUS_DONE
                    : WorkPolicy.TODO_STATUS_TODO;
            int nextProgressRate = WorkPolicy.TODO_STATUS_DONE.equals(nextStatus) ? 100 : 0;
            persistDerivedFieldsIfChanged(currentTodo, nextStatus, nextProgressRate, loginId);
            return new ProgressAggregate(1, WorkPolicy.TODO_STATUS_DONE.equals(nextStatus) ? 1 : 0);
        }

        int leafCount = 0;
        int completedLeafCount = 0;
        for (WorkTodoVo child : children) {
            ProgressAggregate childAggregate = recalculateNode(child, childrenByParent, loginId);
            leafCount += childAggregate.leafCount();
            completedLeafCount += childAggregate.completedLeafCount();
        }

        int nextProgressRate = leafCount == 0 ? 0 : Math.round((completedLeafCount * 100f) / leafCount);
        String nextStatus = nextProgressRate == 100
                ? WorkPolicy.TODO_STATUS_DONE
                : nextProgressRate == 0
                ? WorkPolicy.TODO_STATUS_TODO
                : WorkPolicy.TODO_STATUS_IN_PROGRESS;
        persistDerivedFieldsIfChanged(currentTodo, nextStatus, nextProgressRate, loginId);

        return new ProgressAggregate(leafCount, completedLeafCount);
    }

    private void persistDerivedFieldsIfChanged(WorkTodoVo workTodo, String nextStatus, int nextProgressRate, String loginId) {
        boolean statusChanged = !nextStatus.equals(workTodo.getSts());
        boolean progressChanged = !Integer.valueOf(nextProgressRate).equals(workTodo.getPrgsRt());

        if (!statusChanged && !progressChanged) {
            return;
        }

        workTodoMapper.updateWorkTodoDerivedFields(workTodo.getIdx(), nextStatus, nextProgressRate, loginId);
    }

    private TreeBuildResult buildTree(List<WorkTodoVo> workTodos) {
        Map<Long, List<WorkTodoVo>> childrenByParent = new HashMap<>();
        Map<Long, WorkTodoVo> todoByIdx = new HashMap<>();

        for (WorkTodoVo workTodo : workTodos) {
            todoByIdx.put(workTodo.getIdx(), workTodo);
            childrenByParent.computeIfAbsent(workTodo.getPrntWrkTodoIdx(), key -> new ArrayList<>()).add(workTodo);
        }

        for (List<WorkTodoVo> children : childrenByParent.values()) {
            children.sort(Comparator
                    .comparing((WorkTodoVo workTodo) -> safeSortOrder(workTodo.getSortOrd()))
                    .thenComparing(WorkTodoVo::getCreatedAt)
                    .thenComparing(WorkTodoVo::getIdx));
        }

        List<WorkTodoResponse> roots = new ArrayList<>();
        Counter counter = new Counter();
        for (WorkTodoVo rootTodo : childrenByParent.getOrDefault(null, List.of())) {
            roots.add(toResponse(rootTodo, null, 0, childrenByParent, counter));
        }

        int progressRate = counter.leafCount == 0 ? 0 : Math.round((counter.completedLeafCount * 100f) / counter.leafCount);
        String summaryStatus = counter.leafCount == 0
                ? WorkPolicy.TODO_STATUS_TODO
                : progressRate == 100
                ? WorkPolicy.TODO_STATUS_DONE
                : counter.delayedCount > 0
                ? WorkPolicy.TODO_STATUS_DELAYED
                : progressRate == 0
                ? WorkPolicy.TODO_STATUS_TODO
                : WorkPolicy.TODO_STATUS_IN_PROGRESS;

        return new TreeBuildResult(
                roots,
                new WorkTodoSummaryResponse(
                        summaryStatus,
                        progressRate,
                        workTodos.size(),
                        counter.leafCount,
                        counter.completedLeafCount,
                        counter.delayedCount
                )
        );
    }

    private WorkTodoResponse toResponse(
            WorkTodoVo workTodo,
            WorkTodoVo parentTodo,
            int depth,
            Map<Long, List<WorkTodoVo>> childrenByParent,
            Counter counter
    ) {
        List<WorkTodoVo> childTodos = childrenByParent.getOrDefault(workTodo.getIdx(), List.of());
        List<WorkTodoResponse> children = new ArrayList<>();
        for (WorkTodoVo childTodo : childTodos) {
            children.add(toResponse(childTodo, workTodo, depth + 1, childrenByParent, counter));
        }

        boolean leaf = childTodos.isEmpty();
        boolean completed = WorkPolicy.TODO_STATUS_DONE.equals(workTodo.getSts());
        boolean delayed = !completed
                && workTodo.getDueDt() != null
                && workTodo.getDueDt().isBefore(LocalDate.now());

        if (delayed) {
            counter.delayedCount++;
        }

        if (leaf) {
            counter.leafCount++;
            if (completed) {
                counter.completedLeafCount++;
            }
        }

        return new WorkTodoResponse(
                workTodo.getUuid(),
                parentTodo == null ? null : parentTodo.getUuid(),
                workTodo.getTtl(),
                workTodo.getDscr(),
                delayed ? WorkPolicy.TODO_STATUS_DELAYED : workTodo.getSts(),
                workTodo.getPrgsRt(),
                workTodo.getStrtDt(),
                workTodo.getDueDt(),
                workTodo.getSortOrd(),
                depth,
                leaf,
                completed,
                delayed,
                workTodo.getCreatedAt(),
                workTodo.getUpdatedAt(),
                children
        );
    }

    private void validateNotDescendantParent(WorkTodoVo targetTodo, WorkTodoVo parentTodo, List<WorkTodoVo> workTodos) {
        Map<Long, WorkTodoVo> todoByIdx = new HashMap<>();
        for (WorkTodoVo workTodo : workTodos) {
            todoByIdx.put(workTodo.getIdx(), workTodo);
        }

        WorkTodoVo current = parentTodo;
        while (current != null) {
            if (current.getIdx().equals(targetTodo.getIdx())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "하위 TODO 아래로 이동할 수 없습니다.");
            }
            current = current.getPrntWrkTodoIdx() == null ? null : todoByIdx.get(current.getPrntWrkTodoIdx());
        }
    }

    private List<WorkTodoVo> findChildren(Long parentTodoIdx, List<WorkTodoVo> workTodos) {
        return workTodos.stream()
                .filter(workTodo -> parentTodoIdx.equals(workTodo.getPrntWrkTodoIdx()))
                .toList();
    }

    private int resolveAppendSortOrder(List<WorkTodoVo> workTodos, Long parentTodoIdx) {
        return workTodos.stream()
                .filter(workTodo -> parentTodoIdx == null
                        ? workTodo.getPrntWrkTodoIdx() == null
                        : parentTodoIdx.equals(workTodo.getPrntWrkTodoIdx()))
                .map(WorkTodoVo::getSortOrd)
                .filter(sortOrder -> sortOrder != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private int safeSortOrder(Integer sortOrder) {
        return sortOrder == null ? Integer.MAX_VALUE : sortOrder;
    }

    private WorkTodoVo getRequiredTodo(String todoUuid, List<WorkTodoVo> workTodos) {
        return getOptionalTodo(todoUuid, workTodos) == null
                ? throwNotFound()
                : getOptionalTodo(todoUuid, workTodos);
    }

    private WorkTodoVo throwNotFound() {
        throw new BusinessException(ErrorCode.NOT_FOUND, "업무 TODO를 찾을 수 없습니다.");
    }

    private WorkTodoVo getOptionalTodo(String todoUuid, List<WorkTodoVo> workTodos) {
        if (todoUuid == null || todoUuid.isBlank()) {
            return null;
        }

        return workTodos.stream()
                .filter(workTodo -> todoUuid.equals(workTodo.getUuid()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "업무 TODO를 찾을 수 없습니다."));
    }

    private void validateDateRange(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null) {
            DateUtil.validateDateOrder(startDate, dueDate, ErrorCode.BAD_REQUEST, "시작일은 마감일보다 늦을 수 없습니다.");
        }
    }

    private String normalizeRequiredTitle(String title) {
        String normalized = normalizeText(title);
        ValidationUtil.requireNonNull(normalized, ErrorCode.BAD_REQUEST, "TODO 제목은 필수입니다.");
        return normalized;
    }

    private String normalizeText(String value) {
        return StringUtil.normalizeBlank(value);
    }

    private AcctVo getAccountByLoginId(String loginId) {
        return accountLookupService.getAccountByLoginId(loginId);
    }

    private WorkUnitVo getWorkUnit(String uuid, Long mbrAcctIdx) {
        WorkUnitVo workUnit = workUnitMapper.selectWorkUnit(uuid, mbrAcctIdx);
        if (workUnit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다.");
        }
        return workUnit;
    }

    private record ProgressAggregate(int leafCount, int completedLeafCount) {
    }

    private record TreeBuildResult(List<WorkTodoResponse> roots, WorkTodoSummaryResponse summary) {
    }

    private static final class Counter {
        private int leafCount;
        private int completedLeafCount;
        private int delayedCount;
    }
}
