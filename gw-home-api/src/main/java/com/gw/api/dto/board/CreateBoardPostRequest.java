package com.gw.api.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardPostRequest(
        @NotBlank(message = "categoryUuid는 필수입니다.")
        String categoryUuid,
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 300, message = "title은 300자 이하여야 합니다.")
        String title,
        @NotBlank(message = "content는 필수입니다.")
        String content
) {
}
