package com.gw.api.service.work;

import com.gw.api.config.WorkNotificationConfig;
import com.gw.api.config.WorkNotificationProperties;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Microsoft365MailTest {
    private final WorkNotificationProperties settings=new WorkNotificationProperties();
    private final HttpClient http=mock(HttpClient.class);
    private final HttpResponse<String> response=mock(HttpResponse.class);
    private final Clock clock=mock(Clock.class);
    private final AtomicReference<Instant> now=new AtomicReference<>(Instant.parse("2026-10-25T00:00:00Z"));
    private Microsoft365MailTokenClient tokens;

    @BeforeEach void setup() throws Exception {
        settings.setEnabled(true); settings.setSmtpAuthMode(WorkNotificationProperties.SmtpAuthMode.MICROSOFT365_OAUTH2);
        settings.setSmtpHost("smtp.office365.com"); settings.setSmtpUsername("me@example.test"); settings.setFrom("me@example.test");
        settings.setAppUrl("https://example.test"); settings.setMicrosoftTenantId("11111111-1111-4111-8111-111111111111");
        settings.setMicrosoftClientId("22222222-2222-4222-8222-222222222222"); settings.setMicrosoftClientSecret("fixture&secret+only");
        when(clock.instant()).thenAnswer(call -> now.get());
        when(http.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"token_type\":\"Bearer\",\"access_token\":\"fixture-token\",\"expires_in\":3600}");
        tokens=new Microsoft365MailTokenClient(settings,clock,http);
    }

    @Test void tokenUsesFixedTenantEndpointEncodedSecretAndSmtpScope() throws Exception {
        assertEquals("fixture-token",tokens.getAccessToken());
        var request=ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(request.capture(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertEquals("https://login.microsoftonline.com/11111111-1111-4111-8111-111111111111/oauth2/v2.0/token",request.getValue().uri().toString());
        assertEquals(Duration.ofSeconds(10),request.getValue().timeout().orElseThrow());
        String body=requestBody(request.getValue());
        assertTrue(body.contains("client_secret=fixture%26secret%2Bonly"));
        assertTrue(body.contains("scope=https%3A%2F%2Foutlook.office365.com%2F.default"));
        assertTrue(body.contains("grant_type=client_credentials"));
    }
    @Test void concurrentRequestsReuseTokenAndRefreshBeforeExpiry() throws Exception {
        try(var executor=Executors.newFixedThreadPool(4)) {
            var tasks=executor.invokeAll(List.of(tokens::getAccessToken,tokens::getAccessToken,tokens::getAccessToken,tokens::getAccessToken));
            for(var task:tasks) assertEquals("fixture-token",task.get());
        }
        verify(http,times(1)).send(any(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        now.set(now.get().plusSeconds(3540));
        when(response.body()).thenReturn("{\"token_type\":\"Bearer\",\"access_token\":\"new-token\",\"expires_in\":3600}");
        assertEquals("new-token",tokens.getAccessToken());
        verify(http,times(2)).send(any(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }
    @Test void temporaryTokenFailuresUseExistingRetryAndDoNotExposeResponse() {
        for(int status:List.of(429,500,503)) {
            when(response.statusCode()).thenReturn(status);
            when(response.body()).thenReturn("fixture-sensitive-response");
            var failure=assertThrows(Microsoft365MailTokenClient.TokenException.class,tokens::getAccessToken);
            assertTrue(failure.retryable()); assertFalse(failure.toString().contains("sensitive")); assertNull(failure.getCause());
            assertEquals(WorkMailSender.Outcome.RETRY,WorkMailSender.classify(new MailSendException("send",failure)).outcome());
        }
    }
    @Test void deniedTokenIsPermanentAndRedirectIsNotAccepted() {
        for(int status:List.of(400,401,403,302)) {
            when(response.statusCode()).thenReturn(status);
            var failure=assertThrows(Microsoft365MailTokenClient.TokenException.class,tokens::getAccessToken);
            assertFalse(failure.retryable());
            assertEquals(WorkMailSender.Outcome.FAILED,WorkMailSender.classify(new MailSendException("send",failure)).outcome());
        }
    }
    @Test void invalidTenantDoesNotMakeNetworkCall() {
        settings.setMicrosoftTenantId("../another-host");
        assertThrows(Microsoft365MailTokenClient.TokenException.class,tokens::getAccessToken);
        verifyNoInteractions(http);
    }
    @Test void invalidTokenResponseIsNotCachedOrSent() throws Exception {
        for(String body:List.of("","null","{}","{\"token_type\":\"Bearer\",\"access_token\":\"bad\\r\\ntoken\",\"expires_in\":3600}")) {
            when(response.body()).thenReturn(body);
            assertThrows(Microsoft365MailTokenClient.TokenException.class,tokens::getAccessToken);
        }
        verify(http,times(4)).send(any(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }
    @Test void tokenTimeoutIsSafeToRetryBeforeSmtp() throws Exception {
        when(http.send(any(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(new HttpTimeoutException("fixture-only"));
        var failure=assertThrows(Microsoft365MailTokenClient.TokenException.class,tokens::getAccessToken);
        assertTrue(failure.retryable()); assertNull(failure.getCause());
    }
    @Test void configurationAllowsOnlyMicrosoftHostTlsAndXoauth2() {
        JavaMailSenderImpl sender=(JavaMailSenderImpl)new WorkNotificationConfig().notificationMailSender(settings);
        assertInstanceOf(Microsoft365MailSender.class,sender);
        assertNull(sender.getPassword());
        assertEquals("XOAUTH2",sender.getJavaMailProperties().getProperty("mail.smtp.auth.mechanisms"));
        assertEquals("true",sender.getJavaMailProperties().getProperty("mail.smtp.starttls.required"));
        assertEquals("true",sender.getJavaMailProperties().getProperty("mail.smtp.auth.login.disable"));
        settings.setSmtpHost("untrusted.example.test");
        assertThrows(IllegalStateException.class,() -> new WorkNotificationConfig().notificationMailSender(settings));
        settings.setSmtpHost("smtp.office365.com"); settings.setStarttls(false);
        assertThrows(IllegalStateException.class,() -> new WorkNotificationConfig().notificationMailSender(settings));
        settings.setStarttls(true); settings.setFrom("other@example.test");
        assertThrows(IllegalStateException.class,() -> new WorkNotificationConfig().notificationMailSender(settings));
    }
    @Test void missingIdentityIsNotReadyAndDisabledDraftConfigDoesNotConnect() {
        settings.setMicrosoftClientSecret(""); assertFalse(settings.isReady());
        assertThrows(IllegalStateException.class,() -> new WorkNotificationConfig().notificationMailSender(settings));
        settings.setEnabled(false);
        assertDoesNotThrow(() -> new WorkNotificationConfig().notificationMailSender(settings));
    }
    @Test void eachSmtpConnectionReceivesTokenAndAuthenticationFailureInvalidatesCache() throws Exception {
        var provider=mock(Microsoft365MailTokenClient.class);
        when(provider.getAccessToken()).thenReturn("current-token");
        var transport=mock(Transport.class);
        var sender=new TestSender(provider,transport);
        sender.setHost("smtp.office365.com"); sender.setPort(587); sender.setUsername("me@example.test");
        assertSame(transport,sender.connectTransport());
        verify(transport).connect("smtp.office365.com",587,"me@example.test","current-token");
        doThrow(new AuthenticationFailedException("fixture")).when(transport).connect(anyString(),anyInt(),anyString(),anyString());
        assertThrows(AuthenticationFailedException.class,sender::connectTransport);
        verify(provider).invalidate(); verify(transport).close();
    }
    @Test void tokenFailureNeverOpensSmtpOrSendsMessage() throws Exception {
        var provider=mock(Microsoft365MailTokenClient.class);
        when(provider.getAccessToken()).thenThrow(new Microsoft365MailTokenClient.TokenException(true,"OAUTH_TEMPORARY"));
        var transport=mock(Transport.class);
        var sender=new TestSender(provider,transport);
        var outcome=new WorkMailSender(sender,settings).send("me@example.test",new WorkMailSender.Content("알림","본문","<p>본문</p>"),"key");
        assertEquals(WorkMailSender.Outcome.RETRY,outcome.outcome());
        verifyNoInteractions(transport);
    }

    private static final class TestSender extends Microsoft365MailSender {
        private final Transport transport;
        private TestSender(Microsoft365MailTokenClient tokens,Transport transport) { super(tokens); this.transport=transport; }
        @Override protected Transport getTransport(Session session) { return transport; }
    }

    private String requestBody(HttpRequest request) throws Exception {
        var result=new CompletableFuture<String>();
        var bytes=new java.io.ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer buffer) { byte[] value=new byte[buffer.remaining()]; buffer.get(value); bytes.writeBytes(value); }
            public void onError(Throwable error) { result.completeExceptionally(error); }
            public void onComplete() { result.complete(bytes.toString(StandardCharsets.UTF_8)); }
        });
        return result.get(1,TimeUnit.SECONDS);
    }
}
