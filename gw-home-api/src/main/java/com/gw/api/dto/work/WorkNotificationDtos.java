package com.gw.api.dto.work;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public final class WorkNotificationDtos {
    private WorkNotificationDtos() {}
    public record SettingsRequest(
            @NotBlank @Email @Size(max=254) String email,
            @NotBlank @Size(max=64) String timeZone,
            @NotBlank @Pattern(regexp="(?:[01][0-9]|2[0-3]):[0-5][0-9]") String digestTime,
            @NotBlank @Pattern(regexp="DAILY|WEEKDAYS") String digestCycle,
            boolean digestEnabled, boolean reminderEnabled) {}
    public record SettingsResponse(String email, boolean verified, String timeZone, String digestTime,
            String digestCycle, boolean digestEnabled, boolean reminderEnabled, boolean serverReady,
            OffsetDateTime nextDigestAt) {}
    public record VerifyRequest(@NotBlank @Pattern(regexp="[0-9]{6}") String code) {}
    public record VerifyResponse(boolean verified) {}
    public record ReminderRequest(@NotNull OffsetDateTime sendAt) {}
    public record ReminderResponse(String reminderUuid, String todoUuid, boolean enabled,
            OffsetDateTime sendAt, String timeZone) {}
    public record DeliveryResponse(String deliveryUuid, String kind, String status, int attempts,
            OffsetDateTime dueAt, OffsetDateTime nextTryAt, OffsetDateTime sentAt, String resultCode) {}
}
