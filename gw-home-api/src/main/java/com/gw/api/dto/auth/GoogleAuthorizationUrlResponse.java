package com.gw.api.dto.auth;

public record GoogleAuthorizationUrlResponse(
        String authorizationUrl,
        String state
) {
}
