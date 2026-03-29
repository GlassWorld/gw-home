package com.gw.api.service.work;

import com.gw.api.dto.work.AdminDailyReportMissingRequest;
import com.gw.api.dto.work.AdminDailyReportMissingResponse;
import com.gw.api.dto.work.AdminDailyReportResponse;
import com.gw.api.dto.work.CreateDailyReportRequest;
import com.gw.api.dto.work.DailyReportListRequest;
import com.gw.api.dto.work.DailyReportMissingRequest;
import com.gw.api.dto.work.DailyReportMissingResponse;
import com.gw.api.dto.work.DailyReportResponse;
import com.gw.api.dto.work.UpdateDailyReportRequest;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.work.DailyReportMapper;
import com.gw.infra.db.support.PageSortSupport;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.PageResponse;
import com.gw.share.jvo.work.DailyReportAdmJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.DailyReportListSearchVo;
import com.gw.share.vo.work.DailyReportVo;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DailyReportService {

    private final DailyReportMapper dailyReportMapper;
    private final AccountMapper accountMapper;

    public DailyReportService(DailyReportMapper dailyReportMapper, AccountMapper accountMapper) {
        this.dailyReportMapper = dailyReportMapper;
        this.accountMapper = accountMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<DailyReportResponse> getDailyReportList(String loginId, DailyReportListRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        LocalDate today = LocalDate.now();
        LocalDate dateFrom = request.dateFrom() == null ? today.withDayOfMonth(1) : request.dateFrom();
        LocalDate dateTo = request.dateTo() == null ? today : request.dateTo();
        validateDateRange(dateFrom, dateTo);

        int page = PageSortSupport.normalizePage(request.page());
        int size = PageSortSupport.normalizeSize(request.size());
        DailyReportListSearchVo searchVo = DailyReportListSearchVo.builder()
                .mbrAcctIdx(account.getIdx())
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .sts(normalizeStatus(request.status(), true))
                .kwd(normalizeText(request.keyword()))
                .page(page)
                .size(size)
                .offset((page - 1) * size)
                .build();

        List<DailyReportResponse> content = dailyReportMapper.selectDailyReportList(searchVo).stream()
                .map(this::toResponse)
                .toList();
        long totalCount = dailyReportMapper.countDailyReportList(searchVo);
        int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / size);

        return new PageResponse<>(content, page, size, totalCount, totalPages);
    }

    @Transactional(readOnly = true)
    public DailyReportResponse getDailyReport(String loginId, String uuid) {
        AcctVo account = getAccountByLoginId(loginId);
        return toResponse(getDailyReport(uuid, account.getIdx()));
    }

    public DailyReportResponse createDailyReport(String loginId, CreateDailyReportRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        LocalDate reportDate = requireReportDate(request.reportDate());
        validateDuplicateReportDate(account.getIdx(), reportDate, null);

        DailyReportVo dailyReport = DailyReportVo.builder()
                .mbrAcctIdx(account.getIdx())
                .rptDt(reportDate)
                .cntn(normalizeRequiredContent(request.content()))
                .sts(normalizeStatus(request.status(), false))
                .spclNote(normalizeText(request.note()))
                .createdBy(loginId)
                .build();
        dailyReportMapper.insertDailyReport(dailyReport);

        return toResponse(getDailyReportByIdx(dailyReport.getIdx()));
    }

    public DailyReportResponse updateDailyReport(String loginId, String uuid, UpdateDailyReportRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        DailyReportVo dailyReport = getDailyReport(uuid, account.getIdx());
        dailyReport.setCntn(normalizeRequiredContent(request.content()));
        dailyReport.setSts(normalizeStatus(request.status(), false));
        dailyReport.setSpclNote(normalizeText(request.note()));
        dailyReport.setUpdatedBy(loginId);
        dailyReportMapper.updateDailyReport(dailyReport);

        return toResponse(getDailyReport(uuid, account.getIdx()));
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
                .sts(normalizeStatus(request.status(), true))
                .kwd(normalizeText(request.keyword()))
                .page(page)
                .size(size)
                .offset((page - 1) * size)
                .build();

        List<AdminDailyReportResponse> content = dailyReportMapper.selectAdminDailyReportList(searchVo).stream()
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

    private String normalizeRequiredContent(String content) {
        String normalized = normalizeText(content);

        if (normalized == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "content는 필수입니다.");
        }

        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatus(String status, boolean allowNull) {
        String normalized = normalizeText(status);

        if (normalized == null) {
            if (allowNull) {
                return null;
            }
            return "IN_PROGRESS";
        }

        return switch (normalized) {
            case "PLANNED", "IN_PROGRESS", "DONE" -> normalized;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "올바르지 않은 진행상태입니다.");
        };
    }

    private DailyReportResponse toResponse(DailyReportVo dailyReport) {
        return new DailyReportResponse(
                dailyReport.getUuid(),
                dailyReport.getRptDt(),
                dailyReport.getCntn(),
                dailyReport.getSts(),
                dailyReport.getSpclNote(),
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
                dailyReport.getCntn(),
                dailyReport.getSts(),
                dailyReport.getSpclNote(),
                dailyReport.getCreatedAt(),
                dailyReport.getUpdatedAt()
        );
    }
}
