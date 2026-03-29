package com.gw.api.service.work;

import com.gw.api.dto.work.AdminDailyReportMissingRequest;
import com.gw.api.dto.work.AdminDailyReportMissingResponse;
import com.gw.api.dto.work.AdminDailyReportResponse;
import com.gw.api.dto.work.CreateDailyReportRequest;
import com.gw.api.dto.work.DailyReportListRequest;
import com.gw.api.dto.work.DailyReportMissingRequest;
import com.gw.api.dto.work.DailyReportMissingResponse;
import com.gw.api.dto.work.DailyReportResponse;
import com.gw.api.dto.work.DailyReportWorkUnitResponse;
import com.gw.api.dto.work.UpdateDailyReportRequest;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.work.DailyReportMapper;
import com.gw.infra.db.mapper.work.WorkUnitMapper;
import com.gw.infra.db.support.PageSortSupport;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.PageResponse;
import com.gw.share.jvo.work.DailyReportAdmJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.DailyReportListSearchVo;
import com.gw.share.vo.work.DailyReportVo;
import com.gw.share.vo.work.WorkUnitVo;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DailyReportService {

    private static final String DEFAULT_DAILY_REPORT_STATUS = "IN_PROGRESS";
    private static final Logger log = LoggerFactory.getLogger(DailyReportService.class);

    private final DailyReportMapper dailyReportMapper;
    private final WorkUnitMapper workUnitMapper;
    private final AccountMapper accountMapper;

    public DailyReportService(DailyReportMapper dailyReportMapper, WorkUnitMapper workUnitMapper, AccountMapper accountMapper) {
        this.dailyReportMapper = dailyReportMapper;
        this.workUnitMapper = workUnitMapper;
        this.accountMapper = accountMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<DailyReportResponse> getDailyReportList(String loginId, DailyReportListRequest request) {
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
                .map(this::toResponse)
                .toList();
        long totalCount = dailyReportMapper.countDailyReportList(searchVo);
        int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / size);

        return new PageResponse<>(content, page, size, totalCount, totalPages);
    }

    @Transactional(readOnly = true)
    public DailyReportResponse getDailyReport(String loginId, String uuid) {
        AcctVo account = getAccountByLoginId(loginId);
        return toResponse(enrichDailyReport(getDailyReport(uuid, account.getIdx())));
    }

    public DailyReportResponse createDailyReport(String loginId, CreateDailyReportRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        LocalDate reportDate = requireReportDate(request.reportDate());
        validateDuplicateReportDate(account.getIdx(), reportDate, null);
        List<WorkUnitVo> workUnits = resolveSelectedWorkUnits(account.getIdx(), request.workUnitUuids());
        log.info(
                "createDailyReport request: loginId={}, memberAccountIdx={}, reportDate={}, requestedWorkUnitUuids={}, resolvedWorkUnitCount={}",
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
                .sts(DEFAULT_DAILY_REPORT_STATUS)
                .spclNote(normalizeText(request.note()))
                .createdBy(loginId)
                .build();
        dailyReportMapper.insertDailyReport(dailyReport);
        syncDailyReportWorkUnits(dailyReport.getIdx(), account.getIdx(), workUnits, loginId);

        return toResponse(enrichDailyReport(getDailyReportByIdx(dailyReport.getIdx())));
    }

    public DailyReportResponse updateDailyReport(String loginId, String uuid, UpdateDailyReportRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        DailyReportVo dailyReport = getDailyReport(uuid, account.getIdx());
        List<WorkUnitVo> workUnits = resolveSelectedWorkUnits(account.getIdx(), request.workUnitUuids());
        log.info(
                "updateDailyReport request: loginId={}, dailyReportUuid={}, dailyReportIdx={}, requestedWorkUnitUuids={}, resolvedWorkUnitCount={}",
                loginId,
                uuid,
                dailyReport.getIdx(),
                request.workUnitUuids(),
                workUnits.size()
        );
        dailyReport.setCntn(resolveReportContent(request.content(), request.note()));
        dailyReport.setSts(DEFAULT_DAILY_REPORT_STATUS);
        dailyReport.setSpclNote(normalizeText(request.note()));
        dailyReport.setUpdatedBy(loginId);
        dailyReportMapper.updateDailyReport(dailyReport);
        syncDailyReportWorkUnits(dailyReport.getIdx(), account.getIdx(), workUnits, loginId);

        return toResponse(enrichDailyReport(getDailyReport(uuid, account.getIdx())));
    }

    @Transactional(readOnly = true)
    public List<DailyReportMissingResponse> getMissingDailyReports(String loginId, DailyReportMissingRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        LocalDate today = LocalDate.now();
        LocalDate dateFrom = request.dateFrom() == null ? today.withDayOfMonth(1) : request.dateFrom();
        LocalDate dateTo = request.dateTo() == null ? today : request.dateTo();
        validateDateRange(dateFrom, dateTo);

        List<LocalDate> missingDates = resolveMissingDates(account.getIdx(), dateFrom, dateTo);
        return missingDates.stream()
                .map(DailyReportMissingResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminDailyReportResponse> getAdminDailyReports(DailyReportListRequest request) {
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
                .map(this::toAdminResponse)
                .toList();
        long totalCount = dailyReportMapper.countAdminDailyReportList(searchVo);
        int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / size);

        return new PageResponse<>(content, page, size, totalCount, totalPages);
    }

    @Transactional(readOnly = true)
    public List<AdminDailyReportMissingResponse> getAdminMissingDailyReports(AdminDailyReportMissingRequest request) {
        LocalDate today = LocalDate.now();
        LocalDate dateFrom = request.dateFrom() == null ? today.withDayOfMonth(1) : request.dateFrom();
        LocalDate dateTo = request.dateTo() == null ? today : request.dateTo();
        validateDateRange(dateFrom, dateTo);

        return dailyReportMapper.selectAdminMissingMembers(normalizeText(request.memberUuid())).stream()
                .map(member -> {
                    List<LocalDate> missingDates = resolveMissingDates(member.getMbrAcctIdx(), dateFrom, dateTo);
                    List<LocalDate> writtenDates = dailyReportMapper.selectWrittenDates(member.getMbrAcctIdx(), dateFrom, dateTo);
                    LocalDate lastWrittenDate = writtenDates.isEmpty() ? null : writtenDates.get(writtenDates.size() - 1);
                    return new AdminDailyReportMissingResponse(
                            member.getMbrAcctUuid(),
                            member.getLgnId(),
                            member.getNickNm(),
                            missingDates,
                            missingDates.size(),
                            lastWrittenDate
                    );
                })
                .toList();
    }

    private List<LocalDate> resolveMissingDates(Long mbrAcctIdx, LocalDate dateFrom, LocalDate dateTo) {
        List<LocalDate> writtenDates = dailyReportMapper.selectWrittenDates(mbrAcctIdx, dateFrom, dateTo);
        List<LocalDate> missingDates = new ArrayList<>();
        LocalDate currentDate = dateFrom;

        while (!currentDate.isAfter(dateTo)) {
            if (!isWeekend(currentDate) && !writtenDates.contains(currentDate)) {
                missingDates.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        return missingDates;
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private DailyReportVo getDailyReport(String uuid, Long mbrAcctIdx) {
        DailyReportVo dailyReport = dailyReportMapper.selectDailyReport(uuid, mbrAcctIdx);

        if (dailyReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "일일보고를 찾을 수 없습니다.");
        }

        return dailyReport;
    }

    private DailyReportVo getDailyReportByIdx(Long idx) {
        DailyReportVo dailyReport = dailyReportMapper.selectDailyReportByIdx(idx);

        if (dailyReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "일일보고를 찾을 수 없습니다.");
        }

        return dailyReport;
    }

    private AcctVo getAccountByLoginId(String loginId) {
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        return account;
    }

    private void validateDuplicateReportDate(Long mbrAcctIdx, LocalDate rptDt, String excludeUuid) {
        if (dailyReportMapper.existsDailyReportByDate(mbrAcctIdx, rptDt, excludeUuid)) {
            throw new BusinessException(ErrorCode.DUPLICATE, "같은 날짜의 일일보고가 이미 존재합니다.");
        }
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom.isAfter(dateTo)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    private LocalDate requireReportDate(LocalDate reportDate) {
        if (reportDate == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "reportDate는 필수입니다.");
        }

        return reportDate;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveReportContent(String content, String note) {
        String normalizedContent = normalizeText(content);

        if (normalizedContent != null) {
            return normalizedContent;
        }

        return normalizeText(note);
    }

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

    private void syncDailyReportWorkUnits(Long dailyReportIdx, Long mbrAcctIdx, List<WorkUnitVo> workUnits, String loginId) {
        log.info(
                "syncDailyReportWorkUnits start: dailyReportIdx={}, memberAccountIdx={}, workUnitIdxs={}",
                dailyReportIdx,
                mbrAcctIdx,
                workUnits.stream().map(WorkUnitVo::getIdx).toList()
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
    }

    private <T extends DailyReportVo> T enrichDailyReport(T dailyReport) {
        dailyReport.setWorkUnits(dailyReportMapper.selectDailyReportWorkUnits(dailyReport.getIdx()));
        return dailyReport;
    }

    private List<DailyReportWorkUnitResponse> toWorkUnitResponses(List<WorkUnitVo> workUnits) {
        if (workUnits == null || workUnits.isEmpty()) {
            return List.of();
        }

        return workUnits.stream()
                .map(workUnit -> new DailyReportWorkUnitResponse(
                        workUnit.getUuid(),
                        workUnit.getTtl(),
                        workUnit.getCtgr()
                ))
                .toList();
    }

    private DailyReportResponse toResponse(DailyReportVo dailyReport) {
        return new DailyReportResponse(
                dailyReport.getUuid(),
                dailyReport.getRptDt(),
                toWorkUnitResponses(dailyReport.getWorkUnits()),
                dailyReport.getCntn(),
                dailyReport.getSpclNote() != null ? dailyReport.getSpclNote() : dailyReport.getCntn(),
                dailyReport.getCreatedAt(),
                dailyReport.getUpdatedAt()
        );
    }

    private AdminDailyReportResponse toAdminResponse(DailyReportAdmJvo dailyReport) {
        return new AdminDailyReportResponse(
                dailyReport.getUuid(),
                dailyReport.getMbrAcctUuid(),
                dailyReport.getLgnId(),
                dailyReport.getNickNm(),
                dailyReport.getRptDt(),
                toWorkUnitResponses(dailyReport.getWorkUnits()),
                dailyReport.getCntn(),
                dailyReport.getSpclNote() != null ? dailyReport.getSpclNote() : dailyReport.getCntn(),
                dailyReport.getCreatedAt(),
                dailyReport.getUpdatedAt()
        );
    }
}
