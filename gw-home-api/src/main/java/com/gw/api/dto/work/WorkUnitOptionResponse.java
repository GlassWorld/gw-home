package com.gw.api.dto.work;

public record WorkUnitOptionResponse(
        String workUnitUuid,
        String title,
        String category,
        String status,
        String useYn
) {
}
