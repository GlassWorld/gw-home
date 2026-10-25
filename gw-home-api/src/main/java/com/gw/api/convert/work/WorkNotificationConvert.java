package com.gw.api.convert.work;

import com.gw.api.dto.work.WorkNotificationDtos.*;
import com.gw.share.vo.work.*;

public final class WorkNotificationConvert {
    private WorkNotificationConvert() {}
    // 수신 설정의 외부 응답을 만든다.
    public static SettingsResponse toSettings(WrkNtfStgVo setting, boolean ready) {
        return new SettingsResponse(setting.getRcptEmail(), setting.getVrfdAt()!=null, setting.getTmZone(),
                setting.getDgstTime(), setting.getDgstCycle(), Boolean.TRUE.equals(setting.getDgstEnabled()),
                Boolean.TRUE.equals(setting.getRmdEnabled()), ready, setting.getNextDgstAt());
    }
    // 예약의 내부 키를 숨기고 외부 UUID만 전달한다.
    public static ReminderResponse toReminder(WrkTodoRmdVo reminder, String todoUuid, String zone) {
        return new ReminderResponse(reminder==null?null:reminder.getUuid(), todoUuid,
                reminder!=null && Boolean.TRUE.equals(reminder.getEnabled()),
                reminder==null?null:reminder.getSendAt(), zone);
    }
    // 민감한 본문·수신 주소 없이 발송 결과를 표시한다.
    public static DeliveryResponse toDelivery(WrkNtfDlvrVo delivery) {
        return new DeliveryResponse(delivery.getUuid(), delivery.getKind(), delivery.getSts(), delivery.getAttempts(),
                delivery.getDueAt(), delivery.getNextTryAt(), delivery.getSentAt(), delivery.getResultCd());
    }
}
