package com.gw.api.service.work;

import com.gw.api.convert.work.DailyReportConvert;
import com.gw.api.dto.work.AdminDailyReportMissingRequest;
import com.gw.api.dto.work.AdminDailyReportMissingResponse;
import com.gw.api.dto.work.AdminDailyReportResponse;
import com.gw.api.dto.work.CreateDailyReportRequest;
import com.gw.api.dto.work.DailyReportListRequest;
import com.gw.api.dto.work.DailyReportMissingRequest;
import com.gw.api.dto.work.DailyReportMissingResponse;
import com.gw.api.dto.work.DailyReportResponse;
import com.gw.api.dto.work.UpdateDailyReportRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.work.DailyReportMapper;
import com.gw.infra.db.mapper.work.WorkUnitMapper;
import com.gw.infra.db.support.PageSortSupport;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.WorkPolicy;
import com.gw.share.common.response.PageResponse;
import com.gw.share.util.DateUtil;
import com.gw.share.util.StringUtil;
import com.gw.share.util.ValidationUtil;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.DailyReportListSearchVo;
import com.gw.share.vo.work.DailyReportVo;
import com.gw.share.vo.work.WorkUnitVo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class DailyReportService {

    private final DailyReportMapper dailyReportMapper;
    private final WorkUnitMapper workUnitMapper;
    private final AccountLookupService accountLookupService;

    public DailyReportService(DailyReportMapper dailyReportMapper, WorkUnitMapper workUnitMapper, AccountLookupService accountLookupService) {
        this.dailyReportMapper = dailyReportMapper;
        this.workUnitMapper = workUnitMapper;
        this.accountLookupService = accountLookupService;
    }

    // 로그인 사용자의 일일보고 목록을 조회한다.
    @Transactional(readOnly = true)
    public PageResponse<DailyReportResponse> getDailyReportList(String loginId, DailyReportListRequest request) {
        log.info("getDailyReportList 시작 - loginId: {}, request: {}", loginId, request);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            LocalDate today = LocalDate.now();
            LocalDate dateFrom = request.dateFrom() == null ? today.minusDays(14) : request.dateFrom();
            LocalDate dateTo = request.dateTo() == null ? today : request.dateTo();
            validateDateRange(dateFrom, dateTo);

            int page = PageSortSupport.normalizePage(request.page());
            int size = PageSortSupport.normalizeSize(request.size());
            DailyReportListSearchVo searchVo = DailyReportListSearchVo.builder()
                    .mbrAcctIdx(account.getIdx())
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .kwd(normalizeText(request.keyword()))
                    .page(page)
                    .size(size)
                    .offset((page - 1) * size)
                    .build();

            List<DailyReportResponse> content = dailyReportMapper.selectDailyReportList(searchVo).stream()
                    .map(this::enrichDailyReport)
                    .map(DailyReportConvert::toResponse)
                    .toList();
            long totalCount = dailyReportMapper.countDailyReportList(searchVo);
            int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / size);

            log.info("getDailyReportList 완료 - loginId: {}, totalCount: {}", loginId, totalCount);
            return new PageResponse<>(content, page, size, totalCount, totalPages);
        } catch (BusinessException exception) {
            log.error(
                    "getDailyReportList 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 일일보고 단건을 조회한다.
    @Transactional(readOnly = true)
    public DailyReportResponse getDailyReport(String loginId, String uuid) {
        log.info("getDailyReport 시작 - loginId: {}, uuid: {}", loginId, uuid);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            DailyReportResponse response = DailyReportConvert.toResponse(enrichDailyReport(getDailyReport(uuid, account.getIdx())));
            log.info("getDailyReport 완료 - loginId: {}, uuid: {}", loginId, uuid);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getDailyReport 실패 - loginId: {}, uuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    uuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 일일보고를 생성한다.
    public DailyReportResponse createDailyReport(String loginId, CreateDailyReportRequest request) {
        log.info("createDailyReport 시작 - loginId: {}, request: {}", loginId, request);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            LocalDate reportDate = requireReportDate(request.reportDate());
            validateDuplicateReportDate(account.getIdx(), reportDate, null);
            List<WorkUnitVo> workUnits = resolveSelectedWorkUnits(account.getIdx(), request.workUnitUuids());
            log.info(
                    "createDailyReport 요청 검증 완료 - loginId: {}, memberAccountIdx: {}, reportDate: {}, requestedWorkUnitUuids: {}, resolvedWorkUnitCount: {}",
                    loginId,
                    account.getIdx(),
                    reportDate,
                    request.workUnitUuids(),
                    workUnits.size()
            );

            DailyReportVo dailyReport = DailyReportVo.builder()
                    .mbrAcctIdx(account.getIdx())
                    .rptDt(reportDate)
                    .cntn(resolveReportContent(request.content(), request.note()))
                    .sts(WorkPolicy.STATUS_IN_PROGRESS)
                    .spclNote(normalizeText(request.note()))
                    .createdBy(loginId)
                    .build();
            dailyReportMapper.insertDailyReport(dailyReport);
            syncDailyReportWorkUnits(dailyReport.getIdx(), account.getIdx(), workUnits, loginId);

            DailyReportResponse response = DailyReportConvert.toResponse(enrichDailyReport(getDailyReportByIdx(dailyReport.getIdx())));
            log.info("createDailyReport 완료 - loginId: {}, uuid: {}", loginId, response.uuid());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "createDailyReport 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 일일보고를 수정한다.
    public DailyReportResponse updateDailyReport(String loginId, String uuid, UpdateDailyReportRequest request) {
        log.info("updateDailyReport 시작 - loginId: {}, uuid: {}, request: {}", loginId, uuid, request);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            DailyReportVo dailyReport = getDailyReport(uuid, account.getIdx());
            List<WorkUnitVo> workUnits = resolveSelectedWorkUnits(account.getIdx(), request.workUnitUuids());
            log.info(
                    "updateDailyReport 요청 검증 완료 - loginId: {}, dailyReportUuid: {}, dailyReportIdx: {}, requestedWorkUnitUuids: {}, resolvedWorkUnitCount: {}",
                    loginId,
                    uuid,
                    dailyReport.getIdx(),
                    request.workUnitUuids(),
                    workUnits.size()
            );
            dailyReport.setCntn(resolveReportContent(request.content(), request.note()));
            dailyReport.setSts(WorkPolicy.STATUS_IN_PROGRESS);
            dailyReport.setSpclNote(normalizeText(request.note()));
            dailyReport.setUpdatedBy(loginId);
            dailyReportMapper.updateDailyReport(dailyReport);
            syncDailyReportWorkUnits(dailyReport.getIdx(), account.getIdx(), workUnits, loginId);

            DailyReportResponse response = DailyReportConvert.toResponse(enrichDailyReport(getDailyReport(uuid, account.getIdx())));
            log.info("updateDailyReport 완료 - loginId: {}, uuid: {}", loginId, uuid);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "updateDailyReport 실패 - loginId: {}, uuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    uuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 누락 일일보고 날짜를 조회한다.
    @Transactional(readOnly = true)
    public List<DailyReportMissingResponse> getMissingDailyReports(String loginId, DailyReportMissingRequest request) {
        log.info("getMissingDailyReports 시작 - loginId: {}, request: {}", loginId, request);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            LocalDate today = LocalDate.now();
            LocalDate dateFrom = request.dateFrom() == null ? today.withDayOfMonth(1) : request.dateFrom();
            LocalDate dateTo = request.dateTo() == null ? today : request.dateTo();
            validateDateRange(dateFrom, dateTo);

            List<LocalDate> missingDates = resolveMissingDates(account.getIdx(), dateFrom, dateTo);
            List<DailyReportMissingResponse> response = missingDates.stream()
                    .map(DailyReportMissingResponse::new)
                    .toList();
            log.info("getMissingDailyReports 완료 - loginId: {}, missingCount: {}", loginId, response.size());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getMissingDailyReports 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 관리자용 일일보고 목록을 조회한다.
    @Transactional(readOnly = true)
    public PageResponse<AdminDailyReportResponse> getAdminDailyReports(DailyReportListRequest request) {
        log.info("getAdminDailyReports 시작 - request: {}", request);
        try {
            LocalDate today = LocalDate.now();
            LocalDate dateFrom = request.dateFrom() == null ? today.withDayOfMonth(1) : request.dateFrom();
            LocalDate dateTo = request.dateTo() == null ? today : request.dateTo();
            validateDateRange(dateFrom, dateTo);

            int page = PageSortSupport.normalizePage(request.page());
            int size = PageSortSupport.normalizeSize(request.size());
            DailyReportListSearchVo searchVo = DailyReportListSearchVo.builder()
                    .mbrAcctUuid(normalizeText(request.memberUuid()))
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .kwd(normalizeText(request.keyword()))
                    .page(page)
                    .size(size)
                    .offset((page - 1) * size)
                    .build();

            List<AdminDailyReportResponse> content = dailyReportMapper.selectAdminDailyReportList(searchVo).stream()
                    .map(this::enrichDailyReport)
                    .map(DailyReportConvert::toAdminResponse)
                    .toList();
            long totalCount = dailyReportMapper.countAdminDailyReportList(searchVo);
            int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / size);

            log.info("getAdminDailyReports 완료 - totalCount: {}", totalCount);
            return new PageResponse<>(content, page, size, totalCount, totalPages);
        } catch (BusinessException exception) {
            log.error(
                    "getAdminDailyReports 실패 - 원인: {}, detailMessage: {}",
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 관리자용 누락 일일보고 현황을 조회한다.
    @Transactional(readOnly = true)
    public List<AdminDailyReportMissingResponse> getAdminMissingDailyReports(AdminDailyReportMissingRequest request) {
        log.info("getAdminMissingDailyReports 시작 - request: {}", request);
        try {
            LocalDate today = LocalDate.now();
            LocalDate dateFrom = request.dateFrom() == null ? today.withDayOfMonth(1) : request.dateFrom();
            LocalDate dateTo = request.dateTo() == null ? today : request.dateTo();
            validateDateRange(dateFrom, dateTo);

            List<AdminDailyReportMissingResponse> response = dailyReportMapper.selectAdminMissingMembers(normalizeText(request.memberUuid())).stream()
                    .map(member -> {
                        List<LocalDate> missingDates = resolveMissingDates(member.getMbrAcctIdx(), dateFrom, dateTo);
                        List<LocalDate> writtenDates = dailyReportMapper.selectWrittenDates(member.getMbrAcctIdx(), dateFrom, dateTo);
                        LocalDate lastWrittenDate = writtenDates.isEmpty() ? null : writtenDates.get(writtenDates.size() - 1);
                        return DailyReportConvert.toAdminMissingResponse(
                                member.getMbrAcctUuid(),
                                member.getLgnId(),
                                member.getNickNm(),
                                missingDates,
                                lastWrittenDate
                        );
                    })
                    .toList();
            log.info("getAdminMissingDailyReports 완료 - memberCount: {}", response.size());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getAdminMissingDailyReports 실패 - 원인: {}, detailMessage: {}",
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 조회 기간 내 누락된 평일 날짜를 계산한다.
    private List<LocalDate> resolveMissingDates(Long mbrAcctIdx, LocalDate dateFrom, LocalDate dateTo) {
        List<LocalDate> writtenDates = dailyReportMapper.selectWrittenDates(mbrAcctIdx, dateFrom, dateTo);
        List<LocalDate> missingDates = new ArrayList<>();
        LocalDate currentDate = dateFrom;

        while (!currentDate.isAfter(dateTo)) {
            if (!DateUtil.isWeekend(currentDate) && !writtenDates.contains(currentDate)) {
                missingDates.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        return missingDates;
    }

    // UUID와 소유 계정으로 일일보고를 조회한다.
    private DailyReportVo getDailyReport(String uuid, Long mbrAcctIdx) {
        DailyReportVo dailyReport = dailyReportMapper.selectDailyReport(uuid, mbrAcctIdx);

        if (dailyReport == null) {
            log.error("getDailyReport 실패 - 원인: 일일보고를 찾을 수 없습니다. uuid={}, memberAccountIdx={}", uuid, mbrAcctIdx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "일일보고를 찾을 수 없습니다.");
        }

        return dailyReport;
    }

    // PK로 일일보고를 조회한다.
    private DailyReportVo getDailyReportByIdx(Long idx) {
        DailyReportVo dailyReport = dailyReportMapper.selectDailyReportByIdx(idx);

        if (dailyReport == null) {
            log.error("getDailyReportByIdx 실패 - 원인: 일일보고를 찾을 수 없습니다. idx={}", idx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "일일보고를 찾을 수 없습니다.");
        }

        return dailyReport;
    }

    // 로그인 ID로 회원 계정을 조회한다.
    private AcctVo getAccountByLoginId(String loginId) {
        return accountLookupService.getAccountByLoginId(loginId);
    }

    // 동일 날짜 일일보고 중복 여부를 검증한다.
    private void validateDuplicateReportDate(Long mbrAcctIdx, LocalDate rptDt, String excludeUuid) {
        if (dailyReportMapper.existsDailyReportByDate(mbrAcctIdx, rptDt, excludeUuid)) {
            log.error(
                    "validateDuplicateReportDate 실패 - 원인: 같은 날짜의 일일보고가 이미 존재합니다. memberAccountIdx={}, reportDate={}, excludeUuid={}",
                    mbrAcctIdx,
                    rptDt,
                    excludeUuid
            );
            throw new BusinessException(ErrorCode.DUPLICATE, "같은 날짜의 일일보고가 이미 존재합니다.");
        }
    }

    // 시작일과 종료일의 순서를 검증한다.
    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        DateUtil.validateDateOrder(dateFrom, dateTo, ErrorCode.BAD_REQUEST, "시작일은 종료일보다 늦을 수 없습니다.");
    }

    // 일일보고 작성일 필수 여부를 검증한다.
    private LocalDate requireReportDate(LocalDate reportDate) {
        return ValidationUtil.requireNonNull(reportDate, ErrorCode.BAD_REQUEST, "reportDate는 필수입니다.");
    }

    // 문자열 입력값의 공백을 정리한다.
    private String normalizeText(String value) {
        return StringUtil.normalizeBlank(value);
    }

    // 본문 입력값만 저장용 본문으로 사용한다.
    private String resolveReportContent(String content, String note) {
        return normalizeText(content);
    }

    // 선택된 업무 UUID 목록을 실제 업무 VO 목록으로 변환한다.
    private List<WorkUnitVo> resolveSelectedWorkUnits(Long mbrAcctIdx, List<String> workUnitUuids) {
        if (workUnitUuids == null || workUnitUuids.isEmpty()) {
            log.info("resolveSelectedWorkUnits: memberAccountIdx={}, requestedWorkUnitUuids is empty", mbrAcctIdx);
            return List.of();
        }

        List<String> normalizedUuids = workUnitUuids.stream()
                .map(this::normalizeText)
                .filter(value -> value != null)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));

        if (normalizedUuids.isEmpty()) {
            log.info("resolveSelectedWorkUnits: memberAccountIdx={}, normalized work unit uuids are empty", mbrAcctIdx);
            return List.of();
        }

        List<WorkUnitVo> workUnits = workUnitMapper.selectWorkUnitsByUuids(mbrAcctIdx, normalizedUuids);
        log.info(
                "resolveSelectedWorkUnits: memberAccountIdx={}, normalizedWorkUnitUuids={}, resolvedWorkUnitUuids={}",
                mbrAcctIdx,
                normalizedUuids,
                workUnits.stream().map(WorkUnitVo::getUuid).toList()
        );

        if (workUnits.size() != normalizedUuids.size()) {
            log.warn(
                    "resolveSelectedWorkUnits mismatch: memberAccountIdx={}, requestedCount={}, resolvedCount={}, requestedWorkUnitUuids={}",
                    mbrAcctIdx,
                    normalizedUuids.size(),
                    workUnits.size(),
                    normalizedUuids
            );
            throw new BusinessException(ErrorCode.BAD_REQUEST, "선택한 업무등록 항목 중 유효하지 않은 항목이 있습니다.");
        }

        Map<String, WorkUnitVo> workUnitByUuid = workUnits.stream()
                .collect(Collectors.toMap(WorkUnitVo::getUuid, Function.identity()));

        return normalizedUuids.stream()
                .map(workUnitByUuid::get)
                .toList();
    }

    // 일일보고와 선택 업무 연결 정보를 동기화한다.
    private void syncDailyReportWorkUnits(Long dailyReportIdx, Long mbrAcctIdx, List<WorkUnitVo> workUnits, String loginId) {
        List<Long> previousWorkUnitIdxs = dailyReportMapper.selectDailyReportWorkUnits(dailyReportIdx).stream()
                .map(WorkUnitVo::getIdx)
                .toList();
        List<Long> requestedWorkUnitIdxs = workUnits.stream()
                .map(WorkUnitVo::getIdx)
                .toList();
        List<Long> affectedWorkUnitIdxs = new ArrayList<>(new LinkedHashSet<>());
        affectedWorkUnitIdxs.addAll(previousWorkUnitIdxs);
        affectedWorkUnitIdxs.addAll(requestedWorkUnitIdxs);
        log.info(
                "syncDailyReportWorkUnits start: dailyReportIdx={}, memberAccountIdx={}, previousWorkUnitIdxs={}, requestedWorkUnitIdxs={}",
                dailyReportIdx,
                mbrAcctIdx,
                previousWorkUnitIdxs,
                requestedWorkUnitIdxs
        );
        dailyReportMapper.deleteDailyReportWorkUnits(dailyReportIdx);

        for (WorkUnitVo workUnit : workUnits) {
            dailyReportMapper.insertDailyReportWorkUnit(dailyReportIdx, mbrAcctIdx, workUnit.getIdx(), loginId);
        }

        int savedCount = dailyReportMapper.countDailyReportWorkUnits(dailyReportIdx);
        log.info(
                "syncDailyReportWorkUnits result: dailyReportIdx={}, expectedCount={}, savedCount={}",
                dailyReportIdx,
                workUnits.size(),
                savedCount
        );

        if (savedCount != workUnits.size()) {
            log.error(
                    "syncDailyReportWorkUnits failed: dailyReportIdx={}, memberAccountIdx={}, expectedCount={}, savedCount={}",
                    dailyReportIdx,
                    mbrAcctIdx,
                    workUnits.size(),
                    savedCount
            );
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "선택한 업무 연결 저장에 실패했습니다.");
        }

        if (!affectedWorkUnitIdxs.isEmpty()) {
            int refreshedCount = workUnitMapper.refreshWorkUnitUsageStats(mbrAcctIdx, affectedWorkUnitIdxs);
            log.info(
                    "syncDailyReportWorkUnits usage stats refreshed: dailyReportIdx={}, memberAccountIdx={}, affectedWorkUnitIdxs={}, refreshedCount={}",
                    dailyReportIdx,
                    mbrAcctIdx,
                    affectedWorkUnitIdxs,
                    refreshedCount
            );
        }
    }

    // 일일보고에 연결된 업무 목록을 채운다.
    private <T extends DailyReportVo> T enrichDailyReport(T dailyReport) {
        dailyReport.setWorkUnits(dailyReportMapper.selectDailyReportWorkUnits(dailyReport.getIdx()));
        return dailyReport;
    }
}
