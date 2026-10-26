package com.gw.api.service.life;

import com.gw.api.convert.life.LifeDailyConvert;
import com.gw.api.dto.life.LifeDailyDtos.*;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.life.LifeDailyMapper;
import com.gw.share.common.exception.*;
import com.gw.share.common.policy.LifeTodoPolicy;
import com.gw.share.util.StringUtil;
import com.gw.share.vo.life.LifeDailyVo;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.DateTimeException;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @RequiredArgsConstructor @Slf4j
@Transactional(readOnly = true)
public class LifeDailyService {
    private final LifeDailyMapper mapper;
    private final AccountLookupService accounts;

    // 월별 항목과 완료 기록을 같은 스냅샷에서 조회한다.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Month getMonth(String loginId, String month, String timeZone) {
        return logged("getLifeDailyMonth", loginId, () -> {
            LocalDate today = LifeTodoPolicy.today(timeZone);
            YearMonth selected;
            try {
                if (month == null || !month.matches("\\d{4}-\\d{2}")) throw new DateTimeException("month");
                selected = YearMonth.parse(month);
                if (selected.getYear() < 1 || selected.getYear() > 9999) throw new DateTimeException("year");
            } catch (DateTimeException exception) { throw new BusinessException(ErrorCode.BAD_REQUEST); }
            Long owner = owner(loginId);
            return new Month(selected.toString(), today, timeZone,
                    mapper.selectItems(owner, selected.atDay(1), selected.atEndOfMonth()).stream().map(LifeDailyConvert::toItem).toList(),
                    mapper.selectChecks(owner, selected.atDay(1), selected.atEndOfMonth()).stream().map(LifeDailyConvert::toCheck).toList());
        });
    }

    // 오늘부터 매일 체크할 항목을 등록한다.
    @Transactional
    public Item createItem(String loginId, SaveItem request, String timeZone) {
        return logged("createLifeDaily", loginId, () -> {
            validateTitle(request);
            LocalDate today = LifeTodoPolicy.today(timeZone);
            var item = LifeDailyConvert.toVo(request, lockOwner(loginId), today, loginId);
            mapper.insertItem(item);
            return LifeDailyConvert.toItem(item);
        });
    }

    // 본인의 활성 반복 항목 제목을 수정한다.
    @Transactional
    public Item updateItem(String loginId, String uuid, SaveItem request) {
        return logged("updateLifeDaily", loginId, () -> {
            validateTitle(request);
            String key = requiredUuid(uuid);
            Long owner = lockOwner(loginId);
            var item = owned(owner, key);
            if (item.getEndDt() != null) throw new BusinessException(ErrorCode.BAD_REQUEST, "종료한 항목은 수정할 수 없습니다.");
            requireChanged(mapper.updateTitle(owner, key, request.title().trim(), loginId));
            return LifeDailyConvert.toItem(owned(owner, key));
        });
    }

    // 과거 기록과 오늘 체크를 유지하며 내일부터 반복을 종료한다.
    @Transactional
    public void endItem(String loginId, String uuid, String timeZone) {
        logged("endLifeDaily", loginId, () -> {
            String key = requiredUuid(uuid);
            LocalDate today = LifeTodoPolicy.today(timeZone);
            Long owner = lockOwner(loginId);
            var item = owned(owner, key);
            if (today.isBefore(item.getStartDt())) throw new BusinessException(ErrorCode.BAD_REQUEST);
            if (item.getEndDt() == null) requireChanged(mapper.endItem(owner, key, today, loginId));
            return null;
        });
    }

    // 미래와 반복 기간 밖의 날짜를 거부하고 날짜별 완료 여부를 저장한다.
    @Transactional
    public void saveCheck(String loginId, String uuid, LocalDate date, Completion request, String timeZone) {
        logged("saveLifeDailyCheck", loginId, () -> {
            String key = requiredUuid(uuid);
            LocalDate today = LifeTodoPolicy.today(timeZone);
            if (date == null || date.isAfter(today) || request == null || request.completed() == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST);
            }
            Long owner = lockOwner(loginId);
            var item = owned(owner, key);
            if (date.isBefore(item.getStartDt()) || (item.getEndDt() != null && date.isAfter(item.getEndDt()))) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "반복 기간에 해당하는 날짜만 체크할 수 있습니다.");
            }
            requireChanged(mapper.saveCheck(owner, key, date, request.completed(), loginId));
            return null;
        });
    }

    private Long owner(String loginId) { return accounts.getAccountByLoginId(loginId).getIdx(); }

    private Long lockOwner(String loginId) {
        Long owner = owner(loginId);
        if (mapper.lockOwner(owner) == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return owner;
    }

    private LifeDailyVo owned(Long owner, String uuid) {
        var item = mapper.selectItem(owner, uuid);
        if (item == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return item;
    }

    private void validateTitle(SaveItem request) {
        if (request == null || StringUtil.normalizeBlank(request.title()) == null || request.title().length() > 200) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "제목을 1~200자로 입력해주세요.");
        }
    }

    private String requiredUuid(String value) {
        try {
            String canonical = UUID.fromString(value).toString();
            if (!canonical.equalsIgnoreCase(value)) throw new IllegalArgumentException();
            return canonical;
        } catch (IllegalArgumentException | NullPointerException exception) { throw new BusinessException(ErrorCode.BAD_REQUEST); }
    }

    private void requireChanged(int count) { if (count != 1) throw new BusinessException(ErrorCode.NOT_FOUND); }

    // 제목을 로그에 남기지 않고 공개 흐름의 시작·완료·실패를 기록한다.
    private <T> T logged(String action, String loginId, Supplier<T> operation) {
        log.info("{} 시작 - loginId: {}", action, loginId);
        try {
            T result = operation.get();
            log.info("{} 완료 - loginId: {}", action, loginId);
            return result;
        } catch (RuntimeException exception) {
            log.error("{} 실패 - loginId: {}", action, loginId, exception);
            throw exception;
        }
    }
}
