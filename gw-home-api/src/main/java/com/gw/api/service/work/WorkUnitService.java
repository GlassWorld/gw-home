package com.gw.api.service.work;

import com.gw.api.convert.work.WorkUnitConvert;
import com.gw.api.dto.work.CreateWorkUnitRequest;
import com.gw.api.dto.work.UpdateWorkUnitRequest;
import com.gw.api.dto.work.WorkUnitGitCommitResponse;
import com.gw.api.dto.work.WorkUnitListRequest;
import com.gw.api.dto.work.WorkUnitOptionResponse;
import com.gw.api.dto.work.WorkUnitResponse;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.work.WorkGitMapper;
import com.gw.infra.db.mapper.work.WorkUnitMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.UseYnPolicy;
import com.gw.share.common.policy.WorkPolicy;
import com.gw.share.util.StringUtil;
import com.gw.share.util.ValidationUtil;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.WorkGitPrjVo;
import com.gw.share.vo.work.WorkUnitListSearchVo;
import com.gw.share.vo.work.WorkUnitVo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class WorkUnitService {

    private static final Map<String, String> SORT_CLAUSE_MAP = Map.of(
            "updated", "wu.use_yn DESC, wu.updated_at DESC, wu.created_at DESC",
            "recent", "wu.use_yn DESC, COALESCE(wu.last_used_at, wu.updated_at) DESC, wu.updated_at DESC",
            "frequent", "wu.use_yn DESC, wu.use_cnt DESC, wu.updated_at DESC"
    );

    private final WorkUnitMapper workUnitMapper;
    private final WorkGitMapper workGitMapper;
    private final AccountLookupService accountLookupService;
    private final WorkGitCommitClient workGitCommitClient;

    public WorkUnitService(
            WorkUnitMapper workUnitMapper,
            WorkGitMapper workGitMapper,
            AccountLookupService accountLookupService,
            WorkGitCommitClient workGitCommitClient
    ) {
        this.workUnitMapper = workUnitMapper;
        this.workGitMapper = workGitMapper;
        this.accountLookupService = accountLookupService;
        this.workGitCommitClient = workGitCommitClient;
    }

    // 로그인 사용자의 업무 목록을 조회한다.
    @Transactional(readOnly = true)
    public List<WorkUnitResponse> getWorkUnitList(String loginId, WorkUnitListRequest request) {
        log.info("getWorkUnitList 시작 - loginId: {}, request: {}", loginId, request);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkUnitListSearchVo query = WorkUnitListSearchVo.builder()
                    .mbrAcctIdx(account.getIdx())
                    .kwd(normalizeKeyword(request.keyword()))
                    .ctgr(normalizeText(request.category()))
                    .sts(normalizeStatus(request.status()))
                    .useYn(normalizeOptionalUseYn(request.useYn()))
                    .orderByClause(resolveOrderByClause(request.sort()))
                    .build();

            List<WorkUnitResponse> response = enrichWorkUnits(workUnitMapper.selectWorkUnitList(query), account.getIdx()).stream()
                    .map(WorkUnitConvert::toResponse)
                    .toList();
            log.info("getWorkUnitList 완료 - loginId: {}, count: {}", loginId, response.size());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getWorkUnitList 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 업무 선택 옵션 목록을 조회한다.
    @Transactional(readOnly = true)
    public List<WorkUnitOptionResponse> getWorkUnitOptions(String loginId, Boolean includeInactive) {
        log.info("getWorkUnitOptions 시작 - loginId: {}, includeInactive: {}", loginId, includeInactive);
        try {
            AcctVo account = getAccountByLoginId(loginId);

            List<WorkUnitOptionResponse> response =
                    workUnitMapper.selectWorkUnitOptions(account.getIdx(), Boolean.TRUE.equals(includeInactive)).stream()
                            .map(WorkUnitConvert::toOptionResponse)
                            .toList();
            log.info("getWorkUnitOptions 완료 - loginId: {}, count: {}", loginId, response.size());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getWorkUnitOptions 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    // 로그인 사용자의 업무 상세를 조회한다.
    public WorkUnitResponse getWorkUnit(String loginId, String uuid) {
        log.info("getWorkUnit 시작 - loginId: {}, uuid: {}", loginId, uuid);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkUnitResponse response = WorkUnitConvert.toResponse(enrichWorkUnit(getWorkUnit(uuid, account.getIdx()), account.getIdx()));
            log.info("getWorkUnit 완료 - loginId: {}, uuid: {}", loginId, uuid);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getWorkUnit 실패 - loginId: {}, uuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    uuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 업무를 생성한다.
    public WorkUnitResponse createWorkUnit(String loginId, CreateWorkUnitRequest request) {
        log.info("createWorkUnit 시작 - loginId: {}, title: {}", loginId, request.title());
        try {
            AcctVo account = getAccountByLoginId(loginId);
            String normalizedTitle = normalizeRequiredTitle(request.title());
            validateDuplicateTitle(account.getIdx(), normalizedTitle, null);

            WorkUnitVo workUnit = WorkUnitVo.builder()
                    .mbrAcctIdx(account.getIdx())
                    .ttl(normalizedTitle)
                    .dscr(normalizeText(request.description()))
                    .ctgr(normalizeText(request.category()))
                    .sts(normalizeStatusOrDefault(request.status()))
                    .useYn(UseYnPolicy.YES)
                    .createdBy(loginId)
                    .build();
            workUnitMapper.insertWorkUnit(workUnit);
            syncGitProjects(workUnit.getIdx(), account.getIdx(), request.gitProjectUuids(), loginId);

            WorkUnitResponse response = WorkUnitConvert.toResponse(enrichWorkUnit(getWorkUnitByIdx(workUnit.getIdx()), account.getIdx()));
            log.info("createWorkUnit 완료 - loginId: {}, uuid: {}", loginId, response.workUnitUuid());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "createWorkUnit 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 업무를 수정한다.
    public WorkUnitResponse updateWorkUnit(String loginId, String uuid, UpdateWorkUnitRequest request) {
        log.info("updateWorkUnit 시작 - loginId: {}, uuid: {}, title: {}", loginId, uuid, request.title());
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkUnitVo workUnit = getWorkUnit(uuid, account.getIdx());
            String normalizedTitle = normalizeRequiredTitle(request.title());
            validateDuplicateTitle(account.getIdx(), normalizedTitle, uuid);

            workUnit.setTtl(normalizedTitle);
            workUnit.setDscr(normalizeText(request.description()));
            workUnit.setCtgr(normalizeText(request.category()));
            workUnit.setSts(normalizeStatusOrDefault(request.status()));
            workUnit.setUpdatedBy(loginId);
            workUnitMapper.updateWorkUnit(workUnit);
            syncGitProjects(workUnit.getIdx(), account.getIdx(), request.gitProjectUuids(), loginId);

            WorkUnitResponse response = WorkUnitConvert.toResponse(enrichWorkUnit(getWorkUnit(uuid, account.getIdx()), account.getIdx()));
            log.info("updateWorkUnit 완료 - loginId: {}, uuid: {}", loginId, uuid);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "updateWorkUnit 실패 - loginId: {}, uuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    uuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 업무 사용 여부를 변경한다.
    public WorkUnitResponse updateWorkUnitUse(String loginId, String uuid, String useYn) {
        log.info("updateWorkUnitUse 시작 - loginId: {}, uuid: {}, useYn: {}", loginId, uuid, useYn);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkUnitVo workUnit = getWorkUnit(uuid, account.getIdx());
            workUnitMapper.updateWorkUnitUse(workUnit.getUuid(), account.getIdx(), normalizeUseYn(useYn), loginId);
            WorkUnitResponse response = WorkUnitConvert.toResponse(getWorkUnit(uuid, account.getIdx()));
            log.info("updateWorkUnitUse 완료 - loginId: {}, uuid: {}, useYn: {}", loginId, uuid, response.useYn());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "updateWorkUnitUse 실패 - loginId: {}, uuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    uuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    // 로그인 사용자의 업무와 연결된 Git 커밋을 조회한다.
    public List<WorkUnitGitCommitResponse> getWorkUnitGitCommits(String loginId, String uuid, LocalDate reportDate) {
        log.info("getWorkUnitGitCommits 시작 - loginId: {}, uuid: {}, reportDate: {}", loginId, uuid, reportDate);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            ValidationUtil.requireNonNull(reportDate, ErrorCode.BAD_REQUEST, "reportDate는 필수입니다.");
            WorkUnitVo workUnit = enrichWorkUnit(getWorkUnit(uuid, account.getIdx()), account.getIdx());
            List<WorkUnitGitCommitResponse> response = workGitCommitClient.fetchCommits(workUnit.getGitProjects(), reportDate);
            log.info("getWorkUnitGitCommits 완료 - loginId: {}, uuid: {}, count: {}", loginId, uuid, response.size());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getWorkUnitGitCommits 실패 - loginId: {}, uuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    uuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // UUID와 소유 계정으로 업무를 조회한다.
    private WorkUnitVo getWorkUnit(String uuid, Long mbrAcctIdx) {
        WorkUnitVo workUnit = workUnitMapper.selectWorkUnit(uuid, mbrAcctIdx);

        if (workUnit == null) {
            log.error("getWorkUnit 실패 - 원인: 업무를 찾을 수 없습니다. uuid={}, memberAccountIdx={}", uuid, mbrAcctIdx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다.");
        }

        return workUnit;
    }

    // PK로 업무를 조회한다.
    private WorkUnitVo getWorkUnitByIdx(Long idx) {
        WorkUnitVo workUnit = workUnitMapper.selectWorkUnitByIdx(idx);

        if (workUnit == null) {
            log.error("getWorkUnitByIdx 실패 - 원인: 업무를 찾을 수 없습니다. idx={}", idx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다.");
        }

        return workUnit;
    }

    // 로그인 ID로 회원 계정을 조회한다.
    private AcctVo getAccountByLoginId(String loginId) {
        return accountLookupService.getAccountByLoginId(loginId);
    }

    // 동일 회원의 업무명 중복 여부를 검증한다.
    private void validateDuplicateTitle(Long mbrAcctIdx, String title, String excludeUuid) {
        if (workUnitMapper.existsTitle(mbrAcctIdx, title, excludeUuid)) {
            log.error(
                    "validateDuplicateTitle 실패 - 원인: 동일한 업무명이 이미 등록되어 있습니다. memberAccountIdx={}, title={}, excludeUuid={}",
                    mbrAcctIdx,
                    title,
                    excludeUuid
            );
            throw new BusinessException(ErrorCode.DUPLICATE, "동일한 업무명이 이미 등록되어 있습니다.");
        }
    }

    // 업무명 입력값을 정규화하고 필수 여부를 검증한다.
    private String normalizeRequiredTitle(String title) {
        String normalized = normalizeText(title);
        ValidationUtil.requireNonNull(normalized, ErrorCode.BAD_REQUEST, "업무명은 필수입니다.");
        return normalized;
    }

    // 검색 키워드 입력값을 정규화한다.
    private String normalizeKeyword(String keyword) {
        return normalizeText(keyword);
    }

    // 문자열 입력값의 공백을 정리한다.
    private String normalizeText(String value) {
        return StringUtil.normalizeBlank(value);
    }

    // 업무 상태 입력값을 정규화한다.
    private String normalizeStatus(String status) {
        String normalized = normalizeText(status);

        if (normalized == null) {
            return null;
        }

        return switch (normalized) {
            case WorkPolicy.STATUS_IN_PROGRESS, WorkPolicy.STATUS_DONE, WorkPolicy.STATUS_ON_HOLD -> normalized;
            default -> {
                log.error("normalizeStatus 실패 - 원인: 올바르지 않은 업무 상태입니다. status={}", status);
                throw new BusinessException(ErrorCode.BAD_REQUEST, "올바르지 않은 업무 상태입니다.");
            }
        };
    }

    // 업무 상태 입력값이 없으면 기본 상태로 보정한다.
    private String normalizeStatusOrDefault(String status) {
        String normalized = normalizeStatus(status);
        return normalized == null ? WorkPolicy.STATUS_IN_PROGRESS : normalized;
    }

    // 사용 여부 입력값을 정규화하고 기본값을 보정한다.
    private String normalizeUseYn(String useYn) {
        String normalized = normalizeText(useYn);

        if (normalized == null) {
            return UseYnPolicy.YES;
        }

        return switch (normalized) {
            case UseYnPolicy.YES, UseYnPolicy.NO -> normalized;
            default -> {
                log.error("normalizeUseYn 실패 - 원인: useYn 값이 올바르지 않습니다. useYn={}", useYn);
                throw new BusinessException(ErrorCode.BAD_REQUEST, "useYn은 Y 또는 N 이어야 합니다.");
            }
        };
    }

    // 선택적 사용 여부 입력값을 정규화한다.
    private String normalizeOptionalUseYn(String useYn) {
        String normalized = normalizeText(useYn);

        if (normalized == null) {
            return null;
        }

        return switch (normalized) {
            case UseYnPolicy.YES, UseYnPolicy.NO -> normalized;
            default -> {
                log.error("normalizeOptionalUseYn 실패 - 원인: useYn 값이 올바르지 않습니다. useYn={}", useYn);
                throw new BusinessException(ErrorCode.BAD_REQUEST, "useYn은 Y 또는 N 이어야 합니다.");
            }
        };
    }

    // 정렬 키에 맞는 ORDER BY 절을 결정한다.
    private String resolveOrderByClause(String sort) {
        String normalized = normalizeText(sort);
        return SORT_CLAUSE_MAP.getOrDefault(normalized == null ? "updated" : normalized, SORT_CLAUSE_MAP.get("updated"));
    }

    private WorkUnitVo enrichWorkUnit(WorkUnitVo workUnit, Long mbrAcctIdx) {
        List<WorkUnitVo> workUnits = enrichWorkUnits(List.of(workUnit), mbrAcctIdx);
        return workUnits.isEmpty() ? workUnit : workUnits.getFirst();
    }

    private List<WorkUnitVo> enrichWorkUnits(List<WorkUnitVo> workUnits, Long mbrAcctIdx) {
        if (workUnits == null || workUnits.isEmpty()) {
            return List.of();
        }

        List<Long> workUnitIdxs = workUnits.stream()
                .map(WorkUnitVo::getIdx)
                .toList();

        Map<Long, List<WorkGitPrjVo>> gitProjectMap = new LinkedHashMap<>();
        for (WorkGitPrjVo gitProject : workGitMapper.selectWorkUnitGitProjectsByWorkUnitIdxs(workUnitIdxs, mbrAcctIdx)) {
            gitProjectMap.computeIfAbsent(gitProject.getWorkUnitIdx(), key -> new ArrayList<>()).add(gitProject);
        }

        for (WorkUnitVo workUnit : workUnits) {
            workUnit.setGitProjects(gitProjectMap.getOrDefault(workUnit.getIdx(), List.of()));
        }

        return workUnits;
    }

    private void syncGitProjects(
            Long workUnitIdx,
            Long mbrAcctIdx,
            List<String> gitProjectUuids,
            String loginId
    ) {
        workGitMapper.deleteWorkUnitGitProjects(workUnitIdx, mbrAcctIdx, loginId);

        if (gitProjectUuids == null || gitProjectUuids.isEmpty()) {
            return;
        }

        List<WorkGitPrjVo> gitProjects = workGitMapper.selectGitProjectsByUuids(mbrAcctIdx, gitProjectUuids);
        if (gitProjects.size() != gitProjectUuids.size()) {
            log.error(
                    "syncGitProjects 실패 - 원인: 연결할 Git 프로젝트를 찾을 수 없습니다. memberAccountIdx={}, requestedCount={}, foundCount={}",
                    mbrAcctIdx,
                    gitProjectUuids.size(),
                    gitProjects.size()
            );
            throw new BusinessException(ErrorCode.BAD_REQUEST, "연결할 Git 프로젝트를 찾을 수 없습니다.");
        }

        for (WorkGitPrjVo gitProject : gitProjects) {
            workGitMapper.insertWorkUnitGitProject(workUnitIdx, gitProject.getIdx(), mbrAcctIdx, loginId);
        }
    }
}
