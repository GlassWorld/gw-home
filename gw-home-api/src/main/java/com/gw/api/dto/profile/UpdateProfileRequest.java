package com.gw.api.dto.profile;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateProfileRequest(
        @Size(max = 50, message = "nickname은 50자 이하여야 합니다.")
        String nickname,
        @Size(max = 500, message = "introduction은 500자 이하여야 합니다.")
        String introduction,
        @URL(message = "profileImageUrl 형식이 올바르지 않습니다.")
        String profileImageUrl
) {
}
