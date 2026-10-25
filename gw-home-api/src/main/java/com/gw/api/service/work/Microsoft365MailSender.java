package com.gw.api.service.work;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import org.springframework.mail.javamail.JavaMailSenderImpl;

public class Microsoft365MailSender extends JavaMailSenderImpl {
    private final Microsoft365MailTokenClient tokens;

    public Microsoft365MailSender(Microsoft365MailTokenClient tokens) {
        this.tokens=tokens;
    }

    // 공유 sender의 비밀번호를 바꾸지 않고 각 연결에 현재 OAuth 토큰을 전달한다.
    @Override
    protected Transport connectTransport() throws MessagingException {
        String token=tokens.getAccessToken();
        Transport transport=getTransport(getSession());
        try {
            transport.connect(getHost(),getPort(),getUsername(),token);
            return transport;
        } catch (MessagingException failure) {
            if (failure instanceof AuthenticationFailedException) tokens.invalidate();
            try { transport.close(); } catch (MessagingException ignored) { /* 실패한 연결만 정리한다. */ }
            throw failure;
        }
    }
}
