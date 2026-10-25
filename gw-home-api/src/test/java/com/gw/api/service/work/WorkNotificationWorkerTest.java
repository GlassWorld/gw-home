package com.gw.api.service.work;

import com.gw.api.config.WorkNotificationProperties;
import com.gw.infra.db.mapper.work.WorkNotificationMapper;
import com.gw.share.jvo.work.WorkReminderTargetJvo;
import com.gw.share.vo.work.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkNotificationWorkerTest {
    final WorkNotificationMapper mapper=mock(WorkNotificationMapper.class);
    final WorkNotificationReservations reservations=mock(WorkNotificationReservations.class);
    final WorkMailSender mail=mock(WorkMailSender.class);
    final WorkNotificationProperties properties=new WorkNotificationProperties();
    final Clock clock=Clock.fixed(Instant.parse("2026-10-25T00:01:00Z"),ZoneOffset.UTC);
    WrkNtfDlvrVo delivery; WrkNtfStgVo setting; WorkReminderTargetJvo target;
    WorkNotificationWorker worker;
    @BeforeEach void setup() {
        properties.setEnabled(true); properties.setSmtpHost("localhost"); properties.setFrom("sender@example.test"); properties.setAppUrl("https://example.test");
        var manager=mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        worker=new WorkNotificationWorker(mapper,reservations,properties,mail,new WorkNotificationMessage(properties),clock,manager);
        setting=WrkNtfStgVo.builder().mbrAcctIdx(7L).rcptEmail("me@example.test").vrfdAt(OffsetDateTime.now(clock))
                .ver(1L).tmZone("Asia/Seoul").dgstTime("09:00").dgstCycle("DAILY").rmdEnabled(true).dgstEnabled(true).createdBy("me").build();
        delivery=WrkNtfDlvrVo.builder().idx(1L).uuid("delivery").mbrAcctIdx(7L).wrkTodoRmdIdx(3L)
                .claimToken("token").sts("CLAIMED").kind("REMINDER").stgVer(1L).rmdVer(1L)
                .dueAt(OffsetDateTime.now(clock).minusMinutes(1)).attempts(0).createdBy("me").build();
        target=WorkReminderTargetJvo.builder().idx(22L).active(true).leaf(true).sts("TODO").ttl("할 일").workUnitTtl("업무").uuid("todo").workUnitUuid("unit").build();
        when(mapper.selectSetting(7L)).thenReturn(setting);
        when(mapper.selectActiveAccount(7L)).thenReturn(true);
        when(mapper.selectClaimDeliveries()).thenReturn(List.of(delivery),List.of());
        when(mapper.selectDelivery(1L)).thenReturn(delivery);
        when(mapper.selectReminderTarget(7L,3L)).thenReturn(target);
        when(mapper.selectReminder(7L,22L)).thenReturn(WrkTodoRmdVo.builder().enabled(true).ver(1L).build());
        when(mapper.updateStartSending(1L,"token")).thenReturn(1);
        when(mail.send(anyString(),any(),anyString())).thenReturn(new WorkMailSender.Result(WorkMailSender.Outcome.SENT,"SMTP_ACCEPTED"));
    }
    @Test void successfulSendPersistsStartBeforeExternalCallAndCompletionAfter() {
        worker.tick();
        var order=inOrder(reservations,mapper,mail);
        order.verify(reservations).lock(7L); order.verify(reservations).synchronize(7L,"me");
        order.verify(mapper).updateStartSending(1L,"token");
        order.verify(mail).send(eq("me@example.test"),any(),eq("delivery"));
        order.verify(mapper).updateDeliveryResult(1L,"token","SENT","SMTP_ACCEPTED",null);
    }
    @Test void staleClaimCannotSend() {
        when(mapper.updateStartSending(1L,"token")).thenReturn(0); worker.tick(); verifyNoInteractions(mail);
    }
    @Test void completedTodoCancelsClaimWithoutSending() {
        target.setSts("DONE"); worker.tick(); verifyNoInteractions(mail);
        verify(mapper).updateClaimCancelled(1L,"token","TODO_UNAVAILABLE");
    }
    @Test void changedSettingsOrDisabledAccountCancelClaim() {
        setting.setVer(2L); worker.tick(); verifyNoInteractions(mail);
        verify(mapper).updateClaimCancelled(1L,"token","INVALIDATED");
    }
    @Test void temporaryFailureRetriesAfterOneMinute() {
        when(mail.send(anyString(),any(),anyString())).thenReturn(new WorkMailSender.Result(WorkMailSender.Outcome.RETRY,"SMTP_TEMPORARY"));
        worker.tick(); verify(mapper).updateDeliveryResult(1L,"token","RETRY","SMTP_TEMPORARY",OffsetDateTime.now(clock).plusMinutes(1));
    }
    @Test void secondFailureWaitsFiveMinutes() {
        delivery.setAttempts(1);
        when(mail.send(anyString(),any(),anyString())).thenReturn(new WorkMailSender.Result(WorkMailSender.Outcome.RETRY,"SMTP_TEMPORARY"));
        worker.tick(); verify(mapper).updateDeliveryResult(1L,"token","RETRY","SMTP_TEMPORARY",OffsetDateTime.now(clock).plusMinutes(5));
    }
    @Test void thirdFailureExhaustsRetries() {
        delivery.setAttempts(2);
        when(mail.send(anyString(),any(),anyString())).thenReturn(new WorkMailSender.Result(WorkMailSender.Outcome.RETRY,"SMTP_TEMPORARY"));
        worker.tick(); verify(mapper).updateDeliveryResult(1L,"token","FAILED","RETRY_EXHAUSTED",null);
    }
    @Test void ambiguousAcceptanceNeverBecomesRetry() {
        when(mail.send(anyString(),any(),anyString())).thenReturn(new WorkMailSender.Result(WorkMailSender.Outcome.UNKNOWN,"SMTP_UNCERTAIN"));
        worker.tick(); verify(mapper).updateDeliveryResult(1L,"token","UNKNOWN","SMTP_UNCERTAIN",null);
    }
    @Test void emptyDigestIsSkippedAndUsesConfiguredLocalDate() {
        delivery.setKind("DIGEST");
        when(mapper.selectDigestTodos(any())).thenAnswer(call -> {
            WorkTodoListSearchVo search=call.getArgument(0);
            assertEquals(7L,search.getMbrAcctIdx()); assertEquals(LocalDate.parse("2026-10-25"),search.getToday());
            return List.of();
        });
        worker.tick(); verify(mapper).updateClaimCancelled(1L,"token","EMPTY_DIGEST"); verifyNoInteractions(mail);
    }
    @Test void delayedSchedulerPlansOnlyTodayAndAdvancesToTomorrow() {
        when(mapper.selectClaimDeliveries()).thenReturn(List.of());
        when(mapper.selectDueSettings()).thenReturn(List.of(7L));
        setting.setNextDgstAt(OffsetDateTime.parse("2026-10-20T09:00:00+09:00"));
        worker.tick();
        verify(mapper).insertDigest(setting,"D:2026-10-25",OffsetDateTime.parse("2026-10-25T09:00:00+09:00"));
        assertEquals(OffsetDateTime.parse("2026-10-26T09:00:00+09:00"),setting.getNextDgstAt());
    }
    @Test void disabledServerDoesNotTouchDatabaseOrMail() {
        properties.setEnabled(false); worker.tick(); verifyNoInteractions(mapper,mail,reservations);
    }
}
