package com.gw.api.convert.life;

import com.gw.api.dto.life.LifeDailyDtos.*;
import com.gw.share.vo.life.LifeDailyVo;
import com.gw.share.jvo.life.LifeDailyCheckJvo;
import java.time.LocalDate;
import java.util.UUID;

public final class LifeDailyConvert {
    private LifeDailyConvert() { }

    // 내부 키를 제외한 매일 항목 응답을 만든다.
    public static Item toItem(LifeDailyVo item) {
        return new Item(item.getUuid(), item.getTtl(), item.getStartDt(), item.getEndDt());
    }

    // 날짜별 완료 기록을 외부 식별자로 반환한다.
    public static Check toCheck(LifeDailyCheckJvo check) { return new Check(check.getItemUuid(), check.getCheckDt()); }

    // 인증 사용자와 시작 날짜로 매일 항목을 생성한다.
    public static LifeDailyVo toVo(SaveItem request, Long owner, LocalDate today, String loginId) {
        return LifeDailyVo.builder().uuid(UUID.randomUUID().toString()).mbrAcctIdx(owner).ttl(request.title().trim())
                .startDt(today).createdBy(loginId).updatedBy(loginId).build();
    }
}
