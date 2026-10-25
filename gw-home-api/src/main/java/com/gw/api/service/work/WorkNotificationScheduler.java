package com.gw.api.service.work;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class WorkNotificationScheduler {
    private final WorkNotificationWorker worker;
    // 설정된 서버 주기로 알림 작업자를 실행한다.
    @Scheduled(fixedDelayString="${work.notifications.poll-delay-ms:30000}",initialDelayString="${work.notifications.initial-delay-ms:30000}")
    public void poll() { worker.tick(); }
}
