package com.gw.api.service.work;

import com.gw.api.config.WorkNotificationProperties;
import com.gw.api.dto.work.WorkNotificationDtos.SettingsRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.work.WorkNotificationMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.jvo.work.WorkReminderTargetJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.*;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkNotificationServiceTest {
    final WorkNotificationMapper mapper=mock(WorkNotificationMapper.class);
    final AccountLookupService accounts=mock(AccountLookupService.class);
    final WorkNotificationReservations reservations=mock(WorkNotificationReservations.class);
    final WorkMailSender sender=mock(WorkMailSender.class);
    final PasswordEncoder encoder=mock(PasswordEncoder.class);
    final WorkNotificationProperties properties=new WorkNotificationProperties();
    final Clock clock=Clock.fixed(Instant.parse("2026-10-25T00:00:00Z"),ZoneOffset.UTC);
    final String todoUuid="11111111-1111-4111-8111-111111111111";
    WrkNtfStgVo setting;
    WorkReminderTargetJvo target;
    WorkNotificationService service;

    @BeforeEach void setup() {
        properties.setEnabled(true); properties.setSmtpHost("localhost"); properties.setFrom("sender@example.test"); properties.setAppUrl("https://example.test");
        when(accounts.getAccountByLoginId("me")).thenReturn(AcctVo.builder().idx(7L).lgnId("me").email("me@example.test").build());
        setting=WrkNtfStgVo.builder().mbrAcctIdx(7L).rcptEmail("me@example.test").vrfdAt(OffsetDateTime.now(clock))
                .vrfyTries(0).vrfyCount(0).ver(1L).tmZone("Asia/Seoul").dgstTime("09:00").dgstCycle("DAILY")
                .rmdEnabled(true).dgstEnabled(true).build();
        when(mapper.selectSetting(7L)).thenReturn(setting);
        target=WorkReminderTargetJvo.builder().idx(22L).uuid(todoUuid).active(true).leaf(true).sts("TODO").build();
        when(mapper.selectTarget(7L,todoUuid)).thenReturn(target);
        service=new WorkNotificationService(mapper,accounts,reservations,properties,sender,encoder,clock);
    }
    @Test void differentAddressDisablesBothKindsAndInvalidatesOldReservations() {
        var result=service.saveSettings("me",new SettingsRequest("new@example.test","UTC","10:15","WEEKDAYS",true,true));
        assertFalse(result.verified()); assertFalse(result.digestEnabled()); assertFalse(result.reminderEnabled());
        assertNull(setting.getVrfyHash()); assertNull(setting.getNextDgstAt()); assertEquals(2L,setting.getVer());
        var order=inOrder(reservations,mapper);
        order.verify(reservations).lock(7L);
        order.verify(mapper).updateSetting(setting);
        order.verify(mapper).updateReminderVersions(7L,true,"me");
        order.verify(reservations).synchronize(7L,"me");
    }
    @Test void unverifiedAddressCannotEnableNotifications() {
        setting.setVrfdAt(null);
        assertThrows(BusinessException.class,() -> service.saveSettings("me",new SettingsRequest("me@example.test","Asia/Seoul","09:00","DAILY",true,true)));
        verify(mapper,never()).updateSetting(any());
    }
    @Test void wrongCodesIncrementAttemptsAndSixthAttemptCannotVerify() {
        setting.setVrfdAt(null); setting.setVrfyHash("hash"); setting.setVrfyExpiresAt(OffsetDateTime.now(clock).plusMinutes(10));
        when(encoder.matches("123456","hash")).thenReturn(false);
        for(int index=0;index<5;index++) assertFalse(service.verifyAddress("me","123456").verified());
        when(encoder.matches("123456","hash")).thenReturn(true);
        assertFalse(service.verifyAddress("me","123456").verified());
        assertEquals(5,setting.getVrfyTries()); verify(mapper,times(5)).updateSetting(setting);
    }
    @Test void correctUnexpiredCodeVerifiesAddressWithoutEnablingMail() {
        setting.setVrfdAt(null); setting.setRmdEnabled(false); setting.setDgstEnabled(false);
        setting.setVrfyHash("hash"); setting.setVrfyExpiresAt(OffsetDateTime.now(clock).plusMinutes(1));
        when(encoder.matches("123456","hash")).thenReturn(true);
        assertTrue(service.verifyAddress("me","123456").verified());
        assertNotNull(setting.getVrfdAt()); assertNull(setting.getVrfyHash()); assertFalse(setting.getRmdEnabled());
        assertFalse(service.verifyAddress("me","123456").verified());
    }
    @Test void expiredCodeAndIssuanceRateLimitsBlockRequests() {
        setting.setVrfyHash("hash"); setting.setVrfyExpiresAt(OffsetDateTime.now(clock).minusSeconds(1));
        assertFalse(service.verifyAddress("me","123456").verified());
        setting.setVrfySentAt(OffsetDateTime.now(clock).minusSeconds(59));
        assertThrows(BusinessException.class,() -> service.requestVerification("me"));
        setting.setVrfySentAt(null); setting.setVrfyWindowAt(OffsetDateTime.now(clock)); setting.setVrfyCount(5);
        assertThrows(BusinessException.class,() -> service.requestVerification("me"));
        verifyNoInteractions(sender,encoder);
    }
    @Test void smtpFailureReturnsNormallySoVerificationLimitsAreCommitted() {
        when(encoder.encode(anyString())).thenReturn("hashed-code");
        when(sender.send(anyString(),any(),anyString())).thenReturn(new WorkMailSender.Result(WorkMailSender.Outcome.UNKNOWN,"SMTP_UNCERTAIN"));
        assertEquals("UNKNOWN",service.requestVerification("me"));
        assertEquals(1,setting.getVrfyCount()); assertEquals("hashed-code",setting.getVrfyHash());
        verify(mapper).updateSetting(setting);
        var code=ArgumentCaptor.forClass(String.class); verify(encoder).encode(code.capture());
        assertTrue(code.getValue().matches("[0-9]{6}"));
    }
    @Test void otherUsersTodoIsNotFoundAndNeverInserted() {
        when(mapper.selectTarget(7L,todoUuid)).thenReturn(null);
        assertThrows(BusinessException.class,() -> service.saveReminder("me",todoUuid,OffsetDateTime.now(clock).plusHours(1)));
        verify(mapper,never()).insertReminder(any());
    }
    @Test void completedParentInactiveAndPastReservationsAreRejected() {
        for(int index=0;index<4;index++) {
            target.setSts(index==0?"DONE":"TODO"); target.setLeaf(index!=1); target.setActive(index!=2);
            OffsetDateTime sendAt=OffsetDateTime.now(clock).plusMinutes(index==3?-1:1);
            assertThrows(BusinessException.class,() -> service.saveReminder("me",todoUuid,sendAt));
        }
        verify(mapper,never()).insertReminder(any());
    }
    @Test void validReminderUsesActorAndOwnerThenSynchronizesInSameTransaction() {
        var sendAt=OffsetDateTime.now(clock).plusHours(1);
        when(mapper.selectReminder(7L,22L)).thenReturn(WrkTodoRmdVo.builder().uuid(todoUuid).enabled(true).sendAt(sendAt).build());
        assertTrue(service.saveReminder("me",todoUuid,sendAt).enabled());
        var value=ArgumentCaptor.forClass(WrkTodoRmdVo.class); verify(mapper).insertReminder(value.capture());
        assertEquals(7L,value.getValue().getMbrAcctIdx()); assertEquals(22L,value.getValue().getWrkTodoIdx()); assertEquals("me",value.getValue().getCreatedBy());
        verify(reservations).synchronize(7L,"me");
        service.cancelReminder("me",todoUuid);
        verify(mapper).updateCancelReminder(7L,22L,"me"); verify(mapper).updateInvalidDeliveries(7L);
    }
}
