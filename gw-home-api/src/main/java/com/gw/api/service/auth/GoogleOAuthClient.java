package com.gw.api.service.auth;

import com.gw.api.config.GoogleOAuthProperties;
import com.gw.api.dto.auth.GoogleTokenInfoResponse;
import com.gw.api.dto.auth.GoogleTokenResponse;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleOAuthClient {

    private static final String SCOPE = "openid email profile";
    private final GoogleOAuthProperties properties;
    private final RestClient restClient;

    public GoogleOAuthClient(GoogleOAuthProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    public String generateState() {
        return UUID.randomUUID().toString();
    }

    public String buildAuthorizationUrl(String redirectUri, String state) {
        assertConfigured();

        return UriComponentsBuilder.fromUriString(properties.getAuthorizationUri())
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("state", state)
                .queryParam("prompt", "select_account")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    public GoogleTokenInfoResponse fetchVerifiedGoogleAccount(String code, String redirectUri) {
        assertConfigured();
        GoogleTokenResponse tokenResponse = requestToken(code, redirectUri);

        if (tokenResponse == null || tokenResponse.idToken() == null || tokenResponse.idToken().isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Google 인증 토큰을 확인할 수 없습니다.");
        }

        GoogleTokenInfoResponse tokenInfo = requestTokenInfo(tokenResponse.idToken());

        if (tokenInfo == null || tokenInfo.sub() == null || tokenInfo.sub().isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Google 계정 식별자를 확인할 수 없습니다.");
        }

        if (!properties.getClientId().equals(tokenInfo.aud())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Google 인증 대상이 올바르지 않습니다.");
        }

        if (!Boolean.TRUE.equals(tokenInfo.emailVerified())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Google 이메일 인증이 완료되지 않았습니다.");
        }

        return tokenInfo;
    }

    private GoogleTokenResponse requestToken(String code, String redirectUri) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("code", code);
        requestBody.add("client_id", properties.getClientId());
        requestBody.add("client_secret", properties.getClientSecret());
        requestBody.add("redirect_uri", redirectUri);
        requestBody.add("grant_type", "authorization_code");

        try {
            return restClient.post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(requestBody)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Google 인증 코드 검증에 실패했습니다.");
        }
    }

    private GoogleTokenInfoResponse requestTokenInfo(String idToken) {
        try {
            return restClient.get()
                    .uri(UriComponentsBuilder.fromUriString(properties.getTokenInfoUri())
                            .queryParam("id_token", idToken)
                            .encode(StandardCharsets.UTF_8)
                            .build()
                            .toUri())
                    .retrieve()
                    .body(GoogleTokenInfoResponse.class);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Google 토큰 정보 조회에 실패했습니다.");
        }
    }

    private void assertConfigured() {
        if (properties.getClientId() == null || properties.getClientId().isBlank()
                || properties.getClientSecret() == null || properties.getClientSecret().isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Google OAuth 설정이 필요합니다.");
        }
    }
}
