package com.gw.api.convert.life;

import com.gw.api.dto.life.LifeTodoDtos.*;
import com.gw.share.jvo.life.*;
import com.gw.share.vo.life.*;
import com.gw.share.util.StringUtil;
import java.time.LocalDate;
import java.util.UUID;

public final class LifeTodoConvert {
    private LifeTodoConvert() { }

    // 일상 조회 모델에서 외부 UUID와 화면 필드만 반환한다.
    public static Todo toResponse(LifeTodoJvo todo) {
        return new Todo(todo.getUuid(), todo.getTtl(), todo.getMemo(), todo.getCategoryUuid(), todo.getCategoryNm(),
                todo.getDueDt(), todo.getCmplAt() != null, todo.getCmplAt(), todo.getTodoGroup(), todo.getCreatedAt(), todo.getUpdatedAt());
    }

    // 생활 카테고리의 내부 식별자를 응답에서 제외한다.
    public static Category toCategory(LifeTodoCategoryVo category) {
        return new Category(category.getUuid(), category.getNm());
    }

    // 제목·메모를 정규화하고 인증된 소유자와 카테고리로 저장 모델을 구성한다.
    public static LifeTodoVo toVo(SaveTodo request, Long owner, Long category, String uuid, String loginId) {
        return LifeTodoVo.builder().uuid(uuid).mbrAcctIdx(owner).lfTodoCatIdx(category)
                .ttl(request.title().trim()).memo(StringUtil.normalizeBlank(request.memo())).dueDt(request.dueDate())
                .createdBy(loginId).updatedBy(loginId).build();
    }

    // 사용자별 생활 카테고리의 저장 모델을 만든다.
    public static LifeTodoCategoryVo toCategoryVo(String name, Long owner, String uuid, String loginId) {
        return LifeTodoCategoryVo.builder().uuid(uuid == null ? UUID.randomUUID().toString() : uuid)
                .mbrAcctIdx(owner).nm(name.trim()).createdBy(loginId).updatedBy(loginId).build();
    }

    // 일상 데이터만 사용하여 현재 완료 비율과 날짜별 분류 건수를 반환한다.
    public static Summary toSummary(LifeTodoSummaryJvo summary, LocalDate today) {
        int progress = summary.getTotalCount() == 0 ? 0 : (int) Math.round(100.0 * summary.getCompletedCount() / summary.getTotalCount());
        return new Summary(summary.getTotalCount(), summary.getTodayCount(), summary.getUpcomingCount(), summary.getOverdueCount(),
                summary.getCompletedCount(), summary.getUnscheduledCount(), progress, today);
    }
}
