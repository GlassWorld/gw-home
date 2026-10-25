package com.gw.api.service.work;

import com.gw.api.config.WorkNotificationProperties;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.smtp.SMTPSenderFailedException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Slf4j
public class WorkMailSender {
    public enum Outcome { SENT, RETRY, FAILED, UNKNOWN }
    public record Result(Outcome outcome,String code) {}
    public record Content(String subject,String text,String html) {}
    private final JavaMailSender notificationMailSender;
    private final WorkNotificationProperties properties;

    // 본문과 주소를 로그에 남기지 않고 SMTP 접수 결과를 분류한다.
    public Result send(String recipient,Content content,String messageKey) {
        log.info("알림 메일 발송 시작 - key: {}",messageKey);
        if (!properties.isReady()) return new Result(Outcome.FAILED,"SERVER_NOT_READY");
        try {
            MimeMessage message=notificationMailSender.createMimeMessage();
            MimeMessageHelper helper=new MimeMessageHelper(message,true,StandardCharsets.UTF_8.name());
            if (properties.getFromName().isBlank()) helper.setFrom(properties.getFrom());
            else helper.setFrom(properties.getFrom(),properties.getFromName());
            helper.setTo(recipient);
            helper.setSubject(content.subject()); helper.setText(content.text(),content.html());
            message.setHeader("Message-ID","<"+messageKey+"@gw-home.notification>");
            notificationMailSender.send(message);
            log.info("알림 메일 발송 완료 - key: {}",messageKey);
            return new Result(Outcome.SENT,"SMTP_ACCEPTED");
        } catch (Exception failure) {
            Result result=classify(failure);
            log.warn("알림 메일 발송 실패 - key: {}, result: {}",messageKey,result.code());
            return result;
        }
    }

    // 명시적 비접수만 재시도하고 데이터 전송 중 연결 유실은 접수불명으로 남긴다.
    static Result classify(Throwable failure) {
        Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Throwable> pending=new ArrayDeque<>(); pending.add(failure);
        boolean temporary=false,permanent=false;
        while (!pending.isEmpty()) {
            Throwable current=pending.removeFirst();
            if (!seen.add(current)) continue;
            if (current instanceof Microsoft365MailTokenClient.TokenException token)
                return new Result(token.retryable()?Outcome.RETRY:Outcome.FAILED,token.code());
            if (current instanceof SendFailedException send && send.getValidSentAddresses()!=null
                    && send.getValidSentAddresses().length>0) return new Result(Outcome.UNKNOWN,"PARTIAL_ACCEPTANCE");
            int code=current instanceof SMTPSendFailedException smtp ? smtp.getReturnCode()
                    : current instanceof SMTPAddressFailedException address ? address.getReturnCode()
                    : current instanceof SMTPSenderFailedException sender ? sender.getReturnCode() : 0;
            temporary |= code>=400 && code<500 || current instanceof ConnectException || current instanceof UnknownHostException;
            permanent |= code>=500 && code<600 || current instanceof MailAuthenticationException
                    || current instanceof AuthenticationFailedException;
            if (current.getCause()!=null) pending.add(current.getCause());
            if (current instanceof MessagingException mail && mail.getNextException()!=null) pending.add(mail.getNextException());
            if (current instanceof MailSendException mail) pending.addAll(mail.getFailedMessages().values());
        }
        if (permanent) return new Result(Outcome.FAILED,"SMTP_REJECTED");
        if (temporary) return new Result(Outcome.RETRY,"SMTP_TEMPORARY");
        return new Result(Outcome.UNKNOWN,"SMTP_UNCERTAIN");
    }
}
