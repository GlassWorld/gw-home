package com.gw.api.service.work;

import com.gw.api.convert.work.WorkTodoConvert;
import com.gw.api.dto.work.WorkTodoListRequest;
import com.gw.api.dto.work.WorkTodoListResponse;
import com.gw.api.dto.work.WorkTodoInboxSummaryResponse;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.work.WorkTodoMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.WorkTodoPolicy;
import com.gw.share.common.response.PageResponse;
import com.gw.share.util.StringUtil;
import com.gw.share.vo.work.WorkTodoListSearchVo;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class WorkTodoInboxService {
    private final WorkTodoMapper workTodoMapper;
    private final AccountLookupService accountLookupService;

    // 로그인 사용자의 여러 업무에 속한 최하위 할 일을 페이징 조회한다.
    public PageResponse<WorkTodoListResponse> getMyWorkTodos(String loginId, WorkTodoListRequest request) {
        log.info("getMyWorkTodos 시작 - loginId: {}, page: {}", loginId, request.getPage());
        try {
            WorkTodoListSearchVo search = prepareSearch(loginId, request);
            long total = workTodoMapper.selectMyWorkTodoCount(search);
            var content = workTodoMapper.selectMyWorkTodos(search).stream().map(WorkTodoConvert::toListResponse).toList();
            log.info("getMyWorkTodos 완료 - loginId: {}, total: {}", loginId, total);
            return PageResponse.of(content, request.getPage(), request.getSize(), total);
        } catch (RuntimeException exception) {
            log.error("getMyWorkTodos 실패 - loginId: {}", loginId, exception);
            throw exception;
        }
    }

    // 로그인 사용자의 동일 조회 조건에 대한 분류별 건수와 현재 완료율을 반환한다.
    public WorkTodoInboxSummaryResponse getMyWorkTodoSummary(String loginId, WorkTodoListRequest request) {
        log.info("getMyWorkTodoSummary 시작 - loginId: {}", loginId);
        try {
            WorkTodoListSearchVo search = prepareSearch(loginId, request);
            var response = WorkTodoConvert.toInboxSummary(workTodoMapper.selectMyWorkTodoSummary(search), search.getToday(), request.getTimeZone());
            log.info("getMyWorkTodoSummary 완료 - loginId: {}, total: {}", loginId, response.totalCount());
            return response;
        } catch (RuntimeException exception) {
            log.error("getMyWorkTodoSummary 실패 - loginId: {}", loginId, exception);
            throw exception;
        }
    }

    // 요청의 소유자는 인증 계정에서 결정하며 필터와 날짜 범위를 검증한다.
    private WorkTodoListSearchVo prepareSearch(String loginId, WorkTodoListRequest request) {
        if (request.getGroup() == null || !WorkTodoPolicy.GROUPS.contains(request.getGroup()) || request.getPage() < 1 || request.getPage() > 1000000
                || request.getSize() < 1 || request.getSize() > 100
                || (request.getKeyword() != null && request.getKeyword().length() > 200)
                || (request.getFromDate() != null && request.getToDate() != null && request.getFromDate().isAfter(request.getToDate()))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "할 일 조회 조건을 확인해주세요.");
        }
        String workUnitUuid = StringUtil.normalizeBlank(request.getWorkUnitUuid());
        if (workUnitUuid != null) {
            try { workUnitUuid = UUID.fromString(workUnitUuid).toString(); }
            catch (IllegalArgumentException exception) { throw new BusinessException(ErrorCode.BAD_REQUEST, "올바른 업무 식별자를 지정해주세요."); }
        }
        return WorkTodoListSearchVo.builder()
                .mbrAcctIdx(accountLookupService.getAccountByLoginId(loginId).getIdx())
                .workUnitUuid(workUnitUuid).keyword(StringUtil.normalizeBlank(request.getKeyword()))
                .group(request.getGroup()).includeInactive(request.isIncludeInactive())
                .fromDate(request.getFromDate()).toDate(request.getToDate()).today(WorkTodoPolicy.today(request.getTimeZone()))
                .size(request.getSize()).offset((long) (request.getPage() - 1) * request.getSize()).build();
    }
}
