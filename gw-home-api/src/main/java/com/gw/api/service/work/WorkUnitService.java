package com.gw.api.service.work;

import com.gw.api.dto.work.CreateWorkUnitRequest;
import com.gw.api.dto.work.UpdateWorkUnitRequest;
import com.gw.api.dto.work.WorkUnitListRequest;
import com.gw.api.dto.work.WorkUnitOptionResponse;
import com.gw.api.dto.work.WorkUnitResponse;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.work.WorkUnitMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.WorkUnitListSearchVo;
import com.gw.share.vo.work.WorkUnitVo;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
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

    @Transactional(readOnly = true)
    public List<WorkUnitResponse> getWorkUnitList(String loginId, WorkUnitListRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        WorkUnitListSearchVo query = WorkUnitListSearchVo.builder()
                .mbrAcctIdx(account.getIdx())
                .kwd(normalizeKeyword(request.keyword()))
                .ctgr(normalizeText(request.category()))
                .sts(normalizeStatus(request.status()))
                .useYn(normalizeOptionalUseYn(request.useYn()))
                .orderByClause(resolveOrderByClause(request.sort()))
                .build();

        return workUnitMapper.selectWorkUnitList(query).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkUnitOptionResponse> getWorkUnitOptions(String loginId, Boolean includeInactive) {
        AcctVo account = getAccountByLoginId(loginId);

        return workUnitMapper.selectWorkUnitOptions(account.getIdx(), Boolean.TRUE.equals(includeInactive)).stream()
                .map(this::toOptionResponse)
                .toList();
    }

    public WorkUnitResponse createWorkUnit(String loginId, CreateWorkUnitRequest request) {
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

        return toResponse(getWorkUnitByIdx(workUnit.getIdx()));
    }

    public WorkUnitResponse updateWorkUnit(String loginId, String uuid, UpdateWorkUnitRequest request) {
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

        return toResponse(getWorkUnit(uuid, account.getIdx()));
    }

    public WorkUnitResponse updateWorkUnitUse(String loginId, String uuid, String useYn) {
        AcctVo account = getAccountByLoginId(loginId);
        WorkUnitVo workUnit = getWorkUnit(uuid, account.getIdx());
        workUnitMapper.updateWorkUnitUse(workUnit.getUuid(), account.getIdx(), normalizeUseYn(useYn), loginId);
        return toResponse(getWorkUnit(uuid, account.getIdx()));
    }

    private WorkUnitVo getWorkUnit(String uuid, Long mbrAcctIdx) {
        WorkUnitVo workUnit = workUnitMapper.selectWorkUnit(uuid, mbrAcctIdx);

        if (workUnit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다.");
        }

        return workUnit;
    }

    private WorkUnitVo getWorkUnitByIdx(Long idx) {
        WorkUnitVo workUnit = workUnitMapper.selectWorkUnitByIdx(idx);

        if (workUnit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다.");
        }

        return workUnit;
    }

    private AcctVo getAccountByLoginId(String loginId) {
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        return account;
    }

    private void validateDuplicateTitle(Long mbrAcctIdx, String title, String excludeUuid) {
        if (workUnitMapper.existsTitle(mbrAcctIdx, title, excludeUuid)) {
            throw new BusinessException(ErrorCode.DUPLICATE, "동일한 업무명이 이미 등록되어 있습니다.");
        }
    }

    private String normalizeRequiredTitle(String title) {
        String normalized = normalizeText(title);

        if (normalized == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "업무명은 필수입니다.");
        }

        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        return normalizeText(keyword);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

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

    private String normalizeStatusOrDefault(String status) {
        String normalized = normalizeStatus(status);
        return normalized == null ? "IN_PROGRESS" : normalized;
    }

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

    private String resolveOrderByClause(String sort) {
        String normalized = normalizeText(sort);
        return SORT_CLAUSE_MAP.getOrDefault(normalized == null ? "updated" : normalized, SORT_CLAUSE_MAP.get("updated"));
    }

    private WorkUnitResponse toResponse(WorkUnitVo workUnit) {
        return new WorkUnitResponse(
                workUnit.getUuid(),
                workUnit.getTtl(),
                workUnit.getDscr(),
                workUnit.getCtgr(),
                workUnit.getSts(),
                workUnit.getUseYn(),
                workUnit.getUseCnt() == null ? 0 : workUnit.getUseCnt(),
                workUnit.getLastUsedAt(),
                workUnit.getCreatedAt(),
                workUnit.getUpdatedAt()
        );
    }

    private WorkUnitOptionResponse toOptionResponse(WorkUnitVo workUnit) {
        return new WorkUnitOptionResponse(
                workUnit.getUuid(),
                workUnit.getTtl(),
                workUnit.getCtgr(),
                workUnit.getSts(),
                workUnit.getUseYn()
        );
    }
}
