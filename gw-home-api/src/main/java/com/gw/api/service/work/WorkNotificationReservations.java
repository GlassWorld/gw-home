package com.gw.api.service.work;

import com.gw.infra.db.mapper.work.WorkNotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Slf4j
@Transactional(propagation=Propagation.MANDATORY)
public class WorkNotificationReservations {
    private final WorkNotificationMapper mapper;
    // 업무 행을 잠그기 전에 계정별 알림/수정 순서를 직렬화한다.
    public void lock(Long owner) {
        log.debug("알림 계정 잠금 시작 - owner: {}",owner);
        try { mapper.selectOwnerLock(owner); log.debug("알림 계정 잠금 완료 - owner: {}",owner); }
        catch (RuntimeException error) { log.warn("알림 계정 잠금 실패 - owner: {}",owner); throw error; }
    }
    // 트리나 업무 변경과 같은 트랜잭션에서 예약을 무효화하고 미래 건을 재생성한다.
    public void synchronize(Long owner,String actor) {
        log.debug("알림 예약 정합성 확인 시작 - owner: {}",owner);
        try {
            mapper.updateInvalidReminders(owner,actor);
            mapper.updateChangedSchedules(owner,actor);
            mapper.updateInvalidDeliveries(owner);
            mapper.insertReminderDeliveries(owner);
            log.debug("알림 예약 정합성 확인 완료 - owner: {}",owner);
        } catch (RuntimeException error) { log.warn("알림 예약 정합성 확인 실패 - owner: {}",owner); throw error; }
    }
}
