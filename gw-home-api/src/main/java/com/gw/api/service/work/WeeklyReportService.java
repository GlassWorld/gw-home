package com.gw.api.service.work;

import com.gw.api.dto.work.CreateWeeklyReportRequest;
import com.gw.api.dto.work.DailyReportWorkUnitResponse;
import com.gw.api.dto.work.UpdateWeeklyReportRequest;
import com.gw.api.dto.work.WeeklyReportAiDraftRequest;
import com.gw.api.dto.work.WeeklyReportAiDraftResponse;
import com.gw.api.dto.work.WeeklyReportDailySourceResponse;
import com.gw.api.dto.work.WeeklyReportResponse;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.work.DailyReportMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.DailyReportVo;
import com.gw.share.vo.work.WorkUnitVo;
import com.gw.share.vo.work.WeeklyReportVo;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WeeklyReportService {

    private final DailyReportMapper dailyReportMapper;
    private final AccountMapper accountMapper;
    private final WeeklyReportDraftService weeklyReportDraftService;

    public WeeklyReportService(
            DailyReportMapper dailyReportMapper,
            AccountMapper accountMapper,
            WeeklyReportDraftService weeklyReportDraftService
    ) {
        this.dailyReportMapper = dailyReportMapper;
        this.accountMapper = accountMapper;
        this.weeklyReportDraftService = weeklyReportDraftService;
    }

    @Transactional(readOnly = true)
    public List<WeeklyReportResponse> getWeeklyReportList(String loginId) {
        AcctVo account = getAccountByLoginId(loginId);
        return dailyReportMapper.selectWeeklyReportList(account.getIdx()).stream()
                .map(this::toWeeklyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WeeklyReportResponse getWeeklyReport(String loginId, String uuid) {
        AcctVo account = getAccountByLoginId(loginId);
        return toWeeklyResponse(getWeeklyReport(uuid, account.getIdx()));
    }

    public WeeklyReportResponse createWeeklyReport(String loginId, CreateWeeklyReportRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        LocalDate weekStartDate = requireDate(request.weekStartDate(), "weekStartDate");
        LocalDate weekEndDate = requireDate(request.weekEndDate(), "weekEndDate");
        validateWeekRange(weekStartDate, weekEndDate);

        WeeklyReportVo weeklyReport = WeeklyReportVo.builder()
                .mbrAcctIdx(account.getIdx())
                .wkStrtDt(weekStartDate)
                .wkEndDt(weekEndDate)
                .ttl(normalizeRequiredText(request.title(), "title"))
                .cntn(normalizeRequiredText(request.content(), "content"))
                .opnYn(normalizeOpenYn(request.openYn()))
                .pblsAt(resolvePublishedAt(request.openYn(), null))
                .genType(normalizeGenerationType(request.generationType()))
                .createdBy(loginId)
                .build();
        dailyReportMapper.insertWeeklyReport(weeklyReport);

        return toWeeklyResponse(getWeeklyReportByIdx(weeklyReport.getIdx()));
    }

    public WeeklyReportResponse updateWeeklyReport(String loginId, String uuid, UpdateWeeklyReportRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        WeeklyReportVo weeklyReport = getWeeklyReport(uuid, account.getIdx());
        LocalDate weekStartDate = requireDate(request.weekStartDate(), "weekStartDate");
        LocalDate weekEndDate = requireDate(request.weekEndDate(), "weekEndDate");
        validateWeekRange(weekStartDate, weekEndDate);

        weeklyReport.setWkStrtDt(weekStartDate);
        weeklyReport.setWkEndDt(weekEndDate);
        weeklyReport.setTtl(normalizeRequiredText(request.title(), "title"));
        weeklyReport.setCntn(normalizeRequiredText(request.content(), "content"));
        weeklyReport.setOpnYn(normalizeOpenYn(request.openYn()));
        weeklyReport.setPblsAt(resolvePublishedAt(request.openYn(), weeklyReport.getPblsAt()));
        weeklyReport.setGenType(normalizeGenerationType(request.generationType()));
        weeklyReport.setUpdatedBy(loginId);
        dailyReportMapper.updateWeeklyReport(weeklyReport);

        return toWeeklyResponse(getWeeklyReport(uuid, account.getIdx()));
    }

    @Transactional(readOnly = true)
    public List<WeeklyReportDailySourceResponse> getWeeklySourceDailyReports(
            String loginId,
            LocalDate weekStartDate,
            LocalDate weekEndDate
    ) {
        AcctVo account = getAccountByLoginId(loginId);
        LocalDate normalizedWeekStartDate = requireDate(weekStartDate, "weekStartDate");
        LocalDate normalizedWeekEndDate = requireDate(weekEndDate, "weekEndDate");
        validateWeekRange(normalizedWeekStartDate, normalizedWeekEndDate);

        return dailyReportMapper.selectWeeklySourceDailyReports(account.getIdx(), normalizedWeekStartDate, normalizedWeekEndDate).stream()
                .map(this::enrichDailyReport)
                .map(this::toDailySourceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WeeklyReportAiDraftResponse generateWeeklyReportDraft(String loginId, WeeklyReportAiDraftRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        LocalDate weekStartDate = requireDate(request.weekStartDate(), "weekStartDate");
        LocalDate weekEndDate = requireDate(request.weekEndDate(), "weekEndDate");
        validateWeekRange(weekStartDate, weekEndDate);
        List<DailyReportVo> sourceReports = dailyReportMapper.selectWeeklySourceDailyReports(account.getIdx(), weekStartDate, weekEndDate).stream()
                .map(this::enrichDailyReport)
                .toList();

        return weeklyReportDraftService.generateDraft(
                weekStartDate,
                weekEndDate,
                sourceReports,
                request.additionalPrompt()
        );
    }

    private WeeklyReportVo getWeeklyReport(String uuid, Long mbrAcctIdx) {
        WeeklyReportVo weeklyReport = dailyReportMapper.selectWeeklyReport(uuid, mbrAcctIdx);

        if (weeklyReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "주간보고를 찾을 수 없습니다.");
        }

        return weeklyReport;
    }

    private WeeklyReportVo getWeeklyReportByIdx(Long idx) {
        WeeklyReportVo weeklyReport = dailyReportMapper.selectWeeklyReportByIdx(idx);

        if (weeklyReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "주간보고를 찾을 수 없습니다.");
        }

        return weeklyReport;
    }

    private AcctVo getAccountByLoginId(String loginId) {
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        return account;
    }

    private LocalDate requireDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + "는 필수입니다.");
        }

        return value;
    }

    private void validateWeekRange(LocalDate weekStartDate, LocalDate weekEndDate) {
        if (weekStartDate.isAfter(weekEndDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "weekStartDate는 weekEndDate보다 늦을 수 없습니다.");
        }

        if (weekStartDate.plusDays(6).isBefore(weekEndDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "주간보고 기간은 7일 이내여야 합니다.");
        }
    }

    private String normalizeRequiredText(String value, String fieldName) {
        String normalized = normalizeText(value);

        if (normalized == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + "는 필수입니다.");
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

    private String normalizeOpenYn(String openYn) {
        String normalized = normalizeText(openYn);

        if (normalized == null) {
            return "N";
        }

        return switch (normalized) {
            case "Y", "N" -> normalized;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "openYn은 Y 또는 N 이어야 합니다.");
        };
    }

    private String normalizeGenerationType(String generationType) {
        String normalized = normalizeText(generationType);

        if (normalized == null) {
            return "MANUAL";
        }

        return switch (normalized) {
            case "MANUAL", "OPENAI", "RULE_BASED" -> normalized;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "generationType이 올바르지 않습니다.");
        };
    }

    private OffsetDateTime resolvePublishedAt(String openYn, OffsetDateTime currentPublishedAt) {
        return "Y".equals(normalizeOpenYn(openYn)) ? (currentPublishedAt == null ? OffsetDateTime.now() : currentPublishedAt) : null;
    }

    private WeeklyReportResponse toWeeklyResponse(WeeklyReportVo weeklyReport) {
        return new WeeklyReportResponse(
                weeklyReport.getUuid(),
                weeklyReport.getWkStrtDt(),
                weeklyReport.getWkEndDt(),
                weeklyReport.getTtl(),
                weeklyReport.getCntn(),
                weeklyReport.getOpnYn(),
                weeklyReport.getPblsAt(),
                weeklyReport.getGenType(),
                weeklyReport.getCreatedAt(),
                weeklyReport.getUpdatedAt()
        );
    }

    private WeeklyReportDailySourceResponse toDailySourceResponse(DailyReportVo dailyReport) {
        return new WeeklyReportDailySourceResponse(
                dailyReport.getUuid(),
                dailyReport.getRptDt(),
                toWorkUnitResponses(dailyReport.getWorkUnits()),
                dailyReport.getSpclNote()
        );
    }

    private DailyReportVo enrichDailyReport(DailyReportVo dailyReport) {
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
}
