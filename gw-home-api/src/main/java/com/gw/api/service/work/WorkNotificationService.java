package com.gw.api.service.work;

import com.gw.api.config.WorkNotificationProperties;
import com.gw.api.convert.work.WorkNotificationConvert;
import com.gw.api.dto.work.WorkNotificationDtos.*;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.work.WorkNotificationMapper;
import com.gw.share.common.exception.*;
import com.gw.share.common.response.PageResponse;
import com.gw.share.jvo.work.WorkReminderTargetJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional @RequiredArgsConstructor @Slf4j
public class WorkNotificationService {
    private final WorkNotificationMapper mapper;
    private final AccountLookupService accounts;
    private final WorkNotificationReservations reservations;
    private final WorkNotificationProperties properties;
    private final WorkMailSender mail;
    private final PasswordEncoder passwordEncoder;
    private final Clock notificationClock;
    private final SecureRandom random=new SecureRandom();

    // 본인 메일 알림 설정과 서버 준비 상태를 조회한다.
    public SettingsResponse getSettings(String loginId) {
        return run(loginId,"getNotificationSettings",account ->
            WorkNotificationConvert.toSettings(setting(account),properties.isReady()));
    }

    // 수신 주소·시간대·요약 주기와 활성화 설정을 저장한다.
    public SettingsResponse saveSettings(String loginId,SettingsRequest request) {
        return run(loginId,"saveNotificationSettings",account -> {
            WorkNotificationTime.zone(request.timeZone());
            WrkNtfStgVo setting=setting(account);
            String email=request.email().trim();
            boolean changed=!email.equals(setting.getRcptEmail());
            if (changed) {
                setting.setVrfdAt(null); setting.setVrfyHash(null); setting.setVrfyExpiresAt(null);
            } else if ((request.digestEnabled() || request.reminderEnabled()) && setting.getVrfdAt()==null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,"수신 주소를 먼저 확인하세요.");
            }
            if (!changed && (request.digestEnabled() || request.reminderEnabled())) requireReady();
            setting.setRcptEmail(email); setting.setTmZone(request.timeZone());
            setting.setDgstTime(request.digestTime()); setting.setDgstCycle(request.digestCycle());
            setting.setDgstEnabled(!changed && request.digestEnabled());
            setting.setRmdEnabled(!changed && request.reminderEnabled());
            setting.setVer(setting.getVer()+1); setting.setUpdatedBy(loginId);
            setting.setNextDgstAt(setting.getDgstEnabled()
                    ? WorkNotificationTime.next(notificationClock.instant(),setting.getDgstTime(),setting.getTmZone(),setting.getDgstCycle()):null);
            mapper.updateSetting(setting);
            mapper.updateReminderVersions(account.getIdx(),changed || !setting.getRmdEnabled(),loginId);
            reservations.synchronize(account.getIdx(),loginId);
            return WorkNotificationConvert.toSettings(setting,properties.isReady());
        });
    }

    // 저장된 수신 주소로 제한된 횟수의 확인 코드 메일을 보낸다.
    public String requestVerification(String loginId) {
        return run(loginId,"requestNotificationVerification",account -> {
            requireReady();
            WrkNtfStgVo setting=setting(account);
            if (setting.getRcptEmail().isBlank()) throw new BusinessException(ErrorCode.BAD_REQUEST,"수신 주소를 저장하세요.");
            OffsetDateTime now=OffsetDateTime.now(notificationClock);
            if (setting.getVrfySentAt()!=null && setting.getVrfySentAt().plusSeconds(60).isAfter(now))
                throw new BusinessException(ErrorCode.BAD_REQUEST,"확인 메일은 60초 후 다시 요청할 수 있습니다.");
            if (setting.getVrfyWindowAt()==null || !setting.getVrfyWindowAt().plusHours(1).isAfter(now)) {
                setting.setVrfyWindowAt(now); setting.setVrfyCount(0);
            }
            if (setting.getVrfyCount()>=5) throw new BusinessException(ErrorCode.BAD_REQUEST,"확인 메일 요청은 시간당 5회까지 가능합니다.");
            String code=String.format("%06d",random.nextInt(1_000_000));
            setting.setVrfyHash(passwordEncoder.encode(code)); setting.setVrfyExpiresAt(now.plusMinutes(10));
            setting.setVrfySentAt(now); setting.setVrfyTries(0); setting.setVrfyCount(setting.getVrfyCount()+1);
            setting.setUpdatedBy(loginId); mapper.updateSetting(setting);
            WorkMailSender.Result result=mail.send(setting.getRcptEmail(),
                    new WorkMailSender.Content("[Glass World] 수신 주소 확인","확인 코드: "+code+" (10분 유효)",
                            "<p>수신 주소 확인 코드: <strong>"+code+"</strong></p><p>10분 안에 설정 화면에 입력하세요.</p>"),
                    UUID.randomUUID().toString());
            // 실패해도 발급 제한을 커밋하며 접수불명 메일의 코드도 확인할 수 있게 유지한다.
            return result.outcome().name();
        });
    }

    // 코드 불일치 횟수를 커밋해 반복 추측을 제한한다.
    public VerifyResponse verifyAddress(String loginId,String code) {
        return run(loginId,"verifyNotificationAddress",account -> {
            WrkNtfStgVo setting=setting(account);
            boolean valid=setting.getVrfyHash()!=null && setting.getVrfyExpiresAt()!=null
                    && setting.getVrfyExpiresAt().isAfter(OffsetDateTime.now(notificationClock)) && setting.getVrfyTries()<5;
            if (valid) {
                setting.setVrfyTries(setting.getVrfyTries()+1);
                valid=passwordEncoder.matches(code,setting.getVrfyHash());
                if (valid) { setting.setVrfdAt(OffsetDateTime.now(notificationClock)); setting.setVrfyHash(null); }
                setting.setUpdatedBy(loginId); mapper.updateSetting(setting);
            }
            return new VerifyResponse(valid);
        });
    }

    // 본인 TODO의 개별 알림 설정을 조회한다.
    public ReminderResponse getReminder(String loginId,String todoUuid) {
        return run(loginId,"getTodoReminder",account -> {
            WorkReminderTargetJvo target=target(account.getIdx(),todoUuid);
            return WorkNotificationConvert.toReminder(mapper.selectReminder(account.getIdx(),target.getIdx()),todoUuid,setting(account).getTmZone());
        });
    }

    // 완료되지 않은 최하위 TODO에 미래 절대 시각 알림을 예약한다.
    public ReminderResponse saveReminder(String loginId,String todoUuid,OffsetDateTime sendAt) {
        return run(loginId,"saveTodoReminder",account -> {
            requireReady();
            WrkNtfStgVo setting=setting(account);
            if (setting.getVrfdAt()==null || !Boolean.TRUE.equals(setting.getRmdEnabled()))
                throw new BusinessException(ErrorCode.BAD_REQUEST,"설정에서 수신 주소 확인과 개별 알림을 활성화하세요.");
            WorkReminderTargetJvo target=target(account.getIdx(),todoUuid);
            if (!Boolean.TRUE.equals(target.getActive()) || !Boolean.TRUE.equals(target.getLeaf()) || "DONE".equals(target.getSts()))
                throw new BusinessException(ErrorCode.BAD_REQUEST,"활성 업무의 미완료 최하위 할 일에만 예약할 수 있습니다.");
            if (!sendAt.toInstant().isAfter(notificationClock.instant()))
                throw new BusinessException(ErrorCode.BAD_REQUEST,"미래 발송 시각을 지정하세요.");
            mapper.insertReminder(WrkTodoRmdVo.builder().mbrAcctIdx(account.getIdx()).wrkTodoIdx(target.getIdx())
                    .sendAt(sendAt).strtDt(target.getStrtDt()).dueDt(target.getDueDt()).createdBy(loginId).build());
            reservations.synchronize(account.getIdx(),loginId);
            return WorkNotificationConvert.toReminder(mapper.selectReminder(account.getIdx(),target.getIdx()),todoUuid,setting.getTmZone());
        });
    }

    // 본인 TODO의 대기 개별 알림을 해제한다.
    public void cancelReminder(String loginId,String todoUuid) {
        run(loginId,"cancelTodoReminder",account -> {
            WorkReminderTargetJvo target=target(account.getIdx(),todoUuid);
            mapper.updateCancelReminder(account.getIdx(),target.getIdx(),loginId);
            mapper.updateInvalidDeliveries(account.getIdx()); return null;
        });
    }

    // 본인의 발송 결과를 페이징하여 조회한다.
    public PageResponse<DeliveryResponse> getHistory(String loginId,int page,int size) {
        return run(loginId,"getNotificationHistory",account -> {
            if (page<1 || size<1 || size>100) throw new BusinessException(ErrorCode.BAD_REQUEST);
            long count=mapper.selectHistoryCount(account.getIdx());
            return PageResponse.of(mapper.selectHistory(account.getIdx(),size,((long)page-1)*size).stream()
                    .map(WorkNotificationConvert::toDelivery).toList(),page,size,count);
        });
    }

    private WrkNtfStgVo setting(AcctVo account) {
        mapper.insertDefaultSetting(account.getIdx(),account.getEmail()==null?"":account.getEmail(),account.getLgnId());
        return mapper.selectSetting(account.getIdx());
    }
    private WorkReminderTargetJvo target(Long owner,String todoUuid) {
        try { UUID.fromString(todoUuid); } catch (RuntimeException failure) { throw new BusinessException(ErrorCode.BAD_REQUEST); }
        WorkReminderTargetJvo target=mapper.selectTarget(owner,todoUuid);
        if (target==null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return target;
    }
    private void requireReady() {
        if (!properties.isReady()) throw new BusinessException(ErrorCode.BAD_REQUEST,"메일 서버가 아직 설정되지 않았습니다.");
    }
    private <T> T run(String loginId,String operation,Function<AcctVo,T> action) {
        log.info("{} 시작 - loginId: {}",operation,loginId);
        try {
            AcctVo account=accounts.getAccountByLoginId(loginId);
            reservations.lock(account.getIdx());
            T response=action.apply(account);
            log.info("{} 완료 - loginId: {}",operation,loginId);
            return response;
        } catch (RuntimeException failure) {
            log.warn("{} 실패 - loginId: {}, type: {}",operation,loginId,failure.getClass().getSimpleName());
            throw failure;
        }
    }
}
