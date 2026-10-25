package com.gw.api.service.work;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.api.config.WorkNotificationProperties;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Microsoft365MailTokenClient {
    private final WorkNotificationProperties settings;
    private final Clock clock;
    private final HttpClient client;
    private final ObjectMapper json=new ObjectMapper();
    private String cachedToken;
    private Instant refreshAt=Instant.MIN;

    public Microsoft365MailTokenClient(WorkNotificationProperties settings,Clock clock) {
        this(settings,clock,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    Microsoft365MailTokenClient(WorkNotificationProperties settings,Clock clock,HttpClient client) {
        this.settings=settings;
        this.clock=clock;
        this.client=client;
    }

    // SMTP 전용 앱 토큰을 메모리에만 보관하고 만료 60초 전에 직렬화하여 갱신한다.
    public synchronized String getAccessToken() throws TokenException {
        log.debug("Microsoft 메일 토큰 조회 시작");
        try {
            if (cachedToken!=null && clock.instant().isBefore(refreshAt)) {
                log.debug("Microsoft 메일 토큰 조회 완료 - 캐시");
                return cachedToken;
            }
            if (!settings.hasMicrosoftIdentity()) throw new TokenException(false,"OAUTH_CONFIG_INVALID");
            Instant requestedAt=clock.instant();
            String body="grant_type=client_credentials&client_id="+encode(settings.getMicrosoftClientId())
                    +"&client_secret="+encode(settings.getMicrosoftClientSecret())
                    +"&scope="+encode("https://outlook.office365.com/.default");
            HttpRequest request=HttpRequest.newBuilder(URI.create("https://login.microsoftonline.com/"
                    +settings.getMicrosoftTenantId()+"/oauth2/v2.0/token"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type","application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build();
            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status=response.statusCode();
            if (status==429 || status>=500) throw new TokenException(true,"OAUTH_TEMPORARY");
            if (status!=200) throw new TokenException(false,"OAUTH_REJECTED");
            if (response.body()==null || response.body().length()>65536) throw new TokenException(false,"OAUTH_RESPONSE_INVALID");
            var result=json.readTree(response.body());
            if (result==null) throw new TokenException(false,"OAUTH_RESPONSE_INVALID");
            String token=result.path("access_token").asText("");
            long lifetime=result.path("expires_in").asLong(0);
            if (!"Bearer".equalsIgnoreCase(result.path("token_type").asText()) || token.isBlank()
                    || token.chars().anyMatch(character -> Character.isWhitespace(character) || Character.isISOControl(character))
                    || lifetime<=0) throw new TokenException(false,"OAUTH_RESPONSE_INVALID");
            cachedToken=token;
            refreshAt=requestedAt.plusSeconds(Math.max(0,Math.min(lifetime,86400)-60));
            log.debug("Microsoft 메일 토큰 조회 완료 - 갱신");
            return token;
        } catch (TokenException failure) {
            log.warn("Microsoft 메일 토큰 조회 실패 - code: {}",failure.code());
            throw failure;
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            log.warn("Microsoft 메일 토큰 조회 실패 - 중단");
            throw new TokenException(true,"OAUTH_TEMPORARY");
        } catch (IOException failure) {
            // HTTP/파싱 예외 원문에는 응답·비밀이 섞일 수 있어 전달하거나 기록하지 않는다.
            log.warn("Microsoft 메일 토큰 조회 실패 - 통신 또는 응답 오류");
            throw new TokenException(true,"OAUTH_TEMPORARY");
        }
    }

    // SMTP 인증 거부 후 다음 발송에서는 새 토큰을 받는다.
    public synchronized void invalidate() {
        log.debug("Microsoft 메일 토큰 초기화 시작");
        cachedToken=null;
        refreshAt=Instant.MIN;
        log.debug("Microsoft 메일 토큰 초기화 완료");
    }

    private String encode(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8); }

    // SMTP에 아직 연결하지 않은 토큰 발급 오류만 재시도 여부를 명시한다.
    public static final class TokenException extends MessagingException {
        private final boolean retryable;
        private final String code;
        public TokenException(boolean retryable,String code) {
            super(code);
            this.retryable=retryable;
            this.code=code;
        }
        public boolean retryable() { return retryable; }
        public String code() { return code; }
    }
}
