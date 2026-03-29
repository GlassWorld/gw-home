package com.gw.api.service.work;

import com.gw.api.convert.work.WeeklyReportConvert;
import com.gw.api.dto.work.CreateWeeklyReportRequest;
import com.gw.api.dto.work.UpdateWeeklyReportRequest;
import com.gw.api.dto.work.WeeklyReportAiDraftRequest;
import com.gw.api.dto.work.WeeklyReportAiDraftResponse;
import com.gw.api.dto.work.WeeklyReportDailySourceResponse;
import com.gw.api.dto.work.WeeklyReportResponse;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.work.DailyReportMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.util.DateUtil;
import com.gw.share.util.StringUtil;
import com.gw.share.util.ValidationUtil;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.DailyReportVo;
import com.gw.share.vo.work.WeeklyReportVo;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
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

    // 로그인 사용자의 주간보고 목록을 조회한다.
    @Transactional(readOnly = true)
    public List<WeeklyReportResponse> getWeeklyReportList(String loginId) {
        log.info("getWeeklyReportList 시작 - loginId: {}", loginId);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            List<WeeklyReportResponse> response = dailyReportMapper.selectWeeklyReportList(account.getIdx()).stream()
                    .map(WeeklyReportConvert::toResponse)
                    .toList();
            log.info("getWeeklyReportList 완료 - loginId: {}, count: {}", loginId, response.size());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getWeeklyReportList 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 주간보고 단건을 조회한다.
    @Transactional(readOnly = true)
    public WeeklyReportResponse getWeeklyReport(String loginId, String uuid) {
        log.info("getWeeklyReport 시작 - loginId: {}, uuid: {}", loginId, uuid);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WeeklyReportResponse response = WeeklyReportConvert.toResponse(getWeeklyReport(uuid, account.getIdx()));
            log.info("getWeeklyReport 완료 - loginId: {}, uuid: {}", loginId, uuid);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getWeeklyReport 실패 - loginId: {}, uuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    uuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 주간보고를 생성한다.
    public WeeklyReportResponse createWeeklyReport(String loginId, CreateWeeklyReportRequest request) {
        log.info("createWeeklyReport 시작 - loginId: {}, request: {}", loginId, request);
        try {
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

            WeeklyReportResponse response = WeeklyReportConvert.toResponse(getWeeklyReportByIdx(weeklyReport.getIdx()));
            log.info("createWeeklyReport 완료 - loginId: {}, uuid: {}", loginId, response.uuid());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "createWeeklyReport 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 주간보고를 수정한다.
    public WeeklyReportResponse updateWeeklyReport(String loginId, String uuid, UpdateWeeklyReportRequest request) {
        log.info("updateWeeklyReport 시작 - loginId: {}, uuid: {}, request: {}", loginId, uuid, request);
        try {
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

            WeeklyReportResponse response = WeeklyReportConvert.toResponse(getWeeklyReport(uuid, account.getIdx()));
            log.info("updateWeeklyReport 완료 - loginId: {}, uuid: {}", loginId, uuid);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "updateWeeklyReport 실패 - loginId: {}, uuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    uuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 주간보고 작성에 사용할 일일보고 원본을 조회한다.
    @Transactional(readOnly = true)
    public List<WeeklyReportDailySourceResponse> getWeeklySourceDailyReports(
            String loginId,
            LocalDate weekStartDate,
            LocalDate weekEndDate
    ) {
        log.info(
                "getWeeklySourceDailyReports 시작 - loginId: {}, weekStartDate: {}, weekEndDate: {}",
                loginId,
                weekStartDate,
                weekEndDate
        );
        try {
            AcctVo account = getAccountByLoginId(loginId);
            LocalDate normalizedWeekStartDate = requireDate(weekStartDate, "weekStartDate");
            LocalDate normalizedWeekEndDate = requireDate(weekEndDate, "weekEndDate");
            validateWeekRange(normalizedWeekStartDate, normalizedWeekEndDate);

            List<WeeklyReportDailySourceResponse> response =
                    dailyReportMapper.selectWeeklySourceDailyReports(account.getIdx(), normalizedWeekStartDate, normalizedWeekEndDate).stream()
                            .map(this::enrichDailyReport)
                            .map(WeeklyReportConvert::toDailySourceResponse)
                            .toList();
            log.info("getWeeklySourceDailyReports 완료 - loginId: {}, count: {}", loginId, response.size());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getWeeklySourceDailyReports 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 주간보고 AI 초안을 생성한다.
    @Transactional(readOnly = true)
    public WeeklyReportAiDraftResponse generateWeeklyReportDraft(String loginId, WeeklyReportAiDraftRequest request) {
        log.info("generateWeeklyReportDraft 시작 - loginId: {}, request: {}", loginId, request);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            LocalDate weekStartDate = requireDate(request.weekStartDate(), "weekStartDate");
            LocalDate weekEndDate = requireDate(request.weekEndDate(), "weekEndDate");
            validateWeekRange(weekStartDate, weekEndDate);
            List<DailyReportVo> sourceReports = dailyReportMapper.selectWeeklySourceDailyReports(account.getIdx(), weekStartDate, weekEndDate).stream()
                    .map(this::enrichDailyReport)
                    .toList();

            WeeklyReportAiDraftResponse response = weeklyReportDraftService.generateDraft(
                    weekStartDate,
                    weekEndDate,
                    sourceReports,
                    request.additionalPrompt()
            );
            log.info("generateWeeklyReportDraft 완료 - loginId: {}, sourceCount: {}", loginId, sourceReports.size());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "generateWeeklyReportDraft 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // UUID와 소유 계정으로 주간보고를 조회한다.
    private WeeklyReportVo getWeeklyReport(String uuid, Long mbrAcctIdx) {
        WeeklyReportVo weeklyReport = dailyReportMapper.selectWeeklyReport(uuid, mbrAcctIdx);

        if (weeklyReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "주간보고를 찾을 수 없습니다.");
        }

        return weeklyReport;
    }

    // PK로 주간보고를 조회한다.
    private WeeklyReportVo getWeeklyReportByIdx(Long idx) {
        WeeklyReportVo weeklyReport = dailyReportMapper.selectWeeklyReportByIdx(idx);

        if (weeklyReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "주간보고를 찾을 수 없습니다.");
        }

        return weeklyReport;
    }

    // 로그인 ID로 회원 계정을 조회한다.
    private AcctVo getAccountByLoginId(String loginId) {
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        return account;
    }

    // 주간보고 날짜 입력 필수 여부를 검증한다.
    private LocalDate requireDate(LocalDate value, String fieldName) {
        return ValidationUtil.requireNonNull(value, ErrorCode.BAD_REQUEST, fieldName + "는 필수입니다.");
    }

    // 주간보고 기간 범위를 검증한다.
    private void validateWeekRange(LocalDate weekStartDate, LocalDate weekEndDate) {
        DateUtil.validateDateOrder(weekStartDate, weekEndDate, ErrorCode.BAD_REQUEST, "weekStartDate는 weekEndDate보다 늦을 수 없습니다.");
        ValidationUtil.requireTrue(
                !weekStartDate.plusDays(6).isBefore(weekEndDate),
                ErrorCode.BAD_REQUEST,
                "주간보고 기간은 7일 이내여야 합니다."
        );
    }

    // 필수 문자열 입력값을 정규화하고 비어 있으면 예외 처리한다.
    private String normalizeRequiredText(String value, String fieldName) {
        String normalized = normalizeText(value);
        ValidationUtil.requireNonNull(normalized, ErrorCode.BAD_REQUEST, fieldName + "는 필수입니다.");
        return normalized;
    }

    // 문자열 입력값의 공백을 정리한다.
    private String normalizeText(String value) {
        return StringUtil.normalizeBlank(value);
    }

    // 공개 여부 입력값을 정규화한다.
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

    // 생성 타입 입력값을 정규화한다.
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

    // 공개 여부에 따라 발행 시각을 계산한다.
    private OffsetDateTime resolvePublishedAt(String openYn, OffsetDateTime currentPublishedAt) {
        return "Y".equals(normalizeOpenYn(openYn)) ? (currentPublishedAt == null ? OffsetDateTime.now() : currentPublishedAt) : null;
    }

    // 일일보고에 연결된 업무 목록을 채운다.
    private DailyReportVo enrichDailyReport(DailyReportVo dailyReport) {
        dailyReport.setWorkUnits(dailyReportMapper.selectDailyReportWorkUnits(dailyReport.getIdx()));
        return dailyReport;
    }
}
