package com.gw.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(
        @JsonProperty("id_token") String idToken
) {
}
