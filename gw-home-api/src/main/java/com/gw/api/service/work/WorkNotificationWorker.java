package com.gw.api.service.work;

import com.gw.api.config.WorkNotificationProperties;
import com.gw.infra.db.mapper.work.WorkNotificationMapper;
import com.gw.share.jvo.work.*;
import com.gw.share.vo.work.*;
import java.time.*;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service @Slf4j
public class WorkNotificationWorker {
    private final WorkNotificationMapper mapper;
    private final WorkNotificationReservations reservations;
    private final WorkNotificationProperties properties;
    private final WorkMailSender sender;
    private final WorkNotificationMessage messages;
    private final Clock clock;
    private final TransactionTemplate transaction;
    private final TransactionTemplate independent;

    public WorkNotificationWorker(WorkNotificationMapper mapper,WorkNotificationReservations reservations,
            WorkNotificationProperties properties,WorkMailSender sender,WorkNotificationMessage messages,
            Clock notificationClock,PlatformTransactionManager manager) {
        this.mapper=mapper; this.reservations=reservations; this.properties=properties;
        this.sender=sender; this.messages=messages; this.clock=notificationClock;
        transaction=new TransactionTemplate(manager);
        independent=new TransactionTemplate(manager);
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // 브라우저와 무관하게 오늘 요약을 계획하고 대기 메일을 한 건씩 확보·발송한다.
    public void tick() {
        if (!properties.isReady()) return;
        log.info("알림 작업 시작");
        try {
            independent.executeWithoutResult(status -> {
                mapper.updateUnknownSends(); mapper.updateExpiredClaims(); mapper.updateExpiredDeliveries();
            });
            for (Long owner:mapper.selectDueSettings()) {
                try { transaction.executeWithoutResult(status -> planDigest(owner)); }
                catch (RuntimeException failure) { log.warn("요약 계획 실패 - owner: {}, type: {}",owner,failure.getClass().getSimpleName()); }
            }
            for (int count=0;count<20;count++) {
                List<WrkNtfDlvrVo> claims=independent.execute(status -> mapper.selectClaimDeliveries());
                if (claims==null || claims.isEmpty()) break;
                for (WrkNtfDlvrVo claim:claims) {
                    try { transaction.executeWithoutResult(status -> dispatch(claim)); }
                    catch (RuntimeException failure) { log.warn("알림 처리 실패 - uuid: {}, type: {}",claim.getUuid(),failure.getClass().getSimpleName()); }
                }
            }
            log.info("알림 작업 완료");
        } catch (RuntimeException failure) {
            log.warn("알림 작업 실패 - type: {}",failure.getClass().getSimpleName());
        }
    }

    private void planDigest(Long owner) {
        reservations.lock(owner);
        WrkNtfStgVo setting=mapper.selectSetting(owner);
        Instant now=clock.instant();
        if (setting==null || !Boolean.TRUE.equals(setting.getDgstEnabled()) || setting.getNextDgstAt()==null
                || setting.getNextDgstAt().toInstant().isAfter(now)) return;
        LocalDate today=now.atZone(WorkNotificationTime.zone(setting.getTmZone())).toLocalDate();
        OffsetDateTime due=WorkNotificationTime.at(today,setting.getDgstTime(),setting.getTmZone());
        if (setting.getVrfdAt()!=null && mapper.selectActiveAccount(owner)
                && WorkNotificationTime.eligible(today,setting.getDgstCycle()) && !due.toInstant().isAfter(now)) {
            mapper.insertDigest(setting,"D:"+today,due);
        }
        setting.setNextDgstAt(WorkNotificationTime.next(now,setting.getDgstTime(),setting.getTmZone(),setting.getDgstCycle()));
        setting.setUpdatedBy(setting.getCreatedBy()); mapper.updateSetting(setting);
    }

    private void dispatch(WrkNtfDlvrVo claim) {
        reservations.lock(claim.getMbrAcctIdx());
        reservations.synchronize(claim.getMbrAcctIdx(),claim.getCreatedBy());
        WrkNtfDlvrVo current=mapper.selectDelivery(claim.getIdx());
        if (current==null || !"CLAIMED".equals(current.getSts()) || !Objects.equals(current.getClaimToken(),claim.getClaimToken())) return;
        WrkNtfStgVo setting=mapper.selectSetting(claim.getMbrAcctIdx());
        if (setting==null || setting.getVrfdAt()==null || !Objects.equals(setting.getVer(),current.getStgVer())
                || !mapper.selectActiveAccount(claim.getMbrAcctIdx())
                || current.getDueAt().toInstant().plus(Duration.ofHours(24)).isBefore(clock.instant())) {
            cancel(current,"INVALIDATED"); return;
        }
        WorkMailSender.Content content;
        if ("REMINDER".equals(current.getKind())) {
            WorkReminderTargetJvo target=mapper.selectReminderTarget(current.getMbrAcctIdx(),current.getWrkTodoRmdIdx());
            WrkTodoRmdVo reminder=target==null?null:mapper.selectReminder(current.getMbrAcctIdx(),target.getIdx());
            if (!Boolean.TRUE.equals(setting.getRmdEnabled()) || target==null || !Boolean.TRUE.equals(target.getActive())
                    || !Boolean.TRUE.equals(target.getLeaf()) || "DONE".equals(target.getSts()) || reminder==null
                    || !Boolean.TRUE.equals(reminder.getEnabled()) || !Objects.equals(reminder.getVer(),current.getRmdVer())) {
                cancel(current,"TODO_UNAVAILABLE"); return;
            }
            content=messages.reminder(target);
        } else {
            LocalDate today=clock.instant().atZone(WorkNotificationTime.zone(setting.getTmZone())).toLocalDate();
            if (!Boolean.TRUE.equals(setting.getDgstEnabled())
                    || !current.getDueAt().atZoneSameInstant(WorkNotificationTime.zone(setting.getTmZone())).toLocalDate().equals(today)) {
                cancel(current,"DIGEST_EXPIRED"); return;
            }
            List<WorkTodoListJvo> todos=mapper.selectDigestTodos(WorkTodoListSearchVo.builder()
                    .mbrAcctIdx(current.getMbrAcctIdx()).today(today).includeInactive(false).build());
            if (todos.isEmpty()) { cancel(current,"EMPTY_DIGEST"); return; }
            content=messages.digest(todos,today,setting.getTmZone());
        }
        // 계정 잠금은 외부 호출까지 유지한다. 발송 시작 표시는 별도 커밋하여 장애 후 재발송을 막는다.
        Integer started=independent.execute(status -> mapper.updateStartSending(current.getIdx(),current.getClaimToken()));
        if (started==null || started!=1) return;
        WorkMailSender.Result result=sender.send(setting.getRcptEmail(),content,current.getUuid());
        int attempt=current.getAttempts()+1;
        String state=result.outcome().name();
        String code=result.code();
        OffsetDateTime nextTry=null;
        if (result.outcome()==WorkMailSender.Outcome.RETRY) {
            if (attempt>=3) { state="FAILED"; code="RETRY_EXHAUSTED"; }
            else nextTry=OffsetDateTime.now(clock).plusMinutes(attempt==1?1:5);
        }
        final String finalState=state,finalCode=code;
        final OffsetDateTime retryAt=nextTry;
        independent.executeWithoutResult(status ->
                mapper.updateDeliveryResult(current.getIdx(),current.getClaimToken(),finalState,finalCode,retryAt));
    }
    private void cancel(WrkNtfDlvrVo delivery,String code) {
        mapper.updateClaimCancelled(delivery.getIdx(),delivery.getClaimToken(),code);
    }
}
