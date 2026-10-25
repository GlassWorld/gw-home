package com.gw.api.service.work;

import com.gw.api.convert.work.WorkInsightsConvert;
import com.gw.api.dto.work.WorkInsightsRequest;
import com.gw.api.dto.work.WorkInsightsResponse.Gantt;
import com.gw.api.dto.work.WorkInsightsResponse.Period;
import com.gw.api.dto.work.WorkInsightsResponse.Statistics;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.work.WorkInsightsMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.WorkTodoPolicy;
import com.gw.share.util.StringUtil;
import com.gw.share.vo.work.WorkTodoListSearchVo;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Slf4j
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class WorkInsightsService {
    private final WorkInsightsMapper mapper;
    private final AccountLookupService accounts;
    private final Clock clock;

    // 인증 사용자의 선택 기간 일정과 계층 표시용 조상을 같은 스냅샷에서 조회한다.
    public Gantt getGantt(String loginId, WorkInsightsRequest request) {
        log.info("getGantt 시작 - loginId: {}", loginId);
        try {
            Search search = prepare(loginId, request, false);
            var rows = mapper.selectGanttRows(search.query(), search.period().fromDate(), search.period().toDate());
            if (rows.size() > 2000) throw new BusinessException(ErrorCode.BAD_REQUEST, "표시할 일정이 많습니다. 업무나 기간을 좁혀주세요. (최대 2,000행)");
            var units = mapper.selectUnitInsights(search.query(), search.fromInstant(), search.beforeInstant());
            var response = WorkInsightsConvert.toGantt(search.period(), units, rows);
            log.info("getGantt 완료 - loginId: {}, rows: {}", loginId, rows.size());
            return response;
        } catch (RuntimeException exception) {
            log.error("getGantt 실패 - loginId: {}", loginId, exception);
            throw exception;
        }
    }

    // 인증 사용자의 현재 최하위 현황과 현지 완료 기간별 추이를 구분해 조회한다.
    public Statistics getStatistics(String loginId, WorkInsightsRequest request) {
        log.info("getStatistics 시작 - loginId: {}", loginId);
        try {
            Search search = prepare(loginId, request, true);
            var units = mapper.selectUnitInsights(search.query(), search.fromInstant(), search.beforeInstant());
            var buckets = mapper.selectCompletionBuckets(search.query(), search.fromInstant(), search.beforeInstant(),
                    search.period().timeZone(), request.getBucket());
            var response = WorkInsightsConvert.toStatistics(search.period(), request.getBucket(), units, buckets);
            log.info("getStatistics 완료 - loginId: {}, total: {}", loginId, response.summary().totalCount());
            return response;
        } catch (RuntimeException exception) {
            log.error("getStatistics 실패 - loginId: {}", loginId, exception);
            throw exception;
        }
    }

    private Search prepare(String loginId, WorkInsightsRequest request, boolean statistics) {
        var account = accounts.getAccountByLoginId(loginId);
        String timeZone = request.getTimeZone() == null ? WorkTodoPolicy.DEFAULT_TIME_ZONE : request.getTimeZone();
        if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IANA 시간대를 지정해주세요. 예: Asia/Seoul");
        }
        LocalDate today = WorkTodoPolicy.dateAt(clock.instant(), timeZone);
        ZoneId zone = ZoneId.of(timeZone);
        LocalDate from = request.getFromDate();
        LocalDate to = request.getToDate();
        if (from == null && to == null) {
            from = statistics ? today.minusDays(29) : today;
            to = statistics ? today : today.plusDays(13);
        }
        if (from == null || to == null || from.getYear() < 1900 || to.getYear() > 9998
                || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) >= (statistics ? 366 : 93)
                || request.getBucket() == null || !Set.of("day", "week", "month").contains(request.getBucket())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, statistics
                    ? "완료 기간은 최대 366일이며 집계 단위는 day/week/month입니다."
                    : "일정 조회 기간은 최대 93일입니다. 시작일과 종료일을 함께 지정해주세요.");
        }
        String uuid = StringUtil.normalizeBlank(request.getWorkUnitUuid());
        if (uuid != null) {
            try {
                if (!UUID.fromString(uuid).toString().equalsIgnoreCase(uuid)) throw new IllegalArgumentException();
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "올바른 업무 식별자를 지정해주세요.");
            }
        }
        boolean includeInactive = request.getIncludeInactive() == null ? statistics : request.getIncludeInactive();
        var query = WorkTodoListSearchVo.builder().mbrAcctIdx(account.getIdx()).workUnitUuid(uuid)
                .includeInactive(includeInactive).today(today).group("ALL").build();
        return new Search(query, new Period(from, to, timeZone, today, includeInactive, uuid),
                from.atStartOfDay(zone).toOffsetDateTime(), to.plusDays(1).atStartOfDay(zone).toOffsetDateTime());
    }

    private record Search(WorkTodoListSearchVo query, Period period, OffsetDateTime fromInstant, OffsetDateTime beforeInstant) { }
}
