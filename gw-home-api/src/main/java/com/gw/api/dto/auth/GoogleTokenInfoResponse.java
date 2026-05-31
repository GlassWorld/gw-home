package com.gw.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenInfoResponse(
        String aud,
        String sub,
        String email,
        @JsonProperty("email_verified") Boolean emailVerified
) {
}
