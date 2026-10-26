package com.gw.api.dto.life;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public final class LifeDailyDtos {
    private LifeDailyDtos() { }
    public record SaveItem(@NotBlank @Size(max = 200) String title) { }
    public record Completion(@NotNull Boolean completed) { }
    public record Item(String itemUuid, String title, LocalDate startDate, LocalDate endDate) { }
    public record Check(String itemUuid, LocalDate date) { }
    public record Month(String month, LocalDate today, String timeZone, List<Item> items, List<Check> checks) { }
}
