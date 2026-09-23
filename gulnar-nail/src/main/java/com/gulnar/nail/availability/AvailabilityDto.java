package com.gulnar.nail.availability;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AvailabilityDto {

    public record SlotView(
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime time,
            boolean taken,
            boolean past
    ) {}

    public record DayView(
            LocalDate date,
            boolean closed,
            String note,
            List<SlotView> slots
    ) {}

    public record UpsertDayReq(
            @NotNull LocalDate date,
            Boolean closed,
            String note
    ) {}

    public record AddSlotReq(
            @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime time
    ) {}
}
