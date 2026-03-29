package com.gw.api.service.work;

import com.gw.api.convert.work.WorkUnitConvert;
import com.gw.api.dto.work.CreateWorkUnitRequest;
import com.gw.api.dto.work.UpdateWorkUnitRequest;
import com.gw.api.dto.work.WorkUnitListRequest;
import com.gw.api.dto.work.WorkUnitOptionResponse;
import com.gw.api.dto.work.WorkUnitResponse;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.work.WorkUnitMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.util.StringUtil;
import com.gw.share.util.ValidationUtil;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.WorkUnitListSearchVo;
import com.gw.share.vo.work.WorkUnitVo;
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
    private final AccountMapper accountMapper;

    public WorkUnitService(WorkUnitMapper workUnitMapper, AccountMapper accountMapper) {
        this.workUnitMapper = workUnitMapper;
        this.accountMapper = accountMapper;
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

            List<WorkUnitResponse> response = workUnitMapper.selectWorkUnitList(query).stream()
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

    // 로그인 사용자의 업무를 생성한다.
    public WorkUnitResponse createWorkUnit(String loginId, CreateWorkUnitRequest request) {
        log.info("createWorkUnit 시작 - loginId: {}, request: {}", loginId, request);
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
                    .useYn("Y")
                    .createdBy(loginId)
                    .build();
            workUnitMapper.insertWorkUnit(workUnit);

            WorkUnitResponse response = WorkUnitConvert.toResponse(getWorkUnitByIdx(workUnit.getIdx()));
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
        log.info("updateWorkUnit 시작 - loginId: {}, uuid: {}, request: {}", loginId, uuid, request);
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

            WorkUnitResponse response = WorkUnitConvert.toResponse(getWorkUnit(uuid, account.getIdx()));
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

    // UUID와 소유 계정으로 업무를 조회한다.
    private WorkUnitVo getWorkUnit(String uuid, Long mbrAcctIdx) {
        WorkUnitVo workUnit = workUnitMapper.selectWorkUnit(uuid, mbrAcctIdx);

        if (workUnit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다.");
        }

        return workUnit;
    }

    // PK로 업무를 조회한다.
    private WorkUnitVo getWorkUnitByIdx(Long idx) {
        WorkUnitVo workUnit = workUnitMapper.selectWorkUnitByIdx(idx);

        if (workUnit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다.");
        }

        return workUnit;
    }

    // 로그인 ID로 회원 계정을 조회한다.
    private AcctVo getAccountByLoginId(String loginId) {
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        return account;
    }

    // 동일 회원의 업무명 중복 여부를 검증한다.
    private void validateDuplicateTitle(Long mbrAcctIdx, String title, String excludeUuid) {
        if (workUnitMapper.existsTitle(mbrAcctIdx, title, excludeUuid)) {
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
            case "IN_PROGRESS", "DONE", "ON_HOLD" -> normalized;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "올바르지 않은 업무 상태입니다.");
        };
    }

    // 업무 상태 입력값이 없으면 기본 상태로 보정한다.
    private String normalizeStatusOrDefault(String status) {
        String normalized = normalizeStatus(status);
        return normalized == null ? "IN_PROGRESS" : normalized;
    }

    // 사용 여부 입력값을 정규화하고 기본값을 보정한다.
    private String normalizeUseYn(String useYn) {
        String normalized = normalizeText(useYn);

        if (normalized == null) {
            return "Y";
        }

        return switch (normalized) {
            case "Y", "N" -> normalized;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "useYn은 Y 또는 N 이어야 합니다.");
        };
    }

    // 선택적 사용 여부 입력값을 정규화한다.
    private String normalizeOptionalUseYn(String useYn) {
        String normalized = normalizeText(useYn);

        if (normalized == null) {
            return null;
        }

        return switch (normalized) {
            case "Y", "N" -> normalized;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "useYn은 Y 또는 N 이어야 합니다.");
        };
    }

    // 정렬 키에 맞는 ORDER BY 절을 결정한다.
    private String resolveOrderByClause(String sort) {
        String normalized = normalizeText(sort);
        return SORT_CLAUSE_MAP.getOrDefault(normalized == null ? "updated" : normalized, SORT_CLAUSE_MAP.get("updated"));
    }
}
