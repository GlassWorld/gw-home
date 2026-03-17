package com.gw.api.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank(message = "content는 필수입니다.")
        @Size(max = 2000, message = "content는 2000자 이하여야 합니다.")
        String content
) {
}
